package io.naivekyo.extractor;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.DocumentParagraph;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * 语句的结束符, 暂时只考虑中文结束语
     */
    private static final List<Character> SENTENCE_ENDPOINTS = Arrays.asList('。', '?', '？');

    /**
     * 空字符串
     */
    private static final String EMPTY_STRING = "";
    
    /**
     * 抽取 txt 文件的所有文本内容
     * @param is    文档输入流
     * @return 所有文本内容拼接成的字符串
     * @throws Exception stream 将 IOException 包装为 {@link java.io.UncheckedIOException}
     */
    public static String txtFileTextExtract(InputStream is) throws Exception {
        return txtFileTextExtract(is, "");
    }

    /**
     * 抽取 txt 文件的所有文本内容
     * @param is    文档输入流
     * @param join  拼接文本使用的连接符
     * @return 所有文本内容拼接成的字符串
     * @throws Exception stream 将 IOException 包装为 {@link java.io.UncheckedIOException}
     */
    public static String txtFileTextExtract(InputStream is, String join) throws Exception {
        InputStreamReader iir = null;
        BufferedReader br = null;

        Exception mark = null;
        String textContent = "";
        try {
            iir = new InputStreamReader(is);
            br = new BufferedReader(iir);
            textContent = br.lines().collect(Collectors.joining(join));
        } catch (Exception e) { // handle UncheckedIOException
            mark = e;
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
     * @param is 文档输入流
     * @return 所有段落集合
     * @throws Exception 可能出现的异常 
     */
    public static List<DocumentParagraph> txtFileTextExtract2Paragraphs(InputStream is) throws Exception {
        InputStreamReader iir = null;
        BufferedReader br = null;

        Exception mark = null;
        List<DocumentParagraph> paragraphs = null;
        int p = 1;
        try {
            paragraphs = new ArrayList<>();
            iir = new InputStreamReader(is);
            br = new BufferedReader(iir);
            String line = null;
            while ((line = br.readLine()) != null) {
                paragraphs.add(new DocumentParagraph(1, p++, line));
            }
        } catch (Exception e) {
            mark = e;
        } finally {
            if (br != null)
                br.close();
        }

        if (mark != null)
            throw mark;

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
        return wordDocTextExtract2Paragraphs(is, 300);
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
        String[] pText = wordExtractor.getParagraphText();
        return getWordDocumentParagraphs(threshold, pText);
    }

    private static List<DocumentParagraph> getWordDocumentParagraphs(int threshold, String[] pText) {
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
                for (; k < pList.size() - 1; k++) {
                    String str = pList.get(k);
                    tmp.append(str);
                    if (tmp.length() >= threshold) {
                        paragraphs.add(new DocumentParagraph(1, p++, tmp.toString()));
                        break;
                    }
                }
                if (k == pList.size() - 1) {
                    tmp.append(pList.get(k));
                    paragraphs.add(new DocumentParagraph(1, p++, tmp.toString()));
                }
                i = k;
                tmp = null;
            }
        }
        return paragraphs;
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
        return wordDocxTextExtract2Paragraphs(is, 300);
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
        String[] pText = fullText.split("\\n");
        return getWordDocumentParagraphs(threshold, pText);
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
        try (PDDocument document = PDDocument.load(is, MemoryUsageSetting.setupTempFileOnly())) {
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
        }
        
        if (pageTexts.isEmpty())
            return null;
        return pageTexts;
    }

    /**
     * 抽取 pdf 每个页面中的所有文本片段, 尝试基于特定的规则将文本片段合并为多个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 默认段落字数阈值 300
     * @param is 文档输入流
     * @return 一个 pdf 文档中所有的文本段落集合
     * @throws Exception 文档抽取过程中可能会出现异常
     */
    public static List<DocumentParagraph> pdfTextExtract2Paragraphs(InputStream is) throws Exception {
        return pdfTextExtract2Paragraphs(is, false, 300);
    }

    /**
     * 抽取 pdf 每个页面中的所有文本片段, 尝试基于特定的规则将文本片段合并为多个文本段落, 返回所有的文本段落集合 <br/>
     * 注: 该方法适用于中文文档, 抽取英文文档内容时会出现一定问题
     * @param is    文档输入流
     * @param sortByPosition    pdfbox 文本抽取规则, true 表示按照特定顺序排列每页中的文本, 但需损耗一定性能, 而 false 表示不排序
     * @param threshold 文本段落字数阈值, 调整该阈值会影响文本片段拼接为文本段落的处理逻辑
     * @return  一个 pdf 文档中所有的文本段落集合
     * @throws Exception    文档抽取过程中可能会出现异常
     */
    public static List<DocumentParagraph> pdfTextExtract2Paragraphs(InputStream is, boolean sortByPosition, int threshold) throws Exception {
        List<DocumentParagraph> paragraphs = null;
        PDDocument document = null;
        Exception markEx = null;
        try {
            document = PDDocument.load(is, MemoryUsageSetting.setupTempFileOnly());
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
                                            // len = 0;
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
                                                        Integer pn = p1.getParagraph();
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
                                                boolean curFlag = SENTENCE_ENDPOINTS.contains(curLast);
                                                if (tmp.length() == 0) {
                                                    // 当前页面最后一段, 但是 tmp 是空的, 可能是字数原因新开了一个段落
                                                    DocumentParagraph lastParagraphObj = paragraphs.get(paragraphs.size() - 1);
                                                    String lastParagraph = lastParagraphObj.getContent();
                                                    char preLast = lastParagraph.charAt(lastParagraph.length() - 1);
                                                    boolean preFlag = SENTENCE_ENDPOINTS.contains(preLast);
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
                                                    boolean preFlag = SENTENCE_ENDPOINTS.contains(preLast);
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

        return paragraphs;
    }

    /**
     * 找到指定文本中最后一个语句休止符的下标, 未找到则返回 -1
     * @param text 目标字符串
     * @return 下标 0-base, 未找到则返回 -1
     */
    private static int lastEndpoint(String text) {
        int idx = -1;
        for (int i = 0; i < text.length(); i++) {
            if (SENTENCE_ENDPOINTS.contains(text.charAt(i))) {
                idx = i;
            }
        }
        return idx;
    }
    
}
