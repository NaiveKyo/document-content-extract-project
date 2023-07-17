package io.naivekyo;

import io.naivekyo.content.DocContent;
import io.naivekyo.extractor.ContentExtractor;
import io.naivekyo.extractor.ExtractorHelper;
import io.naivekyo.support.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试类
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:30
 */
public class DocExtractorTest {
    
    public static InputStream is = null;
    
    @BeforeAll
    public static void beforeAll() throws FileNotFoundException {
        is = new FileInputStream(FILE_PATH);
    }
    
    @Test
    public void testTxtFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createTxtFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\txt-content.txt"), collect);
    }
    
    @Test
    public void testDOCFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createHWPFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\word-hwpf-content.txt"), collect);
    }
    
    @Test
    public void testDOCXFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createXWPFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\word-xwpf-content.txt"), collect);
    }
    
    @Test
    public void testPDFFileExtract() {
        // -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider
        // System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        // System.out.println(System.getProperty("sun.java2d.cmm"));
        ContentExtractor extractor = ExtractorHelper.createPDFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\pdf-content.txt"), collect);
    }
    
    @Test
    public void testHSLFFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createHSLFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\ppt-content.txt"), collect);
    }
    
    @Test
    public void testXLSFFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createXSLFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File("C:\\ppt-content.txt"), collect);
    }

    public static final String FILE_PATH = "C:\\";

}
