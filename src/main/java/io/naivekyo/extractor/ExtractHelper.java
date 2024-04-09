package io.naivekyo.extractor;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.DocumentParagraph;
import io.naivekyo.exception.ContentExtractFailureException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简易的抽取工具类, 包含一些静态方法
 * @since 1.0
 */
public class ExtractHelper {

    static {
        // 取消 apache poi 中关于 'zip bomb' 的安全限制
        ZipSecureFile.setMinInflateRatio(0.001d);
    }

    /**
     * 语句结束符：正则表达式
     */
    protected static final String ENDPOINT = "([。?？])";

    /**
     * 语句结束符正则表达式的模式对象
     */
    protected static final Pattern ENDPOINT_REGEX = Pattern.compile(ENDPOINT);

    /**
     * 语句的结束符字符数组, 暂时只考虑中文结束语
     */
    protected static final char[] SENTENCE_ENDPOINTS = new char[] { '。', '?', '？' };

    /**
     * 空字符串
     */
    protected static final String EMPTY_STRING = "";

    /**
     * 段落字数默认值
     */
    protected static final int DEFAULT_THRESHOLD = 400;

    /**
     * 段落字数最大值, max = DEFAULT_THRESHOLD * (1.0 + DEFAULT_FACTOR)
     */
    protected static final float DEFAULT_FACTOR = 3.0f;

    /**
     * 抽取 txt 文件的所有文本内容, 会先尝试获取文件的编码, 如果无法获取就是用默认的 {@link StandardCharsets#UTF_8} 进行处理 <br/>
     * @param is    文档输入流, 要求支持 {@link InputStream#markSupported() mark feature}, 如果不支持就包装为 {@link BufferedInputStream}
     * @return 所有文本内容拼接成的字符串
     * @throws ContentExtractFailureException 内容抽取异常
     */
    public static String txtFileTextExtract(InputStream is) throws IOException {
        return txtFileTextExtract(is, "");
    }

    /**
     * 抽取 txt 文件的所有文本内容, 会先尝试获取文件的编码, 如果无法获取就是用默认的 {@link StandardCharsets#UTF_8} 进行处理 <br/>
     * @param is    文档输入流, 要求支持 {@link InputStream#markSupported() mark feature}, 如果不支持就包装为 {@link BufferedInputStream}
     * @param join  txt 文件是按行抽取的, 该参数用于拼接所有的行
     * @return 所有文本内容拼接成的字符串
     * @throws ContentExtractFailureException 内容抽取异常
     */
    public static String txtFileTextExtract(InputStream is, String join) throws IOException {
        if (!is.markSupported()) {
            is = new BufferedInputStream(is);
        }
        EncodingDetector encodingDetector = new UniversalEncodingDetector();
        Charset charset = encodingDetector.detect(is, new Metadata());
        if (charset == null)
            charset = StandardCharsets.UTF_8;
        return txtFileTextExtract(is, charset, join);
    }

    /**
     * 抽取 txt 文件的所有文本内容, 使用对应的编码进行处理
     * @param is    文档输入流
     * @param join  txt 文件是按行抽取的, 该参数用于拼接所有的行
     * @param charset 文件编码
     * @return 所有文本内容拼接成的字符串
     * @throws ContentExtractFailureException 内容抽取异常
     */
    public static String txtFileTextExtract(InputStream is, Charset charset, String join) throws IOException {
        InputStreamReader iir = null;
        BufferedReader br = null;

        ContentExtractFailureException mark = null;
        String textContent = "";
        try {
            iir = new InputStreamReader(is, charset);
            br = new BufferedReader(iir);
            textContent = br.lines().collect(Collectors.joining(join));
        } catch (Exception e) {
            mark = new ContentExtractFailureException(e.getMessage(), e);
        } finally {
            if (br != null)
                br.close();
        }

        if (mark != null)
            throw mark;

        return textContent;
    }

    /**
     * 按行抽取 txt 所有文本封装为段落集合 <br/>
     * 注意: txt 文件中没有页的概念, 默认只有一页
     * @param is        文件输入流
     * @return 抽取到的所有段落集合
     * @throws IOException 抽取过程中可能会抛出异常
     * @throws ContentExtractFailureException 内容抽取异常
     */
    public static List<DocumentParagraph> txtFileTextExtract2Paragraphs(InputStream is) throws IOException {
        String textContent = txtFileTextExtract(is);
        List<DocumentParagraph> paragraphs = null;
        try {
            paragraphs = getDocumentParagraphs(textContent, " ", DEFAULT_THRESHOLD);
        } catch (Exception e) {
            throw new ContentExtractFailureException(e.getMessage(), e);
        }
        return paragraphs;
    }

