package io.naivekyo.support;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * io helper class
 */
public class IOUtils {

    /**
     * write contents to txt file line by line.
     * @param file target output file
     * @param lines all lines
     */
    public static void writeToTxtFile(File file, List<String> lines) {
        if (file == null)
            throw new NullPointerException("file cant be null.");
        IOException mark = null;
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
           fos = new FileOutputStream(file);
           bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * save file to disk.
     * @param data image bytes
     * @param path save path (directory/name.type)
     */
    public static void saveFile(byte[] data, String path) {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Exception mark = null;
        try {
            fos = new FileOutputStream(path);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (bos != null)
                    bos.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }
    
}
