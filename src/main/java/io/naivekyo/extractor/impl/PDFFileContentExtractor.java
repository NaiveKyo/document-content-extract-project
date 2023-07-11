package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.pdf.OperatorName;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.filter.DecodeResult;
import org.apache.pdfbox.filter.Filter;
import org.apache.pdfbox.filter.FilterFactory;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.PDPostScriptXObject;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDInlineImage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>.pdf 文件内容抽取器实现</p>
 * reference:
 * <ul>
 *     <li><a href='https://stackoverflow.com/questions/40531871/how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136'>https://stackoverflow.com/questions/40531871/how-can-i-check-if-pdf-page-is-imagescanned-by-pdfbox-xpdf/40621136#40621136</a></li>
 *     <li><a href='https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/rendering/CustomGraphicsStreamEngine.java?view=markup'>https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/rendering/CustomGraphicsStreamEngine.java?view=markup</a></li>
 * </ul>
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:41
 */
@Slf4j
public class PDFFileContentExtractor extends AbstractContentExtractor {

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
                // TODO
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                
                int numberOfPages = pdfDocument.getNumberOfPages();
                System.out.println("总页数: " + numberOfPages);
                
                for (int i = 1; i <= numberOfPages; i++) {
                    PDPage pdfPage = pdfDocument.getPage(i - 1);
                    
                    // stripper.setStartPage(i);
                    // stripper.setEndPage(i);
                    // String text = stripper.getText(pdfDocument);
                    // System.out.println("page: " + i);
                    // System.out.println(ContentHelper.cleanWordText(text));
                    
                    // extracted(pdfPage);
                    CustomGraphicsStreamEngine engine = new CustomGraphicsStreamEngine(pdfPage);
                    engine.run();
                }
                // extracted(pdfDocument.getPage(0));

