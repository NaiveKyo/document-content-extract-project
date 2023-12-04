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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 文档文本内容分割器实现 (全文分割为多个 sentence, 多个 sentence 合并为一个 chunk)
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
            log.warn("language: {}, separator set: {}: there is no suitable separator. Use the default strategy, split by word count", 
                    this.lang, Arrays.toString(LanguageEndpoints.getLanguageSet(LanguageEnum.valueOf(this.lang.toUpperCase(Locale.ROOT)))));
            sep = "";
        }
        
        String[] splits = text.split(sep);
        
        // 收集长度小于 chunkLen 的字符串集合
        List<String> tmpSegments = new ArrayList<>();
        // 遍历所有串, 将长度小于 chunkLen 的子串收集起来, 长度大于 chunkLen 的子串进行递归拆分处理
        for (String s : splits) {
            if (s.length() < chunkLen)
                tmpSegments.add(s);
            else {
                if (!tmpSegments.isEmpty()) {
                    // TODO merge
                    List<String> merge = merge(tmpSegments, sep);
                    chunks.addAll(merge);
                    tmpSegments.clear();
                }
                // 递归处理较长的子串
                List<String> recursive = split(s);
                chunks.addAll(recursive);
            }
        }
        
        if (!tmpSegments.isEmpty()) {
            List<String> merge = merge(tmpSegments, sep);
            chunks.addAll(merge);
        }
        
        return chunks;
    }
    
    private List<String> merge(List<String> segments, String separator) {
        int sepLen = separator.length();
        // 最终合并结果, 注意相邻的两个 sentence 之间可能具有重叠部分
        List<String> mergeResult = new ArrayList<>();
        // 
        List<String> currentSegment = new ArrayList<>();
        // 
        int total = 0;
        
        for (String segment : segments) {
            int len = segment.length();
            int tmp = total + len + (sepLen > 0 && !currentSegment.isEmpty() ? sepLen : 0);
            if (tmp > chunkLen) {
                if (total > chunkLen)
                    log.warn("created a chunk of size {}, which is longer than the specified: {}", total, chunkLen);
                if (!currentSegment.isEmpty()) {
                    // join: currentSegment 中所有字符通过分割符合并为一个完整的 sentence
                    String sentence = join(currentSegment, separator);
                    if (sentence != null)
                        mergeResult.add(sentence);
                    // 处理 overlap 部分, 不断移除 currentSegment 的第一个元素, 直到剩下的元素长度小于 overlap length
                    // 剩下的元素作为下个 chunk 的开头, 从而实现两个 chunk 之间具有重叠部分
                    while (total > chunkOverlap || (total > 0 && total + len + (sepLen > 0 && !currentSegment.isEmpty() ? sepLen : 0) > chunkLen)) {
                        total -= currentSegment.get(0).length() + (sepLen > 0 && currentSegment.size() > 1 ? sepLen : 0);
                        currentSegment.remove(0);
                    }
                }
            }
            currentSegment.add(segment);
            total += len + (sepLen > 0 && currentSegment.size() > 1 ? sepLen : 0);
        }

        String sentence = join(currentSegment, separator);
        if (sentence != null)
            mergeResult.add(sentence);
        
        return mergeResult;
    }
    
    private String join(List<String> segments, String separator) {
        if (segments.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            sb.append(segments.get(i));
            if (i < segments.size() - 1)
                sb.append(separator);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        DocumentTextSplitter splitter = new DocumentTextSplitter(LanguageEnum.ZH_CN);
        
        String file = "C:\\Users\\DELL\\Desktop\\云盘抽取结果\\pdf-纯文本-c61e76e0dc138bd809a943833ce92776.json";
        InputStream is = Files.newInputStream(Paths.get(file));
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String json = br.lines().collect(Collectors.joining());
        JSONObject entries = JSONUtil.parseObj(json);
        JSONArray pagination = entries.getJSONArray("content"); // 所有页
        JSONObject first = pagination.getJSONObject(0);
        String content = first.getStr("content", "");
        // 这里做处理
        String regex = "[\n\r]|\\s";
        content = content.trim().replaceAll(regex, "");
        
        List<String> split = splitter.split(content);
        
        // 构造正则表达式
        // String[] set = LanguageEndpoints.getLanguageSet(LanguageEnum.ZH_CN);
        // StringBuilder sb = new StringBuilder();
        // sb.append("[");
        // for (String value : set) {
        //     sb.append(Matcher.quoteReplacement(value));
        // }
        // sb.append("]");
        // String regex = sb.toString();
        // String regex = "[\n\r]";
        // split = split.stream().map(s -> s.trim().replaceAll(regex, "")).collect(Collectors.toList());

        for (String s : split) {
            System.out.println(s);
        }
    }
    
}
