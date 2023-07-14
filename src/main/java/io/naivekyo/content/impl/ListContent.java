package io.naivekyo.content.impl;

import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>保存 ppt 文件的 TextBox 对应的列表内容, 目前仅支持文本内容</p>
 * <p><b>not thread-safe</b></p>
 */
public class ListContent implements DocContent {
    
    private final List<Item> items;

    public ListContent() {
        this.items = new ArrayList<>();
    }

    @Override
    public String getContent() {
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Item item : getItems()) {
            if (item.isHeader())
                sb.append(item.getText()).append("\r\n");
            else 
                sb.append(idx++).append(". ").append(item.getText()).append("\r\n");
        }
        return sb.toString();
    }

    @Override
    public String getHTMLWrapContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getItems().size(); i++) {
            Item item = getItems().get(i);
            if (i == 0) {
                if (item.isHeader())
                    sb.append("<p>").append(item.getText()).append("</p>")
                        .append("<ul>");
                else 
                    sb.append("<ul><li>").append(item.getText()).append("</li>");
            } else {
                sb.append("<li>").append(item.getText()).append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    @Override
    public ContentType getType() {
        return ContentType.LIST;
    }

    /**
     * 获取所有列表项
     * @return 所有列表项
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * 添加列表项
     * @param text 文本内容
     * @param isHeader 是否列表头
     */
    public void addItem(String text, boolean isHeader) {
        this.items.add(new Item(text, isHeader));
    }

    /**
     * 添加列表项
     * @param text 文本内容
     */
    public void addItem(String text) {
        this.addItem(text, false);
    }

    /**
     * 判断当前内容是否为空
     * @return true or false
     */
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListContent that = (ListContent) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return "ListContent{" +
                "items=" + items +
                '}';
    }

    static class Item {
        
        private final String text;
        
        private final boolean headerFlag;

        public Item(String text, boolean isHeader) {
            this.text = text;
            this.headerFlag = isHeader;
        }

        public String getText() {
            return text;
        }

        public boolean getHeaderFlag() {
            return headerFlag;
        }

        /**
         * 判断当前 item 是否为头信息
         * @return true or false
         */
        public boolean isHeader() {
            return headerFlag;
        }
        
    }
}
