package io.naivekyo.content.impl;

import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;

import java.util.List;

/**
 * 对应 ppt 文件的 TextBox
 */
public class ListContent implements DocContent {
    
    private final List<String> items;

    public ListContent(List<String> items) {
        this.items = items;
    }

    @Override
    public String getContent() {
        StringBuilder sb = new StringBuilder();
        for (String item : getItems()) {
            sb.append(item).append("\r\n");
        }
        return sb.toString();
    }

    @Override
    public String getHTMLWrapContent() {
        return null;
    }

    @Override
    public ContentType getType() {
        return ContentType.LIST;
    }

    public List<String> getItems() {
        return items;
    }
    
}
