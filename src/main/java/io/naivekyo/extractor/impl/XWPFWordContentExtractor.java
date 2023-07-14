package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TableContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.IOUtils;
import io.naivekyo.support.word.ImageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class XWPFWordContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(XWPFWordContentExtractor.class);
    
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
                    if (ContentHelper.checkValidText(text)) {
                        this.getContents().add(new TextContent(ContentHelper.cleanExtractedText(text)));
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
        TableContent.TableContentBuilder tableBuilder = new TableContent.TableContentBuilder();
        List<XWPFTableRow> rows = t.getRows();
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
                    if (tRow == null)
                        tRow = new ArrayList<>();
                    if (!ContentHelper.checkValidText(text)) {
                        tRow.add(ContentHelper.EMPTY_STR);
                        continue;
                    }
                    tRow.add(ContentHelper.cleanExtractedText(text));
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
    private void extractParagraphContent(XWPFParagraph p) throws IOException {
        List<XWPFRun> runs = p.getRuns();
        StringBuilder sb = null;
        for (XWPFRun r : runs) {
            List<XWPFPicture> embeddedPictures = r.getEmbeddedPictures();
            if (embeddedPictures != null && embeddedPictures.size() > 0) {
                // 图片来源可能是: 插入的图片、内嵌的图片、画布
                for (XWPFPicture picture : embeddedPictures) {
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] data = pictureData.getData();
                    int typeId = pictureData.getPictureType();
                    ImageType imageType = ImageType.lookupByTypeId(typeId);
                    if (ImageType.UNKNOWN.equals(imageType)) {
                        LOG.error(String.format("word 类型: docx, 解析时出现未知的图片类型, org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE: %s", pictureData.getPictureType()));
                        continue;
                    } else if (ImageType.WMF.equals(imageType)) {
                        data = IOUtils.convertWMFToPNG(data);
                        imageType = ImageType.PNG;
                    } else if (ImageType.EMF.equals(imageType)) {
                        data = IOUtils.convertEMFToPNG(data);
                        imageType = ImageType.PNG;
                    }
                    this.getContents().add(new ImageContent(data, imageType.getMimeType(), imageType.getName()));
                }
            } else {
                // 抽取文本
                String text = r.text();
                if (!ContentHelper.checkValidText(text))
                    continue;
                if (sb == null)
                    sb = new StringBuilder(64);
                sb.append(text);
            }
        }
        if (sb != null) {
            this.getContents().add(new TextContent(ContentHelper.cleanExtractedText(sb.toString())));
        }
    }
    
}