    /**
     * 将文档全文字符串按照特定规则拆分合并为多个段落
     * @param spliceStr 文档中所有文本内容拼接为一个完整的字符串
     * @param splitRegex 字符串的拆分规则, 当无法根据 {@link #ENDPOINT} 拆分时就使用 splitRegex 去拆分文本
     * @param threshold 段落字数阈值, 合并多个文本片段直到总字数超过该数量
     * @return 拆分的所有段落
     */
    private static List<DocumentParagraph> getDocumentParagraphs(String spliceStr, String splitRegex, int threshold) {
        String[] split = spliceStr.split(ENDPOINT);
        if (split.length > 0) {
            Matcher mat = ENDPOINT_REGEX.matcher(spliceStr);
            int i = 0;
            while (mat.find()) {
                split[i] = split[i] + mat.group();
                i++;
            }
            return mergeTextSegment2Paragraphs(threshold, split);
        } else {
            return mergeTextSegment2Paragraphs(threshold, spliceStr.split(splitRegex));
        }
    }

    /**
     * 将一篇文档中的所有文本片段合并为多个文本段落 <br/>
     * 注意返回的段落集合默认每个段落对应页码为 1
     * @param threshold 段落字数阈值, 合并多个文本片段直到总字数超过该数量
     * @param pText 文档中的所有文本片段数组
     * @return 文本段落集合
     */
    private static List<DocumentParagraph> mergeTextSegment2Paragraphs(int threshold, String[] pText) {
        List<String> pList = Arrays.stream(pText).map(String::trim).filter(ContentHelper::hasText).collect(Collectors.toList());
        StringBuilder tmp = null;
        int p = 1;
        List<DocumentParagraph> paragraphs = new ArrayList<>(pList.size() >> 1 + pList.size());
        for (int i = 0; i < pList.size(); i++) {
            String segment = pList.get(i);
            if (segment.length() >= threshold) {
                paragraphs.add(new DocumentParagraph(1, p++, segment));
            } else {
                int k = i + 1;
                tmp = new StringBuilder(segment);
                boolean merge = false;
                for (; k < pList.size() - 1; k++) {
                    String str = pList.get(k);
                    tmp.append(str);
                    if (tmp.length() >= threshold) {
                        paragraphs.add(new DocumentParagraph(1, p++, tmp.toString()));
                        merge = true;
                        break;
                    }
                }
                if (merge) {
                    i = k;
                } else {
                    if (k == pList.size()) {
                        paragraphs.add(new DocumentParagraph(1, p, tmp.toString()));
                        break;
                    }
                }
            }
        }
        return getPrunedParagraphs(threshold, DEFAULT_FACTOR, paragraphs);
    }

    /**
     * 对抽取出的所有段落进行字数裁剪
     * @param threshold 字数阈值
     * @param factor 最大字数影响因子, max = threshold * (1 + factor)
     * @param paragraphs 原段落
     * @return 新的裁剪后的段落集合
     */
    public static List<DocumentParagraph> getPrunedParagraphs(int threshold, float factor, List<DocumentParagraph> paragraphs) {
        if (paragraphs != null && !paragraphs.isEmpty()) {
            List<DocumentParagraph> pList = new ArrayList<>(paragraphs.size() + paragraphs.size() >> 1);
            int max = (int) (threshold * (1.0f + factor));
            for (DocumentParagraph p : paragraphs) {
                String c = p.getContent();
                DocumentParagraph preP = null;
                if (!pList.isEmpty())
                    preP = pList.get(pList.size() - 1);
                if (preP != null) {
                    if (preP.getPagination().equals(p.getPagination())) {
                        if (preP.getParagraph() >= p.getParagraph()) {
                            p.setParagraph(preP.getParagraph() + 1);
                        }
                    }
                }
                if (c.length() > max) {
                    int batch = c.length() % max == 0 ? c.length() / max : c.length() / max + 1;
                    int page = p.getPagination();
                    int paragraph = p.getParagraph();
                    int begin = 0;
                    int end = begin + max;
                    String segment = c;
                    for (int i = 0; i < batch; i++) {
                        if (i == 0) {
                            String pre = segment.substring(begin, end);
                            segment = segment.substring(end);
                            p.setContent(pre);
                            paragraph++;
                            pList.add(p);
                        } else if (i == batch - 1) {
                            pList.add(new DocumentParagraph(page, paragraph++, segment));
                        } else {
                            String pre = segment.substring(begin, end);
                            segment = segment.substring(end);
                            pList.add(new DocumentParagraph(page, paragraph++, pre));
                        }
                    }
                } else {
                    pList.add(p);
                }
            }
            paragraphs = pList;
        }
        return paragraphs;
    }

