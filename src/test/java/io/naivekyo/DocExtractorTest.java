package io.naivekyo;

import io.naivekyo.content.DocContent;
import io.naivekyo.extractor.ContentExtractor;
import io.naivekyo.extractor.ExtractorHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * 测试类
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:30
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
        ContentExtractor extractor = ExtractorHelper.createTxtFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        contents.stream().map(DocContent::getHTMLWrapContent).forEach(System.out::println);
    }
    
    @Test
    public void testDOCFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createHWPFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        contents.stream().map(DocContent::getHTMLWrapContent).forEach(System.out::println);
    }
    
    @Test
    public void testDOCXFileExtract() {
        ContentExtractor extractor = ExtractorHelper.createXWPFFileExtractor(is);
        List<DocContent> contents = extractor.extract();
        contents.stream().map(DocContent::getHTMLWrapContent).forEach(System.out::println);
    }
    
}
