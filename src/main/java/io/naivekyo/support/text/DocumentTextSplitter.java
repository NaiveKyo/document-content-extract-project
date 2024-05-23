package io.naivekyo.support.text;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.naivekyo.constant.LanguageEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档文本内容分割器实现 (全文分割为多个 sentence, 多个 sentence 合并为一个 chunk) <br/>
 * thread-safe
 */
@Slf4j
public class DocumentTextSplitter {
    
    /**
     * sentence 分割符, 用于全文分割
     */
    private final String[] separators;

    /**
     * 语种 <br/>
     * see {@link LanguageEnum}
     */
    private final String lang;

    /**
     * sentence 长度
     */
    private final int chunkLen;

    /**
     * sentence 重叠部分的长度
     */
    private final int chunkOverlap;
    
    private static final int DEFAULT_CHUNK_LENGTH = 500;
    
    private static final int DEFAULT_OVERLAP_LENGTH = 50;

    /**
     * 使用默认分割字符串, sentence chunk 长度 = 500, chunk overlap 长度 = 50
     */
    public DocumentTextSplitter() {
        this(DEFAULT_CHUNK_LENGTH, DEFAULT_OVERLAP_LENGTH);
    }

    /**
     * 使用指定语言对应的分割字符集, sentence chunk 长度 = 500, chunk overlap 长度 = 50
     * @param language 指定语言
     */
    public DocumentTextSplitter(LanguageEnum language) {
        this(language, DEFAULT_CHUNK_LENGTH, DEFAULT_OVERLAP_LENGTH);
    }

    /**
     * 使用默认的分割字符集, 其他参数自定义
     * @param chunkLen sentence 长度
     * @param chunkOverlap sentence 重叠部分的长度
     */
    public DocumentTextSplitter(int chunkLen, int chunkOverlap) {
        this(LanguageEnum.UNKNOWN, chunkLen, chunkOverlap);
    }

    /**
     * 使用特定语种关联的 sentence 分割字符集
     * @param language 语言类型 {@link LanguageEnum}
     * @param chunkLen sentence 长度
     * @param chunkOverlap sentence 重叠部分的长度
     */
    public DocumentTextSplitter(LanguageEnum language, int chunkLen, int chunkOverlap) {
        this(language.getLang(), LanguageEndpoints.getLanguageSet(language), chunkLen, chunkOverlap);
    }

