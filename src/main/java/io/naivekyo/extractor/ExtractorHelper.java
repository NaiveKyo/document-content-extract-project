package io.naivekyo.extractor;

import io.naivekyo.extractor.impl.HWPFWordContentExtractor;
import io.naivekyo.extractor.impl.PDFFileContentExtractor;
import io.naivekyo.extractor.impl.TxtFileContentExtractor;
import io.naivekyo.extractor.impl.XWPFWordContentExtractor;

import java.io.InputStream;

/**
 * 内容抽取器 helper class, 包含快速创建抽取器的静态工厂方法, 以及其他一些便捷的方法
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:35
 */
public final class ExtractorHelper {

    private ExtractorHelper() {
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
    public static ContentExtractor createHWPFFileExtractor(InputStream is) {
        return new HWPFWordContentExtractor(is);
    }

    /**
     * factory method: create .docx file extractor. 
     * @param is 文档输入流
     * @return .docx 文件内容抽取器实例
     */
    public static ContentExtractor createXWPFFileExtractor(InputStream is) {
        return new XWPFWordContentExtractor(is);
    }

    /**
     * factory method: create .pdf file extractor. 
     * @param is 文档输入流
     * @return .pdf 文件内容抽取器实例
     */
    public static ContentExtractor createPDFFileExtractor(InputStream is) {
        return new PDFFileContentExtractor(is);
    }
    
}