                // CustomGraphicsStreamEngine engine = new CustomGraphicsStreamEngine(pdfDocument.getPage(0));
                // engine.run();
            } else {
                log.error("没有权限读取当前 pdf 文件的内容");
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
    
    public static class CustomGraphicsStreamEngine extends PDFGraphicsStreamEngine {

        /**
         * Constructor.
         *
         * @param page
         */
        protected CustomGraphicsStreamEngine(PDPage page) {
            super(page);
        }
        
        // ================================= 扩展方法 ==============================

        /**
         * Runs the engine on the current page.
         * 
         * @throws IOException If there is an IO error while drawing the page.
         */
        public void run() throws IOException {
            super.processPage(super.getPage());
            
            for (PDAnnotation annotation : super.getPage().getAnnotations()) {
                super.showAnnotation(annotation);
            }
        }

        // ================================= 默认实现 ==============================
        
        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
            System.out.printf("appendRectangle %.2f %.2f, %.2f %.2f, %.2f %.2f, %.2f %.2f%n",
                    	                p0.getX(), p0.getY(), p1.getX(), p1.getY(),
                    	                p2.getX(), p2.getY(), p3.getX(), p3.getY());
        }

        static AtomicInteger count = new AtomicInteger(0);
        
        static Map<BufferedImage, Integer> imageCount = new ConcurrentHashMap<>();
        
        @Override
        public void drawImage(PDImage pdImage) throws IOException {
            System.out.println("pdImage: " + pdImage.getClass().getName());
            System.out.println("drawImage: " + pdImage.getImage());
            boolean exist = imageCount.put(pdImage.getImage(), 1) == null;
            if (exist) {
                if (pdImage instanceof PDImageXObject) {
                    PDImageXObject imageXObject = (PDImageXObject) pdImage;
                    // 处理翻转的图片
                    Matrix ctm = super.getGraphicsState().getCurrentTransformationMatrix();
                    String flips = "";
                    if (ctm.getScaleX() < 0)
                        flips += "h";
                    if (ctm.getScaleY() < 0)
                        flips += "v";
                    if (flips.length() > 0)
                        flips = "-" + flips;
                    // pdf 附加的图片
                    String fileType = imageXObject.getSuffix();
                    ContentHelper.saveImage(imageXObject.getImage(), fileType, new File("C:\\" + count.incrementAndGet() + "." + fileType));
                } else if (pdImage instanceof PDInlineImage) {
                    // pdf 中使用特殊表达式表示的图片
                }
            }
           
        }

        @Override
        public void clip(int windingRule) throws IOException {
            System.out.println("clip");
        }

        @Override
        public void moveTo(float x, float y) throws IOException {
            System.out.printf("moveTo %.2f %.2f%n", x, y);
        }

        @Override
        public void lineTo(float x, float y) throws IOException {
            System.out.printf("lineTo %.2f %.2f%n", x, y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
            System.out.printf("curveTo %.2f %.2f, %.2f %.2f, %.2f %.2f%n", x1, y1, x2, y2, x3, y3);
        }

        @Override
        public Point2D getCurrentPoint() throws IOException {
            // if you want to build paths, you'll need to keep track of this like PageDrawer does
            return new Point2D.Float(0, 0);
        }

        @Override
        public void closePath() throws IOException {
            System.out.println("closePath");
        }

        @Override
        public void endPath() throws IOException {
            System.out.println("endPath");
        }

        @Override
        public void strokePath() throws IOException {
            System.out.println("strokePath");
        }

        @Override
        public void fillPath(int windingRule) throws IOException {
            System.out.println("fillPath");
        }

        @Override
        public void fillAndStrokePath(int windingRule) throws IOException {
            System.out.println("fillAndStrokePath");
        }

        @Override
        public void shadingFill(COSName shadingName) throws IOException {
            System.out.println("shadingFill " + shadingName.toString());
        }

        // ================================= 定制操作 ==============================


        @Override
        public void showTextString(byte[] string) throws IOException {
            System.out.print("showTextString \"");
            super.showTextString(string);
            System.out.println("\"");
        }

        @Override
        public void showTextStrings(COSArray array) throws IOException {
            System.out.print("showTextStrings \"");
            super.showTextStrings(array);
            System.out.println("\"");
        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {
            System.out.print("showGlyph " + code);
            super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);
        }
        
        

    }

    private void extracted(PDPage pdfPage) throws IOException {
        PDResources resources = pdfPage.getResources();
        PDFStreamParser parse = new PDFStreamParser(pdfPage);
        // Object token = parse.parseNextToken();
        // List<Object> tokenList = new LinkedList<>();
        //
        // HashMap<String, Integer> counterMap = new HashMap<>();
        parse.parse();
        List<Object> tokens = parse.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            // 根据操作数去处理 pdf 内容
            Object token = tokens.get(i);
            System.out.println(token);
            if (token instanceof Operator) {
                System.out.println(token);
                Operator op = (Operator) token;
                String name = op.getName();

                if (OperatorName.SHOW_TEXT.equals(name)) {
                    // 'Tj' 操作符对应 COSString
                    COSString previous = (COSString) tokens.get(i - 1);
                    // System.out.println("文本: " + previous.getString());
                } else if (OperatorName.SHOW_TEXT_ADJUSTED.equals(name)) {
                    // 'TJ' 操作符对应 COSArray
                    COSArray previous = (COSArray) tokens.get(i - 1);
                }

                if (OperatorName.BEGIN_INLINE_IMAGE.equals(name)) {

                }

            } else if (token instanceof COSName) {
                
            }
            
        }

        // while (token != null) {
        //     counterMap.compute(token.getClass().getName(), (k, v) -> v == null ? 1 : v + 1);
        //     System.out.println(token);
        //     if (token instanceof Operator) {    // Operator
        //         Operator op = (Operator) token;
        //         String name = op.getName();
        //         // if (OperatorName.BEGIN_INLINE_IMAGE_DATA.equals(name)) {
        //         //     byte[] imageData = op.getImageData();
        //         // }
        //        
        //         counterMap.compute(name, (k, v) -> v == null ? 1 : v + 1);
        //     } else {    // COSBase
        //         if ((token instanceof COSName)) {
        //             COSName name = (COSName) token;
        //             PDXObject xObject = resources.getXObject(name);
        //
        //             if (xObject instanceof PDImageXObject) {
        //                 // TODO 处理图片
        //                 PDImageXObject imageXObject = (PDImageXObject) xObject;
        //                 String fileType = imageXObject.getSuffix();
        //                 byte[] bytes = imageXObject.getStream().toByteArray();
        //                 ContentHelper.saveImage(imageXObject.getImage(), fileType, new File("C:\\Users\\DELL\\Desktop\\文档抽取测试数据\\pdf 图片\\1." + fileType));
        //                
        //                 counterMap.compute("image object", (k, v) -> v == null ? 1 : v + 1);
        //             } else if (xObject instanceof PDFormXObject) {
        //                 counterMap.compute("form object", (k, v) -> v == null ? 1 : v + 1);
        //             } else if (xObject instanceof PDPostScriptXObject) {
        //                 counterMap.compute("script object", (k, v) -> v == null ? 1 : v + 1);
        //             }
        //         }
        //     }
        //     tokenList.add(token);
        //     token = parse.parseNextToken();
        // }
        
        // counterMap.entrySet().forEach(System.out::println);
    }

    private void extracted1(PDPage pdfPage) throws IOException {
        PDResources resources = pdfPage.getResources();
        Iterable<COSName> xObjectNames = resources.getXObjectNames();
        for (COSName name : xObjectNames) {
            PDXObject xObject = resources.getXObject(name);
            if (resources.isImageXObject(name)) {   //  处理图片
                System.out.println("1");
                PDImageXObject imageXObject = (PDImageXObject) xObject;
                String fileType = imageXObject.getSuffix();
                byte[] bytes = imageXObject.getStream().toByteArray();
                this.getContents().add(new ImageContent(bytes, fileType));
            } else {
                System.out.println("2");
                if (xObject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject) xObject;
                    System.out.println("Form: " + xObject.getClass().getName());
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
                    System.out.println("Script");
                }
            }
        }
    }
}
