package io.naivekyo;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class FileTypeTests {

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String DOC_CONTENT_TYPE = "application/msword";

    private static final String TXT_CONTENT_TYPE = "text/plain";

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    
    // application/x-tika-ooxml

    @Test
    public void test() throws Exception {
        String file = "";
        // Tika tika = new Tika();
        // String detect = tika.detect(file);
        // System.out.println(detect);

        // TikaConfig tika = new TikaConfig();
        //
        // Metadata metadata = new Metadata();
        // MediaType detect = tika.getDetector().detect(TikaInputStream.get(Paths.get(file), metadata), metadata);
        // System.out.println(detect.getType());

        Tika tika = new Tika();
        String detect = tika.detect(TikaInputStream.get(new FileInputStream(file)));
        System.out.println(detect);
    }

}
