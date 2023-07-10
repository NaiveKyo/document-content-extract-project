package io.naivekyo.content.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;

import java.util.Objects;

/**
 * 纯文本内容 thread-safe
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:25
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
        return ContentHelper.convertTextToHTML(getRawContent());
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