    /**
     * 抽取 doc 后缀的 word 文件中的所有内容, 且去除可能含有的 crud, 速度比 {@link WordExtractor#getTextFromPieces()} 要慢 <br/>
     * <b>注意: 抽取的内容也包含 word 原文中的换行和空格</b>
     * @param is 文档流
     * @return  word 中正文部分所有文本内容
     * @throws IOException IO 异常
     */
    public static String wordDOCTextExtract(InputStream is) throws IOException {
        WordExtractor wordExtractor = new WordExtractor(is);
        return wordExtractor.getText();
    }

    /**
     * 抽取 doc 后缀的 word 文件中的所有文本段落内容 <br/>
     * <b>注意 word 中有些空行也是作为一个段落填充到返回数组中的某个位置上</b>
     * @param is 文档流
     * @return 按照顺序排列的所有段落文本
     * @throws IOException IO 异常
     */
    public static String[] wordDOCParagraphExtract(InputStream is) throws IOException {
        WordExtractor wordExtractor = new WordExtractor(is);
        return wordExtractor.getParagraphText();
    }

    /**
     * 抽取 doc 文档所有页面的所有文本片段, 多个片段如果总字数超过 300 则合为一个段落
     * @param is    文档输入流
     * @return 一个 doc 文档中的所有文本段落集合
     * @throws Exception 可能出现的异常
     */
    public static List<DocumentParagraph> wordDocTextExtract2Paragraphs(InputStream is) throws Exception {
        return wordDocTextExtract2Paragraphs(is, DEFAULT_THRESHOLD);
    }

    /**
     * 抽取 doc 文档所有页面的所有文本片段, 基于特定规则将多个文本片段合并为一个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 将整个文档当作一个页面处理
     * @param is    文档输入流
     * @param threshold 段落字数阈值, 多个文本片段合并后超过该阈值则为一个段落
     * @return 一个 doc 文档中的所有文本段落集合
     * @throws Exception 可能出现的异常
     */
    public static List<DocumentParagraph> wordDocTextExtract2Paragraphs(InputStream is, int threshold) throws Exception {
        WordExtractor wordExtractor = new WordExtractor(is);
        String text = wordExtractor.getText();
        text = text.replaceAll(ContentHelper.SYSTEM_NEW_LINE_SYMBOL, " ");
        return getDocumentParagraphs(text, "[.;]", threshold);
    }

    /**
     * 抽取 docx 后缀的 word 文件中的所有文本内容 <br/>
     * <b>注意: 抽取的内容也包含 word 原文中的换行和空格</b>
     * @param is 文档流
     * @return  word 中所有文本内容
     * @throws IOException IO 异常
     */
    public static String wordDOCXTextExtract(InputStream is) throws IOException {
        XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(new XWPFDocument(is));
        return xwpfWordExtractor.getText();
    }

    /**
     * 抽取 docx 文档所有页面的所有文本片段, 多个文本片段的总字数如果超过 300 则合为一个段落
     * @param is    文档输入流
     * @return  一个 docx 文档中的所有文本段落集合
     * @throws Exception    可能出现的异常
     */
    public static List<DocumentParagraph> wordDocxTextExtract2Paragraphs(InputStream is) throws Exception {
        return wordDocxTextExtract2Paragraphs(is, DEFAULT_THRESHOLD);
    }

