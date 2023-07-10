package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TableContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ICell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFSDTCell;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * .docx 后缀的 Word 文件内容抽取器实现
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:39
 */
@Slf4j
public class XWPFWordContentExtractor extends AbstractContentExtractor {

    public XWPFWordContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    /**
     * 目前仅抽取正文文本内容以及图片数据
     */
    @Override
    protected void doExtract() {
        XWPFDocument xwpfDocument = null;
        Exception markEx = null;
        try {
            xwpfDocument = new XWPFDocument(getDocByteStream());
            List<IBodyElement> bodyElements = xwpfDocument.getBodyElements();
            for (IBodyElement e : bodyElements) {
                BodyElementType elementType = e.getElementType();
                if (BodyElementType.PARAGRAPH.equals(elementType)) {
                    extractParagraphContent((XWPFParagraph) e);
                } else if (BodyElementType.TABLE.equals(elementType)) {
                    extractTableContent((XWPFTable) e);
                } else if (BodyElementType.CONTENTCONTROL.equals(elementType)) {
                    XWPFSDT sdt = (XWPFSDT) e;
                    String text = sdt.getContent().getText();
                    if (ContentHelper.hasText(text)) {
                        this.getContents().add(new TextContent(ContentHelper.cleanWordText(text)));
                    }
                }
            }
        } catch (IOException e) {
            markEx = e;
        } finally {
            try {
                if (xwpfDocument != null)
                    xwpfDocument.close();
            } catch (IOException e) {
                markEx = e;
            }
        }
        if (markEx != null)
            throw new RuntimeException(markEx);
    }

    /**
     * 抽取表格文本内容(暂不考虑内嵌图片)
     * @param t {@link XWPFTable}
     */
    private void extractTableContent(XWPFTable t) {
        List<XWPFTableRow> rows = t.getRows();
        TableContent.TableContentBuilder tableBuilder = new TableContent.TableContentBuilder();
        for (XWPFTableRow row : rows) {
            // 目前仅处理文本
            List<String> tRow = null;
            List<ICell> cells = row.getTableICells();
            for (ICell cell : cells) {
                String text = null;
                if (cell instanceof XWPFTableCell) {
                    text = ((XWPFTableCell) cell).getTextRecursively();
                } else if (cell instanceof XWPFSDTCell) {
                    text = ((XWPFSDTCell) cell).getContent().getText();
                }
                if (text != null) {
                    if (!ContentHelper.checkWordValidText(text))
                        continue;
                    // 表格这里为了行列的完整, 所以允许空字符存在
                    if (tRow == null)
                        tRow = new ArrayList<>();
                    tRow.add(ContentHelper.cleanWordText(text));
                }
            }
            if (tRow != null && !tRow.isEmpty()) {
                boolean match = tRow.stream().allMatch(e -> e == null || "".equals(e));
                if (!match)
                    tableBuilder.addRow(tRow);
            }
        }
        this.getContents().add(tableBuilder.build());
    }

    /**
     * 抽取段落内容
     * @param p {@link XWPFParagraph}
     */
    private void extractParagraphContent(XWPFParagraph p) {
        List<XWPFRun> runs = p.getRuns();
        StringBuilder sb = null;
        for (XWPFRun r : runs) {
            List<XWPFPicture> embeddedPictures = r.getEmbeddedPictures();
            if (embeddedPictures != null && embeddedPictures.size() > 0) {  // 图片来源可能是: 插入的图片、内嵌的图片、画布
                for (XWPFPicture picture : embeddedPictures) {
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] data = pictureData.getData();
                    String type = ContentHelper.getXWPFPictureType(pictureData.getPictureType());
                    if (type == null)
                        log.error("word 类型: docx, 解析时出现未知的图片类型, picture type: {}", pictureData.getPictureType());
                    this.getContents().add(new ImageContent(data, type));
                }
            } else {
                // 抽取文本
                String text = r.text();
                if (!ContentHelper.checkWordValidText(text))
                    continue;
                if (sb == null)
                    sb = new StringBuilder();
                sb.append(text);
            }
        }
        if (sb != null) {
            String text = sb.toString();
            if (ContentHelper.hasText(text)) {
                this.getContents().add(new TextContent(ContentHelper.cleanWordText(text)));
            }
        }
    }
    
}
