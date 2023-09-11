package io.naivekyo;

import io.naivekyo.content.DocContent;
import io.naivekyo.extractor.ContentExtractor;
import io.naivekyo.extractor.ExtractorFactory;
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
 * @since 1.0
 */
public class DocExtractorTest {

    public static final String FILE_PATH = "";
    
    public static InputStream is = null;
    
    @BeforeAll
    public static void beforeAll() throws FileNotFoundException {
        is = new FileInputStream(FILE_PATH);
    }
    
    @Test
    public void testTxtFileExtract() {
        ContentExtractor extractor = ExtractorFactory.createTxtFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
    
    @Test
    public void testDOCFileExtract() {
        ContentExtractor extractor = ExtractorFactory.createHWPFWordExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
    
    @Test
    public void testDOCXFileExtract() {
        ContentExtractor extractor = ExtractorFactory.createXWPFWordExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
    
    @Test
    public void testPDFFileExtract() {
        // -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider
        // System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        // System.out.println(System.getProperty("sun.java2d.cmm"));
        ContentExtractor extractor = ExtractorFactory.createPDFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
    
    @Test
    public void testHSLFFileExtract() {
        ContentExtractor extractor = ExtractorFactory.createHSLFPPTExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
    
    @Test
    public void testXLSFFileExtract() {
        ContentExtractor extractor = ExtractorFactory.createXSLFPPTExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }

    @Test
    public void testHSSFExcelExtract() {
        ContentExtractor extractor = ExtractorFactory.createHSSFExcelExtractor(is);
        List<DocContent> contents = extractor.extract();
        List<String> collect = contents.stream().map(DocContent::getHTMLWrapContent).collect(Collectors.toList());
        IOUtils.writeToTxtFile(new File(""), collect);
    }
}