    /**
     * 抽取 docx 文档所有页面的文本片段, 基于特定规则将多个文本片段合并为一个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 将整个文档当作一个页面处理
     * @param is    文档输入流
     * @param threshold 段落字数阈值, 多个文本片段合并后超过该阈值则为一个段落
     * @return  一个 docx 文档中的所有文本段落集合
     * @throws Exception 可能出现的异常
     */
    public static List<DocumentParagraph> wordDocxTextExtract2Paragraphs(InputStream is, int threshold) throws Exception {
        XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(new XWPFDocument(is));
        String fullText = xwpfWordExtractor.getText();
        fullText = fullText.replaceAll("\\n", "");
        return getDocumentParagraphs(fullText, "[.;]", threshold);
    }

    /**
     * 抽取 pdf 文件中的所有文本内容, 注意文本有序
     * @param is 文档流
     * @return 返回pdf 所有文本内容
     * @throws IOException IO 异常或者没有权限抽取 pdf 内容
     */
    public static List<String> pdfTextExtract(InputStream is) throws IOException {
        return pdfTextExtract(is, true);
    }

    /**
     * 抽取 pdf 文件中的所有文本内容
     * @param is 文档流
     * @param sortByPosition true 表示按照特定顺序排列每页中的文本, 但需损耗一定性能, 而 false 表示不排序
     * @return pdf 所有文本内容
     * @throws IOException IO 异常或者没有权限抽取 pdf 内容
     */
    public static List<String> pdfTextExtract(InputStream is, boolean sortByPosition) throws IOException {
        List<String> pageTexts = null;
        PDDocument document = null;
        Exception bak = null;
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            document = Loader.loadPDF(bytes, "", null, null, IOUtils.createTempFileOnlyStreamCache());
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent()) {
                throw new IOException("You do not have permission to extract text");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(sortByPosition);

            int pageNum = document.getNumberOfPages();
            if (pageNum < 1)
                return null;
            pageTexts = new ArrayList<>(pageNum + pageNum >> 1);
            for (int i = 0; i < pageNum; i++) {
                stripper.setStartPage(i + 1); // 1-base
                stripper.setEndPage(i + 1);

                String text = stripper.getText(document);
                if (ContentHelper.hasText(text)) {
                    text = text.trim();
                    String[] split = text.split(ContentHelper.SYSTEM_NEW_LINE_SYMBOL);
                    text = Arrays.stream(split).filter(ContentHelper::checkValidText).map(ContentHelper::cleanExtractedText).collect(Collectors.joining());
                    pageTexts.add(text);
                }
            }
        } catch (Exception e) {
            bak = e;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                bak = e;
            }
        }
        
        if (bak != null)
            throw new IOException(bak);
        
        if (pageTexts.isEmpty())
            return null;
        
        return pageTexts;
    }

    /**
     * 抽取 pdf 每个页面中的所有文本片段, 尝试基于特定的规则将文本片段合并为多个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 默认段落字数阈值 {@link #DEFAULT_THRESHOLD}, 段落最大字数影响因子 {@link #DEFAULT_FACTOR}
     * @param is 文档输入流
     * @return 一个 pdf 文档中所有的文本段落集合
     * @throws Exception 文档抽取过程中可能会出现异常
     */
    public static List<DocumentParagraph> pdfTextExtract2Paragraphs(InputStream is) throws Exception {
        return pdfTextExtract2Paragraphs(is, false, DEFAULT_THRESHOLD, DEFAULT_FACTOR);
    }

    /**
     * 抽取 pdf 每个页面中的所有文本片段, 尝试基于特定的规则将文本片段合并为多个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 该方法适用于中文文档, 抽取英文文档内容时会出现一定问题
     * @param is    文档输入流
     * @param sortByPosition    pdfbox 文本抽取规则, true 表示按照特定顺序排列每页中的文本, 但需损耗一定性能, 而 false 表示不排序
     * @param threshold 文本段落字数阈值, 调整该阈值会影响文本片段拼接为文本段落的处理逻辑
     * @param factor 段落字数影响因子, 段落最大字数 = threshold * (1 + factor), 浮点数 factor 取值范围 (0, 1.00)
     * @return  一个 pdf 文档中所有的文本段落集合
     * @throws Exception    文档抽取过程中可能会出现异常
     */
    public static List<DocumentParagraph> pdfTextExtract2Paragraphs(InputStream is, boolean sortByPosition, int threshold, float factor) throws Exception {
        List<DocumentParagraph> paragraphs = null;
        PDDocument document = null;
        Exception markEx = null;
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            document = Loader.loadPDF(bytes, "", null, null, IOUtils.createTempFileOnlyStreamCache());
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent()) {
                throw new IOException("You do not have permission to extract text");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(sortByPosition);

            int pageNum = document.getNumberOfPages();
            if (pageNum > 0) {
                paragraphs = new ArrayList<>(pageNum * 3 >> 1 + pageNum * 3);
                String lastSegment = EMPTY_STRING;  // 每页的最后一个文本片段
                // 0 表示无需处理; 
                // 1 表示当前页的最后一个文本片段拼接到下一页第一个文本片段前; 
                // 2 表示当前页的最后一个文本片段需要拼接到前一个片段后, 同时追加下一页的第一个文本片段; 
                // 3 表示上一个片段被截断了, 需要将剩下的部分填充到下一个片段的开头
                int joinFlag = 0;
                for (int i = 0; i < pageNum; i++) {
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String pageFullText = stripper.getText(document);
                    if (ContentHelper.hasText(pageFullText)) {
                        // 全文根据换行符拆分为多个文本片段
                        String[] split = pageFullText.trim().split(ContentHelper.SYSTEM_NEW_LINE_SYMBOL);
                        List<String> cleanTexts = Arrays.stream(split).filter(t -> {
                            boolean equals = EMPTY_STRING.equals(t.trim());
                            return !equals;
                        }).collect(Collectors.toList());

                        // 开始处理当前页面中的所有文本片段
                        if (!cleanTexts.isEmpty()) {
                            StringBuilder tmp = new StringBuilder();
                            int p = 1;
                            // 当前页面第一段文本需要追加上一页的最后一段文本
                            if (joinFlag == 1) {
                                tmp.append(lastSegment);
                                lastSegment = EMPTY_STRING;
                            }
                            for (int j = 0; j < cleanTexts.size(); j++) {
                                String segment = cleanTexts.get(j);
                                if (segment != null && !EMPTY_STRING.equals(segment)) {
                                    if (j == cleanTexts.size() - 1) {   // 每个页面的最后一段文本
                                        if (cleanTexts.size() == 1) {   // 当前页面只有一个文本片段时
                                            tmp.append(segment);
                                            paragraphs.add(new DocumentParagraph(i + 1, p++, tmp.toString()));
                                            tmp = new StringBuilder();
                                            joinFlag = 0;
                                            lastSegment = EMPTY_STRING;
                                        } else {
                                            if (i != pageNum - 1) {
                                                // 非最后一页的其他页面的最后一个文本片段
                                                // 如果当前页面全文都没有文本终止符, 则不做任何处理, 保存当前文本即可
                                                if (joinFlag == 3)
                                                    tmp.append(lastSegment);
                                                String tt = tmp.toString();
                                                StringBuilder full = null;
                                                if (!paragraphs.isEmpty()) {
                                                    full = new StringBuilder();
                                                    for (int i1 = paragraphs.size() - 1; i1 >= 0; i1--) {
                                                        DocumentParagraph p1 = paragraphs.get(i1);
                                                        Integer pn = p1.getPagination();
                                                        if (pn == i + 1) {
                                                            full.append(p1.getContent());
                                                        } else
                                                            break;
                                                    }
                                                    full.append(tt);
                                                }
                                                int tmpI = -1;
                                                if (full != null)
                                                    tmpI = lastEndpoint(full.toString());
                                                else
                                                    tmpI = lastEndpoint(tt);
                                                if (tmpI == -1) {
                                                    // 当前页面全文没有一个完整的句子, 则无需考虑最后一段的情况
                                                    paragraphs.add(new DocumentParagraph(i + 1, p++, tt + segment));
                                                    tmp = new StringBuilder();
                                                    joinFlag = 0;
                                                    lastSegment = EMPTY_STRING;
                                                    continue;
                                                }
                                                // 考虑 tmp 为空的情况
                                                char curLast = segment.charAt(segment.length() - 1);
                                                boolean curFlag = runEndpointMatch(curLast);
                                                if (tmp.length() == 0) {
                                                    // 当前页面最后一段, 但是 tmp 是空的, 可能是字数原因新开了一个段落
                                                    DocumentParagraph lastParagraphObj = paragraphs.get(paragraphs.size() - 1);
                                                    String lastParagraph = lastParagraphObj.getContent();
                                                    char preLast = lastParagraph.charAt(lastParagraph.length() - 1);
                                                    boolean preFlag = runEndpointMatch(preLast);
                                                    if (!preFlag && !curFlag) {
                                                        // 上一段和当前段的结束都不是语句结束符, 合并当前段和上一段以及下一页的第一段
                                                        lastParagraphObj.setContent(lastParagraphObj.getContent() + segment);
                                                        joinFlag = 2;
                                                    }
                                                    if (preFlag && !curFlag) {
                                                        // 上一段结束了, 但是当前段还未结束, 当前段拼接下一页的第一段
                                                        lastSegment = segment;
                                                        joinFlag = 1;
                                                    }
                                                    if (!preFlag && curFlag || preFlag && curFlag) {
                                                        // 场景 1：上一段未结束, 当前段结束, 则合并到上一段
                                                        // 场景 2：两段都结束了, 也合并到上一段中
                                                        lastParagraphObj.setContent(lastParagraphObj.getContent() + segment);
                                                        joinFlag = 0;
                                                    }
                                                } else {
                                                    char preLast = tmp.charAt(tmp.length() - 1);
                                                    boolean preFlag = runEndpointMatch(preLast);
                                                    if (!preFlag && !curFlag) {
                                                        // 上一段和当前段的结束都不是语句结束符, 合并当前段和上一段以及下一页的第一段
                                                        tmp.append(segment);
                                                        paragraphs.add(new DocumentParagraph(i + 1, p++, tmp.toString()));
                                                        joinFlag = 2;
                                                    }
                                                    if (preFlag && !curFlag) {
                                                        // 上一段结束了, 但是当前段还未结束, 当前段拼接下一页的第一段
                                                        paragraphs.add(new DocumentParagraph(i + 1, p++, tmp.toString()));
                                                        lastSegment = segment;
                                                        joinFlag = 1;
                                                    }
                                                    if (!preFlag && curFlag || preFlag && curFlag) {
                                                        // 场景 1：上一段未结束, 当前段结束, 则合并到上一段
                                                        // 场景 2：两段都结束了, 也合并到上一段中
                                                        tmp.append(segment);
                                                        paragraphs.add(new DocumentParagraph(i + 1, p++, tmp.toString()));
                                                        joinFlag = 0;
                                                    }
                                                    tmp = new StringBuilder();
                                                }
                                            } else {
                                                // 最后一页的最后一个段落
                                                tmp.append(segment);
                                                paragraphs.add(new DocumentParagraph(i + 1, p++, tmp.toString()));
                                            }
                                        }
                                    } else {
                                        // 正在处理当前页面第一个文本片段, 同时需要将第一个文本片段追加到上一个文本段落中
                                        if (j == 0 && joinFlag == 2) {  // joinFlag = 2 时 tmp 一定是空的
                                            DocumentParagraph lastParagraphObj = paragraphs.get(paragraphs.size() - 1);
                                            if (lastParagraphObj != null) {
                                                String preParagraph = lastParagraphObj.getContent();
                                                int idx = lastEndpoint(segment);
                                                if (idx == -1) {
                                                    // 继续向下寻找可以截断的文本
                                                    StringBuilder sb = new StringBuilder(preParagraph);
                                                    sb.append(segment);
                                                    int k = j + 1;
                                                    for (; k < cleanTexts.size() - 1; k++) {
                                                        String s = cleanTexts.get(k);
                                                        int x = lastEndpoint(s);
                                                        if (x != -1) {
                                                            String pre = s.substring(0, x + 1);
                                                            String last = s.substring(x + 1);
                                                            sb.append(pre);
                                                            joinFlag = 3;
                                                            lastSegment = last;
                                                            j = k;
                                                            preParagraph = sb.toString();
                                                            break;
                                                        } else {
                                                            sb.append(s);
                                                        }
                                                    }
                                                    if (k == cleanTexts.size() - 1) {
                                                        j = k - 1;
                                                        preParagraph = sb.toString();
                                                        joinFlag = 0;
                                                    }
                                                } else {
                                                    String pre = segment.substring(0, idx + 1);
                                                    String last = segment.substring(idx + 1);
                                                    preParagraph += pre;
                                                    joinFlag = 3;
                                                    lastSegment = last;
                                                }
                                                lastParagraphObj.setContent(preParagraph);
                                            }
                                        } else {
                                            // 处理常规文本片段, 持续拼接片段直到超过阈值
                                            if (joinFlag == 3) {
                                                tmp.append(lastSegment);
                                                // len += lastSegment.length();
                                                lastSegment = EMPTY_STRING;
                                                joinFlag = 0;
                                            }
                                            tmp.append(segment);
                                            if (tmp.length() >= threshold) {
                                                String paragraph = tmp.toString();
                                                tmp = new StringBuilder();
                                                int idx = lastEndpoint(paragraph);
                                                if (idx != -1) {
                                                    String pre = paragraph.substring(0, idx + 1);
                                                    String last = paragraph.substring(idx + 1);
                                                    paragraphs.add(new DocumentParagraph(i + 1, p++, pre));
                                                    lastSegment = last;
                                                    joinFlag = 3;
                                                } else {
                                                    // 继续寻找下一个句子休止的地方
                                                    int k = j + 1;
                                                    StringBuilder sb = new StringBuilder(paragraph);
                                                    for (; k < cleanTexts.size() - 1; k++) {
                                                        String s = cleanTexts.get(k);
                                                        int x = lastEndpoint(s);
                                                        if (x != -1) {
                                                            // 找到后就保存
                                                            String pre = s.substring(0, x + 1);
                                                            String last = s.substring(x + 1);
                                                            sb.append(pre);
                                                            joinFlag = 3;
                                                            lastSegment = last;
                                                            j = k;
                                                            paragraph = sb.toString();
                                                            break;
                                                        } else
                                                            sb.append(s);
                                                    }
                                                    if (k == cleanTexts.size() - 1) {
                                                        j = k - 1;
                                                        paragraph = sb.toString();
                                                        joinFlag = 0;
                                                        lastSegment = EMPTY_STRING;
                                                    }
                                                    paragraphs.add(new DocumentParagraph(i + 1, p++, paragraph));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // 没有实际的内容, 也需要还原备份
                            if (joinFlag == 1 && !EMPTY_STRING.equals(lastSegment)) {
                                DocumentParagraph lastParagraphObj = paragraphs.get(paragraphs.size() - 1);
                                if (lastParagraphObj != null) {
                                    lastParagraphObj.setContent(lastParagraphObj.getContent() + lastSegment);
                                    lastSegment = EMPTY_STRING;
                                }
                            }
                        }
                    } else {
                        // 当前页面没有抽取到文本, 则把前一页备份的最后一个文本片段还原
                        if (!paragraphs.isEmpty() && joinFlag == 1 && !EMPTY_STRING.equals(lastSegment)) {
                            DocumentParagraph lastParagraphObj = paragraphs.get(paragraphs.size() - 1);
                            if (lastParagraphObj != null) {
                                lastParagraphObj.setContent(lastParagraphObj.getContent() + lastSegment);
                                lastSegment = EMPTY_STRING;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            markEx = e;
        } finally {
            if (document != null)
                document.close();
        }

        if (markEx != null)
            throw markEx;
        
        return getPrunedParagraphs(threshold, factor, paragraphs);
    }

    /**
     * 找到指定文本中最后一个语句休止符的下标, 未找到则返回 -1
     * @param text 目标字符串
     * @return 下标 0-base, 未找到则返回 -1
     */
    private static int lastEndpoint(String text) {
        int idx = -1;
        for (int i = 0; i < text.length(); i++) {
            if (runEndpointMatch(text.charAt(i))) {
                idx = i;
            }
        }
        return idx;
    }

    private static boolean runEndpointMatch(char c) {
        for (char s : SENTENCE_ENDPOINTS) {
            if (s == c)
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        String inPath = "";
        String outPath = "";
        InputStream is = null;
        try {
            // 输入文件
            is = Files.newInputStream(Paths.get(inPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<DocumentParagraph> paragraphs = pdfTextExtract2Paragraphs(is);
        if (paragraphs.size() == 0)
            System.out.println("无文本内容");

        // 输出文件
        OutputStream os = null;
        BufferedWriter bw = null;
        try {
            os = new FileOutputStream(outPath, true);
            bw = new BufferedWriter(new OutputStreamWriter(os));
            for (DocumentParagraph dp : paragraphs) {
                String c = dp.getContent();
                bw.write(String.format("第 %d 页 -> 第 %d 段 -> 字数: %d", dp.getPagination(), dp.getParagraph(), c.length()));
                bw.newLine();
                bw.write(c);
                bw.newLine();
                bw.newLine();
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
