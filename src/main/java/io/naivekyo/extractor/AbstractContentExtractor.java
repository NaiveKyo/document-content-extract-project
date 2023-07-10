package io.naivekyo.extractor;

import io.naivekyo.content.DocContent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象内容抽取器, 定义了一些基础属性和方法
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:34
 */
public abstract class AbstractContentExtractor implements ContentExtractor{

    /**
     * 文档的字节输入流
     */
    private final InputStream docByteStream;

    /**
     * 当前文档的内容
     */
    private final List<DocContent> contents;

    public AbstractContentExtractor(InputStream docByteStream) {
        if (docByteStream == null)
            throw new RuntimeException("文档输入流不能为 null");
        this.docByteStream = docByteStream;
        this.contents = new ArrayList<>();
    }

    public InputStream getDocByteStream() {
        return docByteStream;
    }

    public List<DocContent> getContents() {
        return contents;
    }

    @Override
    public List<DocContent> extract() {
        this.doExtract();
        return this.contents;
    }

    /**
     * 模板方法: 执行具体的内容抽取逻辑
     */
    protected abstract void doExtract();
    
}