    /**
     * 自定义参数构造器
     * @param separators 字符分割集
     * @param chunkLen sentence 长度
     * @param chunkOverlap sentence 重叠部分的长度
     */
    public DocumentTextSplitter(String language, String[] separators, int chunkLen, int chunkOverlap) {
        if (language == null || "".equals(language))
            throw new IllegalArgumentException("language identify cant be null.");
        if (separators == null)
            throw new IllegalArgumentException("character split set cant be null.");
        if (chunkLen <= 0)
            throw new IllegalArgumentException("chunk length cant be negative or zero.");
        if (chunkOverlap < 0)
            throw new IllegalArgumentException("chunk overlap length cant be negative.");
        if (chunkOverlap >= chunkLen)
            throw new IllegalArgumentException("chunk overlap length cant equal or greater than chunk length.");
        this.lang = language;
        this.separators = separators;
        this.chunkLen = chunkLen;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 分割片段类
     */
    static class Segment {
        private final String delimiter;
        private final String segment;

        public Segment(String delimiter, String segment) {
            this.delimiter = delimiter;
            this.segment = segment;
        }
        
        public static Segment of(String delimiter, String segment) {
            return new Segment(delimiter, segment);
        }

        public String getDelimiter() {
            return delimiter;
        }

        public String getSegment() {
            return segment;
        }
    }

    /**
     * 清洗目标字符串中可能包含的多余杂质
     * @param text 源字符串
     * @return 新的字符串
     */
    public static String cleanText(String text) {
        // 去除全角空格
        return text.replace((char) 12288, ' ').replaceAll("[\n\r]|\\s", "").trim();
    }
    
    /**
     * 对目标文本做切分工作
     * @param text 全文内容
     * @return 切分后的 chunk 集合
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        String sep = null;
        for (String separator : separators) {
            // 选取适合的分割符对目标字符串进行分割
            if (text.contains(separator)) {
                sep = separator;
                break;
            }
        }
        if (sep == null) {
            // 没有找到适合的分割符, 就按照字符进行分割, 最后按字数合并
            log.warn("language: {}, there is no suitable separator. Use the default strategy, split by word count", this.lang);
            sep = "";
        }
        
        // 使用 separator 对目标字符串进行分割
        List<Segment> segments = new ArrayList<>();
        if (!"".equals(sep)) {
            Pattern pat = Pattern.compile("(" + Matcher.quoteReplacement(sep) + ")");
            String[] splits = pat.split(text);
            Matcher mat = pat.matcher(text);
            boolean canFind = false;
            int cursor = 0;
            while (mat.find()) {
                canFind = true;
                if (splits.length != 0) {
                    if (cursor < splits.length) {
                        String tmp = splits[cursor++].trim();
                        if (tmp.length() != 0)
                            segments.add(new Segment(mat.group(1), tmp));
                    }
                }
            }
            if (!canFind) {
                // 没法使用正则表达式进行分割, 则按字数截断
                segments = normalTruncation(text);
            } else {
                while (cursor < splits.length) {
                    // 处理可能遗漏的数据
                    segments.add(new Segment("", splits[cursor++]));
                }
            }
        } else {
            segments = normalTruncation(text);
        }

        // 收集长度小于 chunkLen 的字符串集合
        List<Segment> tmpSegments = new ArrayList<>();
        // 遍历所有串, 将长度小于 chunkLen 的子串收集起来, 长度大于 chunkLen 的子串进行递归拆分处理
        for (Segment segment : segments) {
            String s = segment.segment;
            if (s.length() < chunkLen)
                tmpSegments.add(segment);
            else {
                if (!tmpSegments.isEmpty()) {
                    // merge
                    List<String> merge = merge(tmpSegments);
                    chunks.addAll(merge);
                    tmpSegments.clear();
                }
                // 使用其他合适的分割符递归处理较长的子串
                List<String> recursive = split(s);
                // fix: 注意这里会丢失当前处理串的隔离符
                if (!recursive.isEmpty()) {
                    int size = recursive.size();
                    recursive.set(size - 1, recursive.get(size - 1) + segment.delimiter);
                }
                chunks.addAll(recursive);
            }
        }
        
        if (!tmpSegments.isEmpty()) {
            List<String> merge = merge(tmpSegments);
            chunks.addAll(merge);
        }
        
        return chunks;
    }

    private List<Segment> normalTruncation(String text) {
        int len = text.length();
        // 考虑到后续可能存在的重叠, 此处分割片段长度尽量短一些
        int interval = chunkLen / 2;
        int fragment = len % interval == 0 ? (len / interval) : (len / interval + 1);
        List<Segment> segmentList = new ArrayList<>(fragment + fragment >> 1);
        for (int i = 0; i < fragment; i++) {
            String tmp;
            if (i == fragment - 1)
                tmp = text.substring(interval * i);
            else
                tmp = text.substring(interval * i, interval * (i + 1));
            segmentList.add(Segment.of("", tmp));
        }
        return segmentList;
    }
    
    private List<String> merge(List<Segment> segments) {
        // 最终合并结果, 注意相邻的两个 sentence 之间可能具有重叠部分
        List<String> mergeResult = new ArrayList<>();
        // 将多个 segment 合并为一个 sentence
        LinkedList<Segment> segmentCollector = new LinkedList<>();
        // 当前 sentence 的字符长度
        int total = 0;
        for (Segment segment : segments) {
            // 当前 segment + separator 后的长度
            int len = segment.getSegment().length() + segment.getDelimiter().length();
            if (total + len > chunkLen) {
                if (!segmentCollector.isEmpty()) {
                    // join: segmentCollector 中所有字符合并为一个完整的 sentence
                    String sentence = join(segmentCollector);
                    if (sentence != null)
                        mergeResult.add(sentence);
                    // 处理 overlap 部分, 不断移除 segmentCollector 的第一个元素, 直到剩下的元素长度小于 overlap length
                    // 剩下的元素作为下个 chunk 的开头, 从而实现两个 chunk 之间具有重叠部分
                    if (chunkOverlap == 0) {
                        segmentCollector.clear();
                        total = 0;
                    } else {
                        while (total > chunkOverlap && segmentCollector.size() > 1) {
                            Segment head = segmentCollector.get(0);
                            total -= head.getSegment().length() + head.getDelimiter().length();
                            segmentCollector.pop();
                        }
                    }
                }
            }
            segmentCollector.add(segment);
            total += len;
        }

        String sentence = join(segmentCollector);
        if (sentence != null)
            mergeResult.add(sentence);
        
        return mergeResult;
    }
    
    private String join(List<Segment> segments) {
        if (segments.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.getSegment()).append(segment.getDelimiter());
        }
        return sb.toString();
    }

    /**
     * Unit Tests 
     */
    public static void main(String[] args) throws Exception {
        DocumentTextSplitter splitter = new DocumentTextSplitter(LanguageEnum.ZH_CN, 500, 0);
        
        String file = "";
        InputStream is = Files.newInputStream(Paths.get(file));
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String json = br.lines().collect(Collectors.joining());
        br.close();
        // txt
        // String content = cleanText(json);
        
        // json
        JSONObject entries = JSONUtil.parseObj(json);
        JSONArray pagination = entries.getJSONArray("content");
        JSONObject first = pagination.getJSONObject(0);
        String content = first.getStr("content", "");
        content = cleanText(content);
        List<String> split = splitter.split(content);

        for (String s : split) {
            System.out.println(s);
        }
        
    }
    
}
