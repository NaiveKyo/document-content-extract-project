package io.naivekyo.support.pdf;

import io.naivekyo.content.DocContent;
import io.naivekyo.content.impl.ImageContent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 扩展 PDFGraphicsStreamEngine 定制图片处理方案
 * @see <a href="https://svn.apache.org/viewvc/pdfbox/branches/">https://svn.apache.org/viewvc/pdfbox/branches/</a>
 * @see <a href="https://svn.apache.org/viewvc/pdfbox/branches/2.0/tools/">https://svn.apache.org/viewvc/pdfbox/branches/2.0/tools/</a>
 * @see <a href="https://svn.apache.org/viewvc/pdfbox/branches/2.0/tools/src/main/java/org/apache/pdfbox/tools/ExtractImages.java?view=markup">https://svn.apache.org/viewvc/pdfbox/branches/2.0/tools/src/main/java/org/apache/pdfbox/tools/ExtractImages.java?view=markup</a>
 * @author NaiveKyo
 * @since 1.0
 */
public class CustomGraphicsStreamEngine extends PDFGraphicsStreamEngine {

    private static final Log LOG = LogFactory.getLog(CustomGraphicsStreamEngine.class);

    /**
     * 临时存储一些 duplicate pdf stream object, 用于图片去重
     */
    private final Set<COSStream> seen = new HashSet<>();

    private static final List<String> JPEG = Arrays.asList(
            COSName.DCT_DECODE.getName(),
            COSName.DCT_DECODE_ABBREVIATION.getName()
    );

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
    
    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
    }
    
    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        if (contents == null)
            contents = new ArrayList<>();
        
        // An external image object. (i.e. pdf 嵌入的外部图片)
        if (pdImage instanceof PDImageXObject) {
            if (pdImage.isStencil()) {
                processColor(getGraphicsState().getNonStrokingColor());
            }
            PDImageXObject xObject = (PDImageXObject) pdImage;
            if (seen.contains(xObject.getCOSObject())) {
                // skip duplicate image
                return;
            }
            seen.add(xObject.getCOSObject());
        }
        
        // save image
        write2ImageContent(pdImage);
    }

    /**
     * find out if it is a tiling pattern, then process that one
     * @param color A color value
     */
    private void processColor(PDColor color) throws IOException {
        if (color.getColorSpace() instanceof PDPattern) {
            PDPattern pattern = (PDPattern) color.getColorSpace();
            PDAbstractPattern abstractPattern = pattern.getPattern(color);
            if (abstractPattern instanceof PDTilingPattern) {
                processTilingPattern(((PDTilingPattern) abstractPattern), null, null);
            }
        }
    }

    private void write2ImageContent(PDImage pdImage) throws IOException {
        String suffix = pdImage.getSuffix();
        if (suffix == null || "jb2".equals(suffix)) {
            suffix = "png";
        } else if ("jpx".equals(suffix)) {
            // use jp2 suffix for file because jpx not known by windows
            suffix = "jp2";
        }
        
        if (hasMasks(pdImage)) {
            // TIKA-3040, PDFBOX-4771: can't save ARGB as JPEG
            suffix = "png";
        }

        // 如果可行的话, 就 write raw image, 但是这里没法获得图片透明度(alpha information)
        BufferedImage image = pdImage.getRawImage();
        if (image != null) {
            int elements = image.getRaster().getNumDataElements();
            suffix = "png";
            if (elements > 3) {
                // 图片的 channel 超过 3 个, 有点像 CMYK, 这里使用 tiff 文件格式
                // 但是需要 class path 中有 TIFF codec 才能正常工作
                suffix = "tiff";
            }
            doImageExtract(image, suffix);
            return;
        }
        
        if ("jpg".equals(suffix)) {
            String colorSpaceName = pdImage.getColorSpace().getName();
            if (PDDeviceGray.INSTANCE.getName().equals(colorSpaceName) || PDDeviceRGB.INSTANCE.getName().equals(colorSpaceName)) {
                // RGB or Gray colorspace: get and write the unmodified JPEG stream
                InputStream data = pdImage.createInputStream(JPEG);
                doImageExtract(data, suffix);
            } else {
                // for CMYK and other "unusual" colorspaces, the JPEG will be converted
                image = pdImage.getImage();
                if (image != null) {
                    doImageExtract(image, suffix);
                }
            }
        } else if ("jp2".equals(suffix)) {
            String colorSpaceName = pdImage.getColorSpace().getName();
            if (PDDeviceGray.INSTANCE.getName().equals(colorSpaceName) || PDDeviceRGB.INSTANCE.getName().equals(colorSpaceName)) {
                // RGB or Gray colorspace: get and write the unmodified JPEG2000 stream
                InputStream data = pdImage.createInputStream(Collections.singletonList(COSName.JPX_DECODE.getName()));
                doImageExtract(data, suffix);
            } else {
                // for CMYK and other "unusual" colorspaces, the image will be converted
                image = pdImage.getImage();
                if (image != null) {
                    doImageExtract(image, "jpeg2000");
                }
            }
        } else if ("tiff".equals(suffix) && pdImage.getColorSpace().equals(PDDeviceGray.INSTANCE)) {
            image = pdImage.getImage();
            if (image == null)
                return;
            // CCITT compressed images can have a different colorspace, but this one is B/W
            // This is a bitonal image, so copy to TYPE_BYTE_BINARY
            // so that a G4 compressed TIFF image is created by ImageIOUtil.writeImage()
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage bitonalImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
            // copy image the old fashioned way - ColorConvertOp is slower!
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bitonalImage.setRGB(x, y, image.getRGB(x, y));
                }
            }
            doImageExtract(bitonalImage, suffix);
        } else {
            image = pdImage.getImage();
            if (image != null) {
                doImageExtract(image, suffix);
            }
        }
    }
    
    private boolean hasMasks(PDImage pdImage) throws IOException {
        if (pdImage instanceof PDImageXObject) {
            PDImageXObject xImg = (PDImageXObject) pdImage;
            return xImg.getMask() != null || xImg.getSoftMask() != null;
        }
        return false;
    }

    /**
     * @param image {@link java.awt.image.RenderedImage}
     * @param suffix image type, e.g. png
     */
    private void doImageExtract(BufferedImage image, String suffix) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, suffix, baos);
        this.contents.add(new ImageContent(baos.toByteArray(), suffix));
    }

    private void doImageExtract(InputStream is, String suffix) throws IOException {
        Exception bakE = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            this.contents.add(new ImageContent(baos.toByteArray(), suffix));
        } catch (IOException e) {
            bakE = e;
        } finally {
            IOUtils.closeQuietly(is);
        }
        if (bakE != null)
            throw new IOException(bakE);
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
        processColor(getGraphicsState().getStrokingColor());
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        processColor(getGraphicsState().getNonStrokingColor());
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        processColor(getGraphicsState().getNonStrokingColor());
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException {
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        PDGraphicsState graphicsState = getGraphicsState();
        RenderingMode renderingMode = graphicsState.getTextState().getRenderingMode();
        if (renderingMode.isFill()) {
            processColor(graphicsState.getNonStrokingColor());
        }
        if (renderingMode.isStroke()) {
            processColor(graphicsState.getStrokingColor());
        }
    }
    
}
