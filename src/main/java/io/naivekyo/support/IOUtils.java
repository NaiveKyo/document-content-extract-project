package io.naivekyo.support;

import org.apache.poi.hemf.usermodel.HemfPicture;
import org.apache.poi.hwmf.usermodel.HwmfPicture;
import org.apache.poi.util.Units;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * io fundamental facilities class
 * @author NaiveKyo
 * @since 1.0
 */
public class IOUtils {

    private IOUtils() {
        // no instances
    }

    /**
     * write contents to txt file line by line.
     * @param file target output file, must exist
     * @param lines all lines
     */
    public static void writeToTxtFile(File file, List<String> lines) {
        if (file == null)
            throw new NullPointerException("file cant be null.");
        IOException mark = null;
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
           fos = new FileOutputStream(file);
           bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * save file to disk.
     * @param data image bytes
     * @param path save path (specific-directory/your-img-name.type)
     */
    public static void saveFile(byte[] data, String path) {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Exception mark = null;
        try {
            fos = new FileOutputStream(path);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (bos != null)
                    bos.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * write bytes to input stream.
     * @param data bytes
     * @return input stream
     */
    public static InputStream wrapToInputStream(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    /**
     * 将图片持久化到指定的目录
     * @param image {@link BufferedImage}
     * @param type 图片类型, png、jpeg
     * @param file  目标文件
     * @return 成功或失败
     * @throws IOException 异常
     */
    public static boolean saveImage(BufferedImage image, String type, File file) throws IOException {
        return ImageIO.write(image, type, file);
    }

    /**
     * 从输入流中提取所有字节
     * @param is 输入流
     * @return  存储所有字节的数组
     * @throws IOException IO 异常
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        return toByteArray(is, 4096);
    }

    /**
     * 使用缓冲数组从输入流中提取所有字节
     * @param is 输入流
     * @param bufferSize 缓存数组大小
     * @return  存储所有字节的数组
     * @throws IOException IO 异常
     */
    public static byte[] toByteArray(InputStream is, int bufferSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[bufferSize];
        int len = -1;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * 将 wmf 文件转换为 png 文件
     * @param data Windows Meta File
     * @return file bytes
     * @throws IOException IOException
     */
    public static byte[] convertWMFToPNG(byte[] data) throws IOException {
        HwmfPicture wmf = new HwmfPicture(wrapToInputStream(data));
        Dimension2D dim = wmf.getSize();
        int width = Units.pointsToPixel(dim.getWidth());
        int height = Units.pointsToPixel(dim.getHeight());
        double max = Math.max(width, height);
        // 保持宽高比例不变
        if (max > 1500) {
            width *= 1500 / max;
            height *= 1500 / max;
        }

        BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        wmf.draw(g2, new Rectangle2D.Double(0, 0, width, height));

        g2.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        ImageIO.write(bufImg, "PNG", os);

        return os.toByteArray();
    }

    /**
     * 将 mef 文件转换为 png 文件
     * @param data Extended windows meta file
     * @return file bytes
     * @throws IOException IOException
     */
    public static byte[] convertEMFToPNG(byte[] data) throws IOException {
        HemfPicture emf = new HemfPicture(wrapToInputStream(data));
        Dimension2D dim = emf.getSize();

        int width = Units.pointsToPixel(dim.getWidth());
        int height = Units.pointsToPixel(dim.getHeight());
        double max = Math.max(width, height);
        // 保持宽高比例不变
        if (max > 1500) {
            width *= 1500 / max;
            height *= 1500 / max;
        }

        BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        emf.draw(g2, new Rectangle2D.Double(0, 0, width, height));

        g2.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        ImageIO.write(bufImg, "PNG", os);

        return os.toByteArray();
    }
    
    public static byte[] readDataFromNetworkSource(String url, String referer) {
        URL networkURL = null;
        URLConnection conn = null;
        InputStream is = null;
        Exception ex = null;
        ByteArrayOutputStream baos = null;
        try {
            networkURL = new URL(url);
            conn = networkURL.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("referer", referer);
            is = conn.getInputStream();
            baos = new ByteArrayOutputStream(4096);
            byte[] buf = new byte[4096];
            int len = -1;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
        } catch (IOException e) {
            ex = e;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                ex = e;
            }
        }

        if (ex != null)
            throw new RuntimeException(ex);
        return baos.toByteArray();
    }
    
}
