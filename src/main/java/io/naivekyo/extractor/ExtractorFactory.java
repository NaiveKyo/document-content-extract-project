package io.naivekyo.extractor;

import io.naivekyo.extractor.impl.HSLFPPTContentExtractor;
import io.naivekyo.extractor.impl.HWPFWordContentExtractor;
import io.naivekyo.extractor.impl.PDFFileContentExtractor;
import io.naivekyo.extractor.impl.TxtFileContentExtractor;
import io.naivekyo.extractor.impl.XSLFPPTContentExtractor;
import io.naivekyo.extractor.impl.XWPFWordContentExtractor;
import org.apache.poi.poifs.filesystem.FileMagic;

import java.io.IOException;
import java.io.InputStream;

/**
 * 内容抽取器 factory class, 包含快速创建抽取器的静态工厂方法, 以及其他一些便捷的方法
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:35
 */
public final class ExtractorFactory {

    private ExtractorFactory() {
    }

    /**
     * factory method: create .txt file extractor. 
     * @param is 文档输入流
     * @return .txt 文件内容抽取器实例
     */
    public static ContentExtractor createTxtFileExtractor(InputStream is) {
        return new TxtFileContentExtractor(is);
    }

    /**
     * factory method: create .doc file extractor. 
     * @param is 文档输入流
     * @return .doc 文件内容抽取器实例
     */
    public static ContentExtractor createHWPFWordExtractor(InputStream is) {
        return new HWPFWordContentExtractor(is);
    }

    /**
     * factory method: create .docx file extractor. 
     * @param is 文档输入流
     * @return .docx 文件内容抽取器实例
     */
    public static ContentExtractor createXWPFWordExtractor(InputStream is) {
        return new XWPFWordContentExtractor(is);
    }

    /**
     * 根据文件字节流自动检测 word 文件类型(.doc/.docx)
     * @param is	文件输入流
     * @return		抽取器实例
     * @throws IOException IOException
     * @throws RuntimeException 未知的类型
     */
    public static ContentExtractor smartCreateWordExtractor(InputStream is) throws IOException {
        InputStream prepareIs = FileMagic.prepareToCheckMagic(is);
        FileMagic fm = FileMagic.valueOf(prepareIs);
        if (FileMagic.OLE2.equals(fm))
            return new HWPFWordContentExtractor(prepareIs);
        else if (FileMagic.OOXML.equals(fm))
            return new XWPFWordContentExtractor(prepareIs);
        else
            throw new RuntimeException("未知的 word 文件类型");
    }

    /**
     * factory method: create .pdf file extractor. 
     * @param is 文档输入流
     * @return .pdf 文件内容抽取器实例
     */
    public static ContentExtractor createPDFFileExtractor(InputStream is) {
        return new PDFFileContentExtractor(is);
    }

    /**
     * factory method: create .ppt file extractor
     * @param is 文档输入流
     * @return .ppt 文件内容抽取器实例
     */
    public static ContentExtractor createHSLFPPTExtractor(InputStream is) {
        return new HSLFPPTContentExtractor(is);
    }

    /**
     * factory method: create .pptx file extractor
     * @param is 文档输入流
     * @return .pptx 文件内容抽取器实例
     */
    public static ContentExtractor createXSLFPPTExtractor(InputStream is) {
        return new XSLFPPTContentExtractor(is);
    }


    /**
     * 根据文件字节流自动检测 ppt 文件类型(.ppt/.pptx)
     * @param is	文件输入流
     * @return		抽取器实例
     * @throws IOException IOException
     * @throws RuntimeException 未知的类型
     */
    public static ContentExtractor smartCreatePPTExtractor(InputStream is) throws IOException {
        InputStream prepareIs = FileMagic.prepareToCheckMagic(is);
        FileMagic fm = FileMagic.valueOf(prepareIs);
        if (FileMagic.OLE2.equals(fm))
            return new HSLFPPTContentExtractor(prepareIs);
        else if (FileMagic.OOXML.equals(fm))
            return new XSLFPPTContentExtractor(prepareIs);
        else
            throw new RuntimeException("未知的 ppt 文件类型");
    }
    
}
