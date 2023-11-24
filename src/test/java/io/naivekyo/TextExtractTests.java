package io.naivekyo;

import io.naivekyo.content.DocumentParagraph;
import io.naivekyo.extractor.ExtractHelper;
import io.naivekyo.util.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 测试文档文本内容抽取及段落合并
 */
public class TextExtractTests {

    @Test
    public void pdfTextExtract2Paragraphs() throws Exception {
        InputStream is = readFile("");
        export2TxtFile(ExtractHelper.pdfTextExtract2Paragraphs(is), "");
    }
    
    @Test
    public void wordDOCExtract2Paragraphs() throws Exception {
        InputStream is = readFile("");
        export2TxtFile(ExtractHelper.pdfTextExtract2Paragraphs(is), "");
    }

    public static InputStream readFile(String path) {
        try {
            return Files.newInputStream(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void export2TxtFile(List<DocumentParagraph> paragraphs, String path) {
        OutputStream os = null;
        BufferedWriter bw = null;
        try {
            os = new FileOutputStream(path, true);
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
    
    
    @Test
    public void pdf2txt() throws Exception {
        String dir = "";
        String target = "";
        Pattern pat = Pattern.compile("^(.*).pdf$");
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(dir));
        for (Path path : dirStream) {
            if (Files.isRegularFile(path)) {
                String name = path.toFile().getName();
                Matcher mat = pat.matcher(name);
                String prefix = "";
                if (mat.matches()) {
                    prefix = mat.group(1);
                }
                System.out.println(prefix + ".txt");
                List<DocumentParagraph> paragraphs = ExtractHelper.pdfTextExtract2Paragraphs(Files.newInputStream(path), false, 500, 4.0f);
                // List<String> lines = paragraphs.stream().map(DocumentParagraph::getContent).collect(Collectors.toList());
                // IOUtils.writeToTxtFile(new File(target + prefix + ".txt"), lines);
                export2TxtFile(paragraphs, target + prefix + ".txt");
            }
        }
    }
    
    @Test
    public void mergeTxtSegmentFile() throws Exception {
        String dir = "C:\\Users\\DELL\\Desktop\\赵志源\\diskdcp_dcp41_04";
        Pattern txtFilePattern = Pattern.compile(".*\\.txt$");
        String replace = "XIANXiNSZH|ZAOJIAO|XIANXINGZHE";
        String to = "C:\\Users\\DELL\\Desktop\\赵志源\\txt3.txt";
        
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(dir));
        List<String> fullText = new ArrayList<>();
        
        for (Path subPath : dirStream) {
            String fileName = subPath.getFileName().toString();
            if (Files.isRegularFile(subPath) && txtFilePattern.matcher(fileName).matches()) {
                fullText.addAll(IOUtils.readTxtFile(Files.newInputStream(subPath)));
            }
        }
        fullText = fullText.stream().map(t -> {
            return t.replaceAll(replace, "");
        }).filter(t -> !"".equals(t)).collect(Collectors.toList());
        
        IOUtils.writeToTxtFile(new File(to), fullText);
    }
}
