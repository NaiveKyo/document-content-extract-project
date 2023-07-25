package io.naivekyo.content.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;
import io.naivekyo.support.function.ContentConverter;

import java.util.Objects;

/**
 * <p>纯文本内容</p>
 * <p><b>not-thread-safe</b></p>
 * @author NaiveKyo
 * @since 1.0
 */
public class TextContent implements DocContent {

    /**
     * 原始内容
     */
    private String rawContent;

    public TextContent(String content) {
        if (!ContentHelper.hasText(content))
            throw new RuntimeException("文本内容不能为空");
        this.rawContent = content;
    }

    @Override
    public String getContent() {
        return getRawContent();
    }

    @Override
    public String getHTMLWrapContent() {
        return ContentHelper.convertTextToHTML(ContentHelper.escapeTextContent(this));
    }

    @Override
    public String getHTMLWrapContent(ContentConverter<DocContent, String> converter) {
        if (converter == null)
            return getHTMLWrapContent();
        return ContentHelper.convertTextToHTML(ContentHelper.escapeTextContent(this, converter));
    }

    @Override
    public ContentType getType() {
        return ContentType.TEXT;
    }

    /**
     * 获取没有经过处理的原始字符串内容
     * @return raw content
     */
    public String getRawContent() {
        return rawContent;
    }

    /**
     * 设置文本内容
     * @param content text content
     */
    public void setRawContent(String content) {
        this.rawContent = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextContent that = (TextContent) o;
        return Objects.equals(rawContent, that.rawContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawContent);
    }

    @Override
    public String toString() {
        return "Raw text content: " + this.rawContent;
    }
    
}
