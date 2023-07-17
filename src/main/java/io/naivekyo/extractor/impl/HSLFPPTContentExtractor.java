package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.ListContent;
import io.naivekyo.content.impl.TableContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.IOUtils;
import io.naivekyo.support.word.ImageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hslf.usermodel.HSLFAutoShape;
import org.apache.poi.hslf.usermodel.HSLFComment;
import org.apache.poi.hslf.usermodel.HSLFMasterSheet;
import org.apache.poi.hslf.usermodel.HSLFNotes;
import org.apache.poi.hslf.usermodel.HSLFObjectShape;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFPictureShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideMaster;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSoundData;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTableCell;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hslf.usermodel.HSLFTitleMaster;
import org.apache.poi.sl.usermodel.ObjectMetaData;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>.ppt 后缀的 ppt 文件内容抽取器实现</p>
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/">apache poi examples</a>
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/hslf/DataExtraction.java">DataExtraction.java</a>
 */
@SuppressWarnings("rawtypes")
public class HSLFPPTContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(HSLFPPTContentExtractor.class);
    
    public HSLFPPTContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    /**
     * 映射图片在流中的索引位置, 确保图片只被读取一次
     */
    private BitSet picBitSet = null;
    
    @Override
    protected void doExtract() {
        SlideShow hslf = null;
        Exception mark = null;
        try {
            hslf = SlideShowFactory.create(getDocByteStream());
            
            if (hslf instanceof HSLFSlideShow) {
                HSLFSlideShow hslfSlideShow = (HSLFSlideShow) hslf;
                
                // 图片数据的 bitset
                picBitSet = new BitSet(hslfSlideShow.getPictureData().size());
                
                // TODO 暂不处理音频数据
                HSLFSoundData[] soundDataList = hslfSlideShow.getSoundData();

                // 幻灯片母版只提取图片
                List<HSLFSlideMaster> slideMasters = hslfSlideShow.getSlideMasters();
                for (HSLFSlideMaster sm : slideMasters) {
                    for (HSLFShape shape : sm.getShapes()) {
                        if (shape instanceof HSLFPictureShape) {
                            handlePictureShape(-1, "", (HSLFPictureShape) shape);
                        }
                    }
                }
                
                // TODO title 母版暂不处理
                List<HSLFTitleMaster> titleMasters = hslfSlideShow.getTitleMasters();

                // 分页解析所有幻灯片
                List<HSLFSlide> slides = hslfSlideShow.getSlides();
                for (HSLFSlide currentSlide : slides) {
                    int page = currentSlide.getSlideNumber();
                    getContents().add(new TextContent(String.format("第 %d 页", page)));
                    
                    // 处理批注文字
                    handleComment(page, currentSlide.getComments());
                    // 处理备注文字
                    handleNote(page, currentSlide.getNotes());
                    // 处理 layout TODO
                    HSLFMasterSheet slideLayout = currentSlide.getSlideLayout();
                    for (HSLFShape shape : slideLayout.getShapes()) {
                        if (shape instanceof HSLFPictureShape)
                            handlePictureShape(page, "", (HSLFPictureShape) shape);
                    }
                    
                    // 当前幻灯片的所有内容
                    List<HSLFShape> shapeList = currentSlide.getShapes();
                    for (HSLFShape shape : shapeList) {
                        if (shape instanceof HSLFObjectShape) {
                            // MS OLE(Object Linking and Embedding) documents
                            handleOLEShape(page, (HSLFObjectShape) shape);
                        } else if (shape instanceof HSLFPictureShape) {
                            // 图片
                            handlePictureShape(page, "", (HSLFPictureShape) shape);
                        } else if (shape instanceof HSLFTable) {
                            // 表格
                            handleTableShape((HSLFTable) shape);
                        } else if (shape instanceof HSLFAutoShape) {
                            // 文本
                            handleTextParagraphs(((HSLFAutoShape) shape).getTextParagraphs());
                        } else if (shape instanceof HSLFTextBox) {
                            // 列表
                            handleTextBox((HSLFTextBox) shape);
                        } else {
                            LOG.warn(String.format("ppt 内容抽取, 当前幻灯片页码: %d, 待处理的 Shape 信息: classType: %s, shapeType: %s, shapeName: %s TODO",
                                    page, shape.getClass().getName(), shape.getShapeType(), shape.getShapeName()));
                        }
                    }
                }
            } else {
                LOG.error(String.format("无效的 ppt 类型, 当前抽取器处理 HSLF 类型的 ppt, 待处理文档为 %s", hslf.getClass().getName()));
            }
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (hslf != null)
                    hslf.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * 处理备注文本
     * @param page 当前幻灯片的页码
     * @param notes {@link HSLFNotes}
     */
    private void handleNote(int page, HSLFNotes notes) {
        if (notes != null) {
            List<List<HSLFTextParagraph>> textParagraphs = notes.getTextParagraphs();
            List<String> noteList = null;
            for (List<HSLFTextParagraph> textParagraph : textParagraphs) {
                for (HSLFTextParagraph textRuns : textParagraph) {
                    StringBuilder sb = null;
                    for (HSLFTextRun run : textRuns.getTextRuns()) {
                        String text = run.getRawText();
                        if (ContentHelper.checkValidText(text)) {
                            if (sb == null)
                                sb = new StringBuilder();
                            sb.append(text);
                        }
                    }
                    if (sb != null) {
                        if (noteList == null)
                            noteList = new ArrayList<>();
                        noteList.add(sb.toString());
                    }
                }
            }
            if (noteList != null && !noteList.isEmpty()) {
                getContents().add(new TextContent(String.format("页码: %d -- 备注: ", page)));
                getContents().addAll(noteList.stream().map(TextContent::new).collect(Collectors.toList()));
            }
        }
    }

    /**
     * 处理批注文本
     * @param page 当前幻灯片的页码
     * @param comments {@link HSLFComment}
     */
    private void handleComment(int page, List<HSLFComment> comments) {
        if (comments != null && !comments.isEmpty()) {
            List<String> commentList = null;
            for (HSLFComment c : comments) {
                String text = c.getText();
                if (ContentHelper.checkValidText(text)) {
                    if (commentList == null) {
                        commentList = new ArrayList<>();
                    }
                    commentList.add(ContentHelper.cleanExtractedText(text));
                }
            }
            if (commentList != null && !commentList.isEmpty()) {
                getContents().add(new TextContent(String.format("页码: %d -- 批注: ", page)));
                getContents().addAll(commentList.stream().map(TextContent::new).collect(Collectors.toList()));
            }
        }
    }

    /**
     * 处理 OLE document
     * @param page current slide page number
     * @param ole Object Linking and Embedding
     * @see org.apache.poi.sl.usermodel.ObjectMetaData
     */
    private void handleOLEShape(int page, HSLFObjectShape ole) throws IOException {
        String name = ole.getInstanceName();
        ObjectMetaData.Application lookup = ObjectMetaData.Application.lookup(ole.getProgId());
        switch (name == null ? "" : name) {
            case "Worksheet": {
                // TODO excel 区分 xls, xlsx
                if (lookup != null) {
                    if (lookup.equals(ObjectMetaData.Application.EXCEL_V8)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(String.format(".ppt 文件, 页码: %d, 读取到 EXCEL_V8 OLE type TODO", page));
                    } else if (lookup.equals(ObjectMetaData.Application.EXCEL_V12)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(String.format(".ppt 文件, 页码: %d, 读取到 EXCEL_V12 OLE type TODO", page));
                    } else {
                        LOG.error(String.format("读取 .ppt 文件时, 页码: %d, 解析到未知的 excel OLE 类型: %s TODO", page, lookup));
                    }
                }
                break;
            }
            case "文档":
            case "Document": {
                // TODO word 区分 doc, docx
                if (lookup != null) {
                    if (lookup.equals(ObjectMetaData.Application.WORD_V8)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(String.format(".ppt 文件, 页码: %d, 读取到 WORD_V8 OLE type TODO", page));
                    } else if (lookup.equals(ObjectMetaData.Application.WORD_V12)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(".ppt 文件, 读取到 WORD_V12 OLE type TODO");
                    } else {
                        LOG.error(String.format("读取 .ppt 文件时, 页码: %d, 解析到未知的 word OLE 类型: %s TODO", page, lookup));
                    }
                }
                break;
            }
            case "CorelDRAW": // vector graphics file (.cdr)
            case "Equation" : // 数学公式
            case "Visio":   // Visio 文件
            case "位图图像":
            case "BMP 图象":
            case "图片":
            case "公式": {
                handlePictureShape(page, name, ole);
                break;
            }
            default: {
                // 未知的 OLE 类型
                LOG.error(String.format("处理 .ppt 文件时, 页码: %d, 读取到未知类型的 OLE 文件: classType: %s, name: %s, full name: %s", 
                        page, ole.getClass().getName(), name, ole.getFullName()));
            }
        }
    }
    
    /**
     * 处理列表数据
     * @param textBox Represents a TextFrame shape in PowerPoint.
     */
    private void handleTextBox(HSLFTextBox textBox) {
        ListContent listContent = null;
        int i = 0;
        for (HSLFTextParagraph tr : textBox) {
            List<HSLFTextRun> textRuns = tr.getTextRuns();
            StringBuilder sb = null;
            for (HSLFTextRun run : textRuns) {
                String text = run.getRawText();
                if (ContentHelper.checkValidText(text)) {
                    if (sb == null)
                        sb = new StringBuilder();
                    sb.append(text);
                }
            }
            if (sb != null) {
                String text = sb.toString();
                if (listContent == null)
                    listContent = new ListContent();
                if (i == 0)
                    listContent.addItem(ContentHelper.cleanExtractedText(text), true);
                else
                    listContent.addItem(ContentHelper.cleanExtractedText(text));
            }
            i++;
        }
        if (listContent != null && !listContent.isEmpty()) {
            getContents().add(listContent);
        }
    }

    /**
     * 处理表格数据
     * @param shape Represents a table in a PowerPoint presentation
     */
    private void handleTableShape(HSLFTable shape) {
        int numberOfRows = shape.getNumberOfRows();
        int numberOfColumns = shape.getNumberOfColumns();
        TableContent.TableContentBuilder builder = null;
        for (int i = 0; i < numberOfRows; i++) {
            List<String> row = null;
            for (int j = 0; j < numberOfColumns; j++) {
                // TODO 处理带行/列 span 的表格
                HSLFTableCell cell = shape.getCell(i, j);
                if (cell != null) {
                    String text = cell.getText();
                    if (row == null)
                        row = new ArrayList<>();
                    if (ContentHelper.hasText(text))
                        row.add(ContentHelper.cleanExtractedText(text));
                    else 
                        row.add(ContentHelper.EMPTY_STR);
                }
            }
            if (row != null && !row.isEmpty()) {
                if (builder == null)
                    builder = new TableContent.TableContentBuilder();
                builder.addRow(row);
            }
        }
        if (builder != null)
            getContents().add(builder.build());
    }

    /**
     * 处理文本数据
     * @param textParagraphs 所有段落
     */
    private void handleTextParagraphs(List<HSLFTextParagraph> textParagraphs) {
        if (textParagraphs == null || textParagraphs.isEmpty())
            return;
        for (HSLFTextParagraph textParagraph : textParagraphs) {
            StringBuilder sb = null;
            for (HSLFTextRun run : textParagraph.getTextRuns()) {
                String text = run.getRawText();
                if (ContentHelper.checkValidText(text)) {
                    if (sb == null)
                        sb = new StringBuilder();
                    sb.append(text);
                }
            }
            if (sb != null) {
                getContents().add(new TextContent(ContentHelper.cleanExtractedText(sb.toString())));
            }
        }
    }
    
    /**
     * 处理图片数据
     * @param page 当前图片所处幻灯片的页码, -1 表示从母版提取的图片
     * @param oleName 当前 OLE 类型名称
     * @param pic Represents a picture in a PowerPoint document.
     * @throws IOException IOException
     */
    private void handlePictureShape(int page, String oleName, HSLFPictureShape pic) throws IOException {
        int picIndex = pic.getPictureIndex();
        if (picIndex == 0) {
            if (page == -1)
                LOG.error(String.format("处理 .ppt 文件, 从母版解析图片数据时发现无效数据, OLE name: %s", oleName));
            else 
                LOG.error(String.format("处理 .ppt 文件, 页码: %d, 解析图片数据时发现无效数据, OLE name: %s", page, oleName));
            return;
        }
        
        if (!picBitSet.get(picIndex - 1)) {
            HSLFPictureData pictureData = pic.getPictureData();
            if (pictureData == null) {
                LOG.error(String.format("处理 .ppt 文件时, 页码: %d, OLE name: %s, 无法获得图片数据, shape 类型: %s", page, oleName, pic.getShapeName()));
                picBitSet.set(picIndex - 1);
                return;
            }
            byte[] data = pictureData.getData();
            PictureData.PictureType pt = pictureData.getType();
            String extension = pt.extension;
            String mimeType = pt.contentType;
            // WMF 和 EMF 文件全部转换为 PNG 图片
            if (PictureData.PictureType.WMF.equals(pt)) {
                data = IOUtils.convertWMFToPNG(data);
                getContents().add(new ImageContent(data, ImageType.PNG.getMimeType(), ImageType.PNG.getExtension()));
            } else if (PictureData.PictureType.EMF.equals(pt)) {
                data = IOUtils.convertEMFToPNG(data);
                getContents().add(new ImageContent(data, ImageType.PNG.getMimeType(), ImageType.PNG.getExtension()));
            } else {
                getContents().add(new ImageContent(data, mimeType, extension.substring(extension.indexOf(".") + 1)));
            }
            picBitSet.set(picIndex - 1);
        }
    }
    
}
