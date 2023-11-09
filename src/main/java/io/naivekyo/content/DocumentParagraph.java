package io.naivekyo.content;

import java.io.Serializable;
import java.util.Objects;

/**
 * 文档段落抽象
 * @author NaiveKyo
 * @since 1.0
 */
public class DocumentParagraph implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public DocumentParagraph() {
    }

    public DocumentParagraph(Integer pagination, Integer paragraph, String content) {
        this.pagination = pagination;
        this.paragraph = paragraph;
        this.content = content;
    }

    /**
     * 页码
     */
    private Integer pagination;

    /**
     * 段落的编号, 页码 + 段落号对应某个文档中的某段内容
     */
    private Integer paragraph;

    /**
     * 段落内容
     */
    private String content;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getPagination() {
        return pagination;
    }

    public void setPagination(Integer pagination) {
        this.pagination = pagination;
    }

    public Integer getParagraph() {
        return paragraph;
    }

    public void setParagraph(Integer paragraph) {
        this.paragraph = paragraph;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "DocumentParagraph{" +
                "pagination=" + pagination +
                ", paragraph=" + paragraph +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentParagraph that = (DocumentParagraph) o;
        return Objects.equals(pagination, that.pagination) && Objects.equals(paragraph, that.paragraph) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pagination, paragraph, content);
    }
    
}
