package io.naivekyo.util;

import org.apache.poi.poifs.filesystem.FileMagic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 使用 apache poi {@link org.apache.poi.poifs.filesystem.FileMagic} 检测文件的类型 <br/>
 * 调用工具类中的方法时应先调用 {@link #wrapRepeatableReadInputStream(InputStream)} 方法
 */
public class FileTypeDetector {

    /**
     * 包装输入流, 为其添加 mark-reset feature, 如果 is 本身具有该特性则不做处理
     * @param is 输入流
     * @return is or {@link java.io.BufferedInputStream}
     */
    public static InputStream wrapRepeatableReadInputStream(InputStream is) {
        return FileMagic.prepareToCheckMagic(is);
    }

    /**
     * PDF file
     * @param is mark-reset support
     * @throws IOException
     */
    public static boolean isPDF(InputStream is) throws IOException {
        return compare(is, FileMagic.PDF);
    }

    /**
     * Microsoft's OLE 2 Compound Document Format
     * @param is mark-reset support
     * @throws IOException
     */
    public static boolean isOLE2(InputStream is) throws IOException {
        return compare(is, FileMagic.OLE2);
    }

    /**
     * Office Open XML (ooxml)
     * @param is mark-reset support
     * @throws IOException
     */
    public static boolean isOOXML(InputStream is) throws IOException {
        return compare(is, FileMagic.OOXML);
    }

    private static boolean compare(InputStream is, FileMagic target) throws IOException {
        return target.equals(FileMagic.valueOf(is));
    }

    /**
     * Unit Tests
     */
    public static void main(String[] args) throws Exception {
        String path = "";
        InputStream is = Files.newInputStream(Paths.get(path));
        System.out.println(isPDF(wrapRepeatableReadInputStream(is)));
    }
    
}
