package io.naivekyo.content;

/**
 * 文档内容对应的类型
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:22
 */
public enum ContentType {
    
    IMAGE("image"), TEXT("text"), TABLE("table");

    private String name;

    ContentType(String name) {
        this.name = name;
    }
    
}
