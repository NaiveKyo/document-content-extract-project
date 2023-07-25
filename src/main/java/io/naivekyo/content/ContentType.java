package io.naivekyo.content;

/**
 * 文档内容对应的类型
 * @author NaiveKyo
 * @since 1.0
 */
public enum ContentType {

    /**
     * 图片内容
     */
    IMAGE("image"),

    /**
     * 文本内容
     */
    TEXT("text"),

    /**
     * 表格内容
     */
    TABLE("table"),

    /**
     * 带标题的有序/无序列表
     */
    LIST("list");

    private final String name;

    ContentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
}
