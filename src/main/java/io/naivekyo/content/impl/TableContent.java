package io.naivekyo.content.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;
import io.naivekyo.support.function.ContentConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>word 表格内容(当前版本只存储文本, 不考虑图片)</p>
 * <p><b>not thread-safe</b></p>
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:29
 */
public class TableContent implements DocContent {
    
    // TODO 适配带 row/col span 的表格
    
    /**
     * 表格采用二维数组格式, row + col
     */
    private final List<List<TextContent>> rawContent;

    /**
     * 表格的总行数
     */
    private int rowSize;

    /**
     * 表格的最大列数
     */
    private int colSize;

    private TableContent() {
        this.rawContent = new ArrayList<>();
    }

    public List<List<TextContent>> getRawContent() {
        return rawContent;
    }

    public int getRowSize() {
        return rowSize;
    }

    public int getColSize() {
        return colSize;
    }

    @Override
    public String getContent() {
        List<List<TextContent>> data = getRawContent();
        if (data == null || data.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            StringBuilder colBuilder = new StringBuilder();
            List<TextContent> row = data.get(i);
            for (int j = 0; j < row.size(); j++) {
                TextContent col = row.get(j);
                colBuilder.append(col.getRawContent());
                if (j < row.size() - 1)
                    colBuilder.append("\t");
            }
            sb.append(colBuilder);
            if (i < data.size() - 1)
                sb.append("\r\n");
        }

        return sb.toString();
    }

    @Override
    public String getHTMLWrapContent() {
        return ContentHelper.convertTableDataToHtml(this);
    }

    @Override
    public String getHTMLWrapContent(ContentConverter<DocContent, String> converter) {
        if (converter == null)
            return getHTMLWrapContent();
        return ContentHelper.convertTableDataToHtml(this, converter);
    }

    @Override
    public ContentType getType() {
        return ContentType.TABLE;
    }

    public static class TableContentBuilder {

        private final TableContent table;

        public TableContentBuilder() {
            this.table = new TableContent();
        }

        public TableContentBuilder addRow(List<String> row) {
            if (row == null || row.isEmpty())
                throw new RuntimeException("表格行数据不能为空");
            this.table.rawContent.add(row.stream().filter(ContentHelper::hasText).map(TextContent::new).collect(Collectors.toList()));
            return this;
        }

        public TableContent build() {
            this.table.rowSize = table.rawContent.size();
            int max = 0;
            for (List<TextContent> col : this.table.rawContent) {
                max = Math.max(col.size(), max);
            }
            this.table.colSize = max;
            return table;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableContent that = (TableContent) o;
        return rowSize == that.rowSize && colSize == that.colSize && Objects.equals(rawContent, that.rawContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawContent, rowSize, colSize);
    }

    @Override
    public String toString() {
        return "TableContent{" +
                "rawContent=" + rawContent +
                ", rowSize=" + rowSize +
                ", colSize=" + colSize +
                '}';
    }
    
}
