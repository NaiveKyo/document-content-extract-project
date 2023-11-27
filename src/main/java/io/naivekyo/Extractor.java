package io.naivekyo;

import io.naivekyo.content.DocumentParagraph;
import io.naivekyo.extractor.ExtractHelper;
import io.naivekyo.util.IOUtils;
import io.naivekyo.util.TikaFacadeUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * main class
 */
public class Extractor {
    
    private static final String[] OPTIONS = { "-h", "-s", "-b" };
    
    /**
     * args0: options <br/> 
     * -s 标准模式用于单个文件抽取 <br/>
     * -b 批量模式用于抽取特定目录下的所有文件内容 <br/>
     * 
     * args1: 输入文件路径
     * args2: 输出文件路径
     * @param args 参数
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("please type -h for some help information. e.g. extractor.exe -h or extractor -h");
            return;
        }
        System.out.println();
        String op = args[0];
        if (checkOptions(op)) {
            switch (op) {
                case "-h":
                    showHelpInformation();
                    break;
                case "-s":
                    if (args.length == 3) {
                        extractFileContent(args[1], args[2]);
                    } else {
                        if (args.length <= 2)
                            System.out.println("-s option must work with input file and output directory. note file name can't contains white space.");
                        if (args.length > 3)
                            System.out.println("-s option only need two arguments but get three. note file name can't contains white space.");
                    }
                    break;
                case "-b":
                    if (args.length == 3)
                        extractAllFilesInDirectory(args[1], args[2]);
                    else {
                        if (args.length <= 2)
                            System.out.println("-b option must work with two arguments: input and output directory. note directory name can't contains white space.");
                        if (args.length > 3)
                            System.out.println("-b option only need two arguments but get three. note directory name can't contains white space.");
                    }
                    break;
                default:
                    System.out.println("invalid option, please type one of [-h|-s|-b].");
            }
        } else {
            System.out.println("invalid option, please type one of [-h|-s|-b].");
        }
    }

    private static void extractAllFilesInDirectory(String inputDir, String outputDir) {
        List<Path> regularFiles = null;
        Path in = null;
        Path out = null;
        try {
            in = Paths.get(inputDir);
            out = Paths.get(outputDir);
            if (!Files.isDirectory(in)) {
                System.out.println("argument: " + inputDir + " is not a directory");
                return;
            }
            if (!Files.isDirectory(out)) {
                System.out.println("argument: " + outputDir + " is not a directory");
                return;
            }
            // recursive walk
            regularFiles = new ArrayList<>();
            walkDirectory(regularFiles, in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (regularFiles != null) {
            if (regularFiles.isEmpty()) {
                System.out.println("can't find any document file in this folder: " + inputDir);
                return;
            } else {
                System.out.println("detect regular file number: " + regularFiles.size());
            }
            int s = 0;
            int f = 0;
            for (int i = 0; i < regularFiles.size(); i++) {
                Path path = regularFiles.get(i);
                boolean process = true;
                String tip = null;
                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(Files.newInputStream(path));
                    String media = TikaFacadeUtil.detectFileMediaType(bis);
                    List<DocumentParagraph> paragraphs = null;
                    if (TikaFacadeUtil.isPdf(media)) {
                        paragraphs = ExtractHelper.pdfTextExtract2Paragraphs(bis);
                    } else if (TikaFacadeUtil.isOLE2Word(media)) {
                        paragraphs = ExtractHelper.wordDocTextExtract2Paragraphs(bis);
                    } else if (TikaFacadeUtil.isOOXMLWord(media)) {
                        paragraphs = ExtractHelper.wordDocxTextExtract2Paragraphs(bis);
                    } else {
                        process = false;
                        tip = "no support file, name: " + path.getFileName().toString() + ", media type: " + media;
                    }
                    if (paragraphs == null || paragraphs.isEmpty()) {
                        process = false;
                        tip = "cant extract any text content from " + path.getFileName().toString();
                    } else {
                        List<String> textContents = paragraphs.stream().map(DocumentParagraph::getContent).collect(Collectors.toList());
                        String originName = path.getFileName().toString();
                        String prefix = null;
                        if (originName.contains(".")) {
                            Pattern pat = Pattern.compile("^(.*)?\\..*$");
                            Matcher mat = pat.matcher(originName);
                            if (mat.matches()) {
                                prefix = mat.group(1);
                            } else {
                                process = false;
                                tip = "can't extract file name. origin name: " + originName;
                            }
                        } else
                            prefix = originName;
                        if (process) {
                            String output = out.normalize() + System.getProperty("file.separator") + prefix + ".txt";
                            IOUtils.writeToTxtFile(new File(output),
                                    textContents);
                        }
                    }
                } catch (Exception e) {
                    process = false;
                    e.printStackTrace();
                    tip = e.getMessage();
                } finally {
                    try {
                        if (bis != null)
                            bis.close();
                    } catch (IOException e) {
                        process = false;
                        tip = e.getMessage();
                    }
                }
                System.out.printf("progress: [%d/%d], file name: %s, %s%n", (i + 1), regularFiles.size(), 
                        path.getFileName().toString(), process ? "success" : "failure, tip: " + tip);
                if (process) s++;
                else f++;
            }
            System.out.printf("work is completed. total: %d, success: %d, failure: %d%n", regularFiles.size(), s, f);
        }
    }

    private static void walkDirectory(List<Path> bucket, Path dir) throws IOException {
        for (Path path : Files.newDirectoryStream(dir)) {
            if (Files.isRegularFile(path))
                bucket.add(path);
            else {
                if (Files.isDirectory(path))
                    walkDirectory(bucket, path);
            }
        }
    }

    private static void extractFileContent(String file, String dir) {
        BufferedInputStream bis = null;
        try {
            Path filePath = Paths.get(file);
            if (!Files.isRegularFile(filePath)) {
                System.out.println(file + " is not a regular file.");
                return;
            }
            Path dirPath = Paths.get(dir);
            if (!Files.isDirectory(dirPath)) {
                System.out.println(dir + " is not a directory");
                return;
            }
            bis = new BufferedInputStream(Files.newInputStream(filePath));
            String media = TikaFacadeUtil.detectFileMediaType(bis);
            List<DocumentParagraph> paragraphs = null;
            if (TikaFacadeUtil.isPdf(media)) {
                paragraphs = ExtractHelper.pdfTextExtract2Paragraphs(bis);
            } else if (TikaFacadeUtil.isOLE2Word(media)) {
                paragraphs = ExtractHelper.wordDocTextExtract2Paragraphs(bis);
            } else if (TikaFacadeUtil.isOOXMLWord(media)) {
                paragraphs = ExtractHelper.wordDocxTextExtract2Paragraphs(bis);
            } else {
                System.out.println("can't handle this file. file media type: " + media);
            }
            if (paragraphs == null || paragraphs.isEmpty()) {
                System.out.println("cant extract any text content from " + file);
                return;
            }
            List<String> textContents = paragraphs.stream().map(DocumentParagraph::getContent).collect(Collectors.toList());
            String originName = filePath.getFileName().toString();
            String prefix = null;
            if (originName.contains(".")) {
                Pattern pat = Pattern.compile("^(.*)?\\..*$");
                Matcher mat = pat.matcher(originName);
                if (mat.matches()) {
                    prefix = mat.group(1);
                } else {
                    System.out.println("can't extract file name.");
                    return;
                }
            } else 
                prefix = originName;
            String output = dirPath.normalize() + System.getProperty("file.separator") + prefix + ".txt";
            IOUtils.writeToTxtFile(new File(output), 
                    textContents);
            System.out.println("extract work is completed. output file: " + output);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null)
                    bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void showHelpInformation() {
        System.out.printf("features: extract pdf, word, txt file text content then write to target txt file.%n");
        System.out.printf("%5s\tGain some help information for extractor tool.%n", "-h");
        System.out.printf("%5s\t%-60s\tExtract the text content of the input file and write it to a TXT file in the output directory.%n", "-s", "[input file] [output dir]");
        System.out.printf("%5s\t%-60s\te.g. extractor.exe -s C:\\file.pdf D:\\dir%n", "", "");
        System.out.printf("%5s\t%-60s\tExtracts the text content of all files in the source directory and writes it to the target directory.%n", "-b", "[source directory] [target directory]");
        System.out.printf("%5s\t%-60s\te.g. extractor.exe -b C:\\dir1 D:\\dir2%n", "", "");
    }

    private static boolean checkOptions(String op) {
        for (String option : OPTIONS) {
            if (option.equals(op))
                return true;
        }
        return false;
    }
    
    
    
}
