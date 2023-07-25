package io.naivekyo.extractor.impl;

import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * .txt 文本文件内容抽取器实现
 * @author NaiveKyo
 * @since 1.0
 */
public class TxtFileContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(TxtFileContentExtractor.class);
    
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
