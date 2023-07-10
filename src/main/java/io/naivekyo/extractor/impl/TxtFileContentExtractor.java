package io.naivekyo.extractor.impl;

import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * .txt 文本文件内容抽取器实现
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:36
 */
@Slf4j
public class TxtFileContentExtractor extends AbstractContentExtractor {

    public TxtFileContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        InputStream is = this.getDocByteStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Exception ex = null;
        try {
            String str = null;
            while ((str = br.readLine()) != null) {
                if (!"".equals(str))
                    this.getContents().add(new TextContent(str));
            }
        } catch (IOException e) {
            ex = e;
            log.error("读取文本行时出现异常", e);
        } finally {
            try {
                br.close();
                is.close();
            } catch (IOException e) {
                ex = e;
            }
        }
        if (ex != null)
            throw new RuntimeException(ex);
    }
    
}
