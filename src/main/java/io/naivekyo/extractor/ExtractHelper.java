package io.naivekyo.extractor;

import io.naivekyo.content.ContentHelper;
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
     * 按页、业内按段的形式抽取 pdf 文本内容
     * @param is    文档输入流
     * @return  按段落拆分的所有文本, 未抽取到文本内容则返回 null
     * @throws IOException  IO 相关异常
     */
    public static List<String> pdfTextExtractPageSegment(InputStream is) throws IOException {
        List<String> pageTexts = null;
        try (PDDocument document = PDDocument.load(is, MemoryUsageSetting.setupTempFileOnly())) {
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent()) {
                throw new IOException("You do not have permission to extract text");
            }
            PDFTextStripper stripper = new PDFTextStripper();

            int pageNum = document.getNumberOfPages();
            if (pageNum < 1){
                return null;
            }
            pageTexts = new ArrayList<>();

            String lastParagraph = "";  // 当前页的最后一段文本
            for (int i = 0; i < pageNum; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageFullText = stripper.getText(document);
                if (ContentHelper.hasText(pageFullText)) {
                    String[] split = pageFullText.trim().split(ContentHelper.SYSTEM_NEW_LINE_SYMBOL + "\\s");
                    // 获取当前页的文本: 按段落拆分
                    List<String> cleanTexts = Arrays.stream(split).filter(t -> {
                        boolean equals = "".equals(t.trim());
                        return !equals;
                    }).map(t -> t.trim().replaceAll(ContentHelper.SYSTEM_NEW_LINE_SYMBOL, "")).collect(Collectors.toList());

                    StringBuilder tmp = new StringBuilder();
                    boolean last = false;
                    if (!cleanTexts.isEmpty()) {
                        // 多段合并
                        for (int j = 0; j < cleanTexts.size(); j++) {
                            String paragraph = cleanTexts.get(j);
                            last = j == cleanTexts.size() - 1;  // 最后一段
                            if (j == 0) {
                                // 当前页面第一段文本需要追加上一页的最后一段文本
                                tmp.append(lastParagraph);
                            }
                            if (paragraph != null && !"".equals(paragraph)) {
                                if (last && i != pageNum - 1) {
                                    // 考虑到最后一段跨页的情况, 这里直接将当前页最后一段文本放到下一页去
                                    // 注意最后一页的情况
                                    lastParagraph = paragraph;
                                } else {
                                    tmp.append(paragraph);
                                }
                                pageTexts.add(tmp.toString());
                                tmp = new StringBuilder();
                            }
                        }
                    }
                }
            }
        }

        if (pageTexts.isEmpty()){
            return null;
        }
        return pageTexts;
    }
    
}
