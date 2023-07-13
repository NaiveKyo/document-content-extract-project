package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.DocContent;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.pdf.CustomGraphicsStreamEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.graphics.PDPostScriptXObject;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>.pdf 文件内容抽取器实现</p>
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:41
 * @see <a href="https://stackoverflow.com/questions/40531871/how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136">stackoverflow-questions-40531871how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136</a>
 * @see <a href="https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/rendering/CustomGraphicsStreamEngine.java?view=markup">apache pdfbox examples: CustomGraphicsStreamEngine.java</a>
 */
public class PDFFileContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(PDFFileContentExtractor.class);
    
    private PDFTextStripper textStripper;
    
    public PDFFileContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        PDDocument pdfDocument = null;
        Exception markEx = null;
        try {
            pdfDocument = PDDocument.load(getDocByteStream());
            AccessPermission ap = pdfDocument.getCurrentAccessPermission();
            boolean canExtract = ap.canExtractForAccessibility();
            if (canExtract) {
                int numberOfPages = pdfDocument.getNumberOfPages();
                for (int i = 1; i <= numberOfPages; i++) {
                    PDPage pdfPage = pdfDocument.getPage(i - 1);
                    // 处理文本
                    String pageText = this.extractByTextStripper(pdfDocument, i);
                    if (pageText != null) {
                        pageText = pageText.trim();
                        String[] split = pageText.split("\r\n");
                        List<TextContent> texts = Arrays.stream(split).filter(ContentHelper::hasText).map(TextContent::new).collect(Collectors.toList());
                        for (int j = 0; j < texts.size() - 1; j++) {
                            getContents().add(texts.get(j));
                        }
                    }
                    
                    // 处理图片
                    CustomGraphicsStreamEngine engine = new CustomGraphicsStreamEngine(pdfPage, i);
                    engine.run();
                    List<DocContent> images = engine.getContents();
                    if (images != null && !images.isEmpty())
                        getContents().addAll(images);
                    getContents().add(new TextContent(String.format("第 %d 页", i)));
                }
            } else {
                LOG.error("没有权限读取当前 pdf 文件的内容");
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

    /**
     * 使用 StreamParser 按照字节流读取每个 page, 解析流中的操作数或其他对象, 情况较为复杂, 不推荐
     * @param pdfPage
     * @throws IOException
     * @deprecated 
     */
    private void extractByStreamParser(PDPage pdfPage) throws IOException {
        PDResources resources = pdfPage.getResources();
        PDFStreamParser parse = new PDFStreamParser(pdfPage);
        parse.parse();
        List<Object> tokens = parse.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            // 根据操作数去处理 pdf 内容
            Object token = tokens.get(i);
            if (token instanceof Operator) {
                System.out.println(token);
                Operator op = (Operator) token;
                String name = op.getName();

                if (OperatorName.SHOW_TEXT.equals(name)) {
                    // 'Tj' 操作符对应 COSString
                    COSString previous = (COSString) tokens.get(i - 1);
                } else if (OperatorName.SHOW_TEXT_ADJUSTED.equals(name)) {
                    // 'TJ' 操作符对应 COSArray
                    COSArray previous = (COSArray) tokens.get(i - 1);
                }

                if (OperatorName.BEGIN_INLINE_IMAGE.equals(name)) {
                    // 'BI' 对应 inline image
                }
            } else if (token instanceof COSName) {
                COSName name = (COSName) token;
                if (resources.isImageXObject(name)) {
                    PDImageXObject imageXObject = (PDImageXObject) resources.getXObject(name);
                    // 获取嵌入的外部图片
                }
            }
        }
    }

    /**
     * pdf 所有 page 共享 resource 和 dictionary, 此方法通过 resource 可以获取所有图片, 但是无法处理文本
     * @param pdfPage
     * @throws IOException
     * @deprecated 
     */
    private void extractBySharedResource(PDPage pdfPage) throws IOException {
        PDResources resources = pdfPage.getResources();
        Iterable<COSName> xObjectNames = resources.getXObjectNames();
        for (COSName name : xObjectNames) {
            PDXObject xObject = resources.getXObject(name);
            if (resources.isImageXObject(name)) {   //  处理图片
                PDImageXObject imageXObject = (PDImageXObject) xObject;
                String fileType = imageXObject.getSuffix();
                byte[] bytes = imageXObject.getStream().toByteArray();
                this.getContents().add(new ImageContent(bytes, fileType));
            } else {
                if (xObject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject) xObject;
                    PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
                    textStripper.setSortByPosition(true);
                    PDRectangle bBox = form.getBBox();
                    int x = (int) bBox.getLowerLeftX();
                    int y = (int) bBox.getLowerLeftY();
                    int width = (int) bBox.getWidth();
                    int height = (int) bBox.getHeight();
                    
                    textStripper.addRegion("class1", new Rectangle(x, y, width, height));
                    textStripper.extractRegions(pdfPage);
                    String text = textStripper.getTextForRegion("class1");
                    System.out.printf("[%d, %d, %d, %d]%n", x, y, width, height);
                } else if (xObject instanceof PDPostScriptXObject) {
                }
            }
        }
    }
    
}
