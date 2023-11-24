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
        System.out.print("input args: ");
        for (String arg : args) {
            System.out.print(arg + " ");
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
                    System.out.println("TODO: to be completed");
                    break;
                default:
                    System.out.println("invalid option, please type one of [-h|-s|-b].");
            }
        } else {
            System.out.println("invalid option, please type one of [-h|-s|-b].");
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
            String type = null;
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
        System.out.printf("%5s\tGain some help information for extractor tool.%n", "-h");
        System.out.printf("%5s\t[input file] [output dir]\t\tExtract the text content of the input file and write it to a TXT file in the output directory.%n", "-s");
        System.out.printf("%5s\t                         \t\te.g. extractor.exe C:\\file.pdf D:\\dir%n", "");
        System.out.printf("%5s\t[source directory] [target directory]\t\tExtracts the text content of all files in the source directory and writes it to the target directory.%n", "-b");
        System.out.printf("%5s\t                                     \t\te.g. extractor.exe C:\\dir1 D:\\dir2%n", "");

    }

    private static boolean checkOptions(String op) {
        for (String option : OPTIONS) {
            if (option.equals(op))
                return true;
        }
        return false;
    }
    
    
    
}
