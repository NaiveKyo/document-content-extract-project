package io.naivekyo.support.pdf;

import io.naivekyo.content.DocContent;
import io.naivekyo.content.impl.ImageContent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDInlineImage;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扩展 PDFGraphicsStreamEngine 定制图片处理方案
 */
public class CustomGraphicsStreamEngine extends PDFGraphicsStreamEngine {

    private static final Log LOG = LogFactory.getLog(CustomGraphicsStreamEngine.class);

    private static final Object DUMMY_OBJ = new Object();
    
    private int pageNum;

    /**
     * 图片去重使用的 map
     */
    private Map<BufferedImage, Object> distinctImage = new HashMap<>();
    
    private List<DocContent> contents;
    
    /**
     * Constructor.
     *
     * @param page A page in a PDF document.
     */
    public CustomGraphicsStreamEngine(PDPage page) {
        super(page);
    }

    /**
     * Constructor.
     * 
     * @param page  A page in a PDF document.
     * @param pageNum page number
     */
    public CustomGraphicsStreamEngine(PDPage page, int pageNum) {
        super(page);
        this.pageNum = pageNum;
    }

    // ================================= 扩展方法 ==============================

    /**
     * Runs the engine on the current page.
     *
     * @throws IOException If there is an IO error while drawing the page.
     */
    public void run() throws IOException {
        super.processPage(super.getPage());
    }

    /**
     * 获取抽取的所有内容
     * @return 抽取的内容
     */
    public List<DocContent> getContents() {
        return this.contents;
    }
    
    // ================================= 默认实现 ==============================

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
    }
    
    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        if (contents == null)
            contents = new ArrayList<>();
        BufferedImage image = pdImage.getImage();
        boolean distinct = distinctImage.put(image, DUMMY_OBJ) == null;
        if (pdImage instanceof PDImageXObject) {
            // 只能通过 PDImageXObject 形式获取 pdf 嵌入的外部图片资源
            PDImageXObject imageXObject = (PDImageXObject) pdImage;
            if (distinct) {
                String fileType = imageXObject.getSuffix();
                if (fileType == null) {
                    LOG.error("解析 .pdf 文件时, 从 PDImageXObject 读取到未知的图片类型");
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, fileType, baos);
                    this.contents.add(new ImageContent(baos.toByteArray(), fileType));
                }
            }
        } else if (pdImage instanceof PDInlineImage) {
            // pdf 中使用特殊表达式生成的图片
            PDInlineImage inlineImage = (PDInlineImage) pdImage;
            if (distinct) {
                String fileType = inlineImage.getSuffix();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, fileType, baos);
                this.contents.add(new ImageContent(baos.toByteArray(), fileType));
            }
        } else {
            LOG.error(String.format("未知的 pdf 图片类型, %s", pdImage.getClass().getName()));
        }
    }

    @Override
    public void clip(int windingRule) throws IOException {
    }

    @Override
    public void moveTo(float x, float y) throws IOException {
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
    }

    @Override
    public Point2D getCurrentPoint() throws IOException {
        return new Point2D.Float(0, 0);
    }

    @Override
    public void closePath() throws IOException {
    }

    @Override
    public void endPath() throws IOException {
    }

    @Override
    public void strokePath() throws IOException {
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException {
    }

    // ================================= TODO 定制操作 ==============================
    
    @Override
    public void showTextString(byte[] string) throws IOException {
        super.showTextString(string);
    }

    @Override
    public void showTextStrings(COSArray array) throws IOException {
        super.showTextStrings(array);
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        super.showGlyph(textRenderingMatrix, font, code, displacement);
    }
    
}
