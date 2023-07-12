package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

/**
 * .pptx 后缀的 ppt 文件内容抽取器实现
 */
public class XSLFPPTContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(XSLFPPTContentExtractor.class);
    
    public XSLFPPTContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        
    }
    
}
