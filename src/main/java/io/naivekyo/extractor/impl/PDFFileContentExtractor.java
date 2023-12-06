package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.DocContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.pdf.CustomGraphicsStreamEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>.pdf 文件内容抽取器实现</p>
 * @author NaiveKyo
 * @since 1.0
 * @see <a href="https://stackoverflow.com/questions/40531871/how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136">stackoverflow-questions-40531871how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136</a>
 * @see <a href="https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/rendering/CustomGraphicsStreamEngine.java?view=markup">apache pdfbox examples: CustomGraphicsStreamEngine.java</a>
 */
public class PDFFileContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(PDFFileContentExtractor.class);
    
    private PDFTextStripper textStripper;

    // see https://pdfbox.apache.org/2.0/getting-started.html
    // Important notice when using PDFBox with Java 8 before 1.8.0_191 or Java 9 before 9.0.4
    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }
    
    public PDFFileContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        PDDocument pdfDocument = null;
        Exception markEx = null;
        try {
            byte[] bytes = IOUtils.toByteArray(getDocByteStream());
            pdfDocument = Loader.loadPDF(bytes, "", null, null, IOUtils.createTempFileOnlyStreamCache());
            AccessPermission ap = pdfDocument.getCurrentAccessPermission();
            boolean canExtract = ap.canExtractForAccessibility();
            if (canExtract) {
                int numberOfPages = pdfDocument.getNumberOfPages();
                for (int i = 1; i <= numberOfPages; i++) {
                    getContents().add(new TextContent(String.format("第 %d 页", i)));
                    PDPage pdfPage = pdfDocument.getPage(i - 1);
                    // 处理文本
                    String pageText = this.extractByTextStripper(pdfDocument, i);
                    if (pageText != null) {
                        pageText = pageText.trim();
                        String[] split = pageText.split(ContentHelper.SYSTEM_NEW_LINE_SYMBOL);
                        List<TextContent> texts = Arrays.stream(split).filter(ContentHelper::checkValidText).map(TextContent::new).collect(Collectors.toList());
                        for (int j = 0; j < texts.size() - 1; j++) {
                            getContents().add(texts.get(j));
                        }
                    }
                    
                    // 处理图片
                    CustomGraphicsStreamEngine engine = new CustomGraphicsStreamEngine(pdfPage);
                    engine.run();
                    List<DocContent> images = engine.getContents();
                    if (images != null && !images.isEmpty())
                        getContents().addAll(images);
                }
            } else {
                LOG.error("没有权限读取当前 pdf 文件的内容");
                throw new RuntimeException("没有权限读取该 pdf 文件");
            }
        } catch (IOException e) {
            markEx = e;
        } finally {
            try {
                if (pdfDocument != null)
                    pdfDocument.close();
            } catch (IOException e) {
                markEx = e;
            }
        }
        if (markEx != null)
            throw new RuntimeException(markEx);
    }

    /**
     * 抽取指定页面内的所有文本
     * @param doc pdf 文档对象
     * @param pageNum 指定页码
     * @return  页面中包含的所有文本信息
     * @throws IOException IO 异常
     */
    private String extractByTextStripper(PDDocument doc, int pageNum) throws IOException {
        if (textStripper == null) {
            textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
        }
        textStripper.setStartPage(pageNum);
        textStripper.setEndPage(pageNum);
        return textStripper.getText(doc);
    }
    
}
