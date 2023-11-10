package io.naivekyo;

import io.naivekyo.content.DocumentParagraph;
import io.naivekyo.extractor.ExtractHelper;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 测试文档文本内容抽取及段落合并
 */
public class TextExtractTest {

    @Test
    public void pdfTextExtract2Paragraphs() throws Exception {
        InputStream is = readFile("");
        export2TxtFile(ExtractHelper.pdfTextExtract2Paragraphs(is));
    }

    public static InputStream readFile(String path) {
        try {
            return Files.newInputStream(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void export2TxtFile(List<DocumentParagraph> paragraphs) {
        String target = "";
        OutputStream os = null;
        BufferedWriter bw = null;
        try {
            os = new FileOutputStream(target, true);
            bw = new BufferedWriter(new OutputStreamWriter(os));
            for (DocumentParagraph p : paragraphs) {
                String c = p.getContent();
                bw.write(String.format("第 %d 页 -> 第 %d 段 -> 字数: %d", p.getPagination(), p.getParagraph(), c.length()));
                bw.newLine();
                bw.write(c);
                bw.newLine();
                bw.newLine();
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
