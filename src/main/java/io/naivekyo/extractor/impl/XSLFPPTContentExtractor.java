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
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFComment;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFNotesMaster;
import org.apache.poi.xslf.usermodel.XSLFObjectShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * .pptx 后缀的 ppt 文件内容抽取器实现
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/">examples</a>
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/xslf/">XSLF Examples</a>
 * @author NaiveKyo
 * @since 1.0
 */
public class XSLFPPTContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(XSLFPPTContentExtractor.class);
    
    private static final Object DUMMY_OBJ = new Object();
    
    /**
     * pptx 中 media 文件名的 map, 用于防止多次读取同一张图片的情况 
     */
    private Map<String, Object> mediaNameMap = null;
    
    public XSLFPPTContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        XMLSlideShow xmls = null;
        Exception mark = null;
        
        try {
            xmls = new XMLSlideShow(getDocByteStream());
            
            // 获取文档中所有的嵌入文件(音频/视频等等)
            List<PackagePart> embeddedFiles = xmls.getAllEmbeddedParts();
            if (embeddedFiles != null && !embeddedFiles.isEmpty())
                LOG.warn("处理 .pptx 文件时, 发现内嵌文件需要处理, TODO");
            
            // 获取所有 media 数据(图片/视频/音频), 目前只处理图片 TODO
            List<XSLFPictureData> mediaDataList = xmls.getPictureData();
            mediaNameMap = new HashMap<>((int) (mediaDataList.size() / .45f + 1f));
            
            // 处理幻灯片母版, 抽取图片数据
            List<XSLFSlideMaster> slideMasters = xmls.getSlideMasters();
            for (XSLFSlideMaster slideMaster : slideMasters) {
                for (XSLFShape shape : slideMaster.getShapes()) {
                    if (shape instanceof XSLFPictureShape) {
                        handlePicture(-1, ((XSLFPictureShape) shape).getPictureData());
                    }
                }
            }

            // TODO 暂时不处理备注母版
            XSLFNotesMaster notesMaster = xmls.getNotesMaster();
            
            // 分页处理所有幻灯片
            for (XSLFSlide slide : xmls.getSlides()) {
                int slideNumber = slide.getSlideNumber();   // 幻灯片页码, 从 1 开始计数
                getContents().add(new TextContent(String.format("第 %d 页", slideNumber)));

                // 批注
                handleComments(slideNumber, slide.getComments());

                // 备注
                handleNotes(slideNumber, slide.getNotes());

                // 处理 layout
                XSLFSlideLayout layout = slide.getSlideLayout();
                for (XSLFShape shape : layout.getShapes()) {
                    if (shape instanceof XSLFPictureShape) {
                        handlePicture(0, ((XSLFPictureShape) shape).getPictureData());
                    }
                }
                
                // 按照 shape 的类型处理所有内容
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFPictureShape) {
                        // 处理图片
                        handlePicture(slideNumber, ((XSLFPictureShape) shape).getPictureData());
                    } else if (shape instanceof XSLFTextBox) {
                        // 处理列表
                        handleTextBox(((XSLFTextBox) shape));
                    } else if (shape instanceof XSLFTextShape) {
                        // 处理常规文本
                        handleText(((XSLFTextShape) shape));
                    } else if (shape instanceof XSLFTable) {
                        // 处理表格
                        handleTable(((XSLFTable) shape));
                    } else if (shape instanceof XSLFObjectShape) {
                        // 处理 OLE
                        handleOLEShape(slideNumber, ((XSLFObjectShape) shape));
                    } else if (shape instanceof XSLFGroupShape) {
                        // 处理 group
                        handleGroupShape(slideNumber, (XSLFGroupShape) shape);
                    } else {
                        LOG.warn(String.format("pptx 内容抽取, 当前幻灯片页码: %d, 待处理的 Shape 信息: classType: %s, shapeName: %s",
                                slideNumber, shape.getClass().getName(), shape.getShapeName()));
                    }
                }
            }

            // 处理完所有幻灯片后, 查看是否有遗漏的图片没有抽取
            for (XSLFPictureData picData : mediaDataList) {
                String name = picData.getFileName();
                if (ContentHelper.hasText(name)) {
                    if (mediaNameMap.get(name) == null) {
                        byte[] data = picData.getData();
                        PictureData.PictureType pt = picData.getType();
                        if (pt == null) {
                            LOG.warn(String.format("TODO: 处理 .pptx 文件时, 读取到无法处理的 mime-type 文件, 数据类型: %s", picData.getContentType()));
                            continue;
                        }
                        if (PictureData.PictureType.WDP.equals(pt)) {
                            LOG.warn("pptx 文件, 暂不处理 Microsoft Windows Media Photo image (.wdp) 图片文件");
                            continue;
                        }
                        String mimeType = pt.contentType;
                        String extension = pt.extension;
                        getContents().add(new ImageContent(data, mimeType, extension.substring(extension.indexOf(".") + 1)));
                    }
                }
            }
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (xmls != null)
                    xmls.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * 递归处理 GroupShape
     * @param page  幻灯片页码
     * @param shape {@link XSLFGroupShape}
     * @throws IOException IOException
     */
    private void handleGroupShape(int page, XSLFGroupShape shape) throws IOException {
        for (XSLFShape gs : shape.getShapes()) {
            if (gs instanceof XSLFPictureShape) {
                handlePicture(page, ((XSLFPictureShape) gs).getPictureData());
            } else if (gs instanceof XSLFTextBox) {
                // 处理列表
                handleTextBox(((XSLFTextBox) gs));
            } else if (gs instanceof XSLFTextShape) {
                // 处理常规文本
                handleText(((XSLFTextShape) gs));
            } else if (gs instanceof XSLFTable) {
                // 处理表格
                handleTable(((XSLFTable) gs));
            } else if (gs instanceof XSLFObjectShape) {
                // 处理 OLE
                handleOLEShape(page, ((XSLFObjectShape) gs));
            } else if (gs instanceof XSLFGroupShape) {
                handleGroupShape(page, ((XSLFGroupShape) gs));
            } else {
                LOG.warn(String.format("pptx 内容抽取, 当前幻灯片页码: %d, 待处理的 Shape 信息: classType: %s, shapeName: %s",
                        page, gs.getClass().getName(), gs.getShapeName()));
            }
        }
    }

    /**
     * 处理和当前幻灯片关联的批注信息, 仅抽取文本
     * @param page 当前页码
     * @param comments {@link XSLFComment}
     */
    private void handleComments(int page, List<XSLFComment> comments) {
        if (comments != null && !comments.isEmpty()) {
            List<String> commentList = null;
            for (XSLFComment c : comments) {
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
     * 处理当前幻灯片关联的备注信息, 仅抽取文本
     * @param page 当前页码
     * @param notes {@link XSLFNotes}
     */
    private void handleNotes(int page, XSLFNotes notes) {
        if (notes != null) {
            List<List<XSLFTextParagraph>> textParagraphs = notes.getTextParagraphs();
            List<String> noteList = null;
            for (List<XSLFTextParagraph> textParagraph : textParagraphs) {
                for (XSLFTextParagraph textRuns : textParagraph) {
                    StringBuilder sb = null;
                    for (XSLFTextRun run : textRuns.getTextRuns()) {
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
     * 抽取 {@link XSLFTextBox} 列表数据
     * @param textBoxShape {@link XSLFTextBox}
     */
    private void handleTextBox(XSLFTextBox textBoxShape) {
        ListContent listContent = null;
        List<XSLFTextParagraph> textParagraphs = textBoxShape.getTextParagraphs();
        for (int i = 0; i < textParagraphs.size(); i++) {
            XSLFTextParagraph textParagraph = textParagraphs.get(i);
            List<XSLFTextRun> textRuns = textParagraph.getTextRuns();
            StringBuilder sb = null;
            for (XSLFTextRun run : textRuns) {
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
     * 抽取表格数据
     * @param tableShape {@link XSLFTable}
     */
    private void handleTable(XSLFTable tableShape) {
        int numberOfRows = tableShape.getNumberOfRows();
        int numberOfColumns = tableShape.getNumberOfColumns();
        TableContent.TableContentBuilder builder = null;
        for (int i = 0; i < numberOfRows; i++) {
            List<String> row = null;
            for (int j = 0; j < numberOfColumns; j++) {
                // TODO 处理带行/列 span 的表格
                XSLFTableCell cell = tableShape.getCell(i, j);
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
     * 抽取文本内容
     * @param textShape {@link XSLFTextShape}
     */
    private void handleText(XSLFTextShape textShape) {
        List<XSLFTextParagraph> allTextParagraph = textShape.getTextParagraphs();
        if (allTextParagraph == null || allTextParagraph.size() == 0)
            return;
        for (XSLFTextParagraph textParagraph : allTextParagraph) {
            StringBuilder sb = null;
            for (XSLFTextRun textRun : textParagraph.getTextRuns()) {
                String rawText = textRun.getRawText();
                if (ContentHelper.checkValidText(rawText)) {
                    if (sb == null)
                        sb = new StringBuilder();
                    sb.append(rawText);
                }
            }
            if (sb != null)
                getContents().add(new TextContent(ContentHelper.cleanExtractedText(sb.toString())));
        }
    }

    /**
     * 解析 OLE 内嵌对象, 根据类型采用对应措施
     * @param page 幻灯片页码
     * @param objShape {@link XSLFObjectShape}
     */
    private void handleOLEShape(int page, XSLFObjectShape objShape) {
        LOG.info(String.format("处理 .pptx 文件时, 页码: %d, 发现待处理的 OLE 类型 , fullName : %s, shapeName : %s", 
                page, objShape.getFullName(), objShape.getShapeName()));
    }

    /**
     * 抽取图片数据
     * @param page 当前幻灯片页码, -1 表示来自母版
     * @param pictureData {@link XSLFPictureShape}
     * @throws IOException IOException
     */
    private void handlePicture(int page, XSLFPictureData pictureData) throws IOException {
        PictureData.PictureType pt = pictureData.getType();
        if (pt == null) {
            LOG.error(String.format("处理 .pptx 文件时, 读取到无效的图片数据, 页码: %s, 数据类型: %s", page == -1 ? "母版" : page, pictureData.getContentType()));
            return;
        }
        byte[] data = pictureData.getData();
        String name = pictureData.getFileName();
        if (ContentHelper.hasText(name)) {
            if (mediaNameMap.put(name, DUMMY_OBJ) == null) {
                extractPicture(data, pt);
            }
        } else
            extractPicture(data, pt);
    }

    private void extractPicture(byte[] data, PictureData.PictureType pt) throws IOException {
        String mimeType = pt.contentType;
        String extension = pt.extension;
        // WMF 和 EMF 文件全部转换为 PNG 图片
        if (PictureData.PictureType.WMF.equals(pt)) {
            data = IOUtils.convertWMFToPNG(data);
            getContents().add(new ImageContent(data, ImageType.PNG.getMimeType(), ImageType.PNG.getExtension()));
        } else if (PictureData.PictureType.EMF.equals(pt)) {
            data = IOUtils.convertEMFToPNG(data);
            getContents().add(new ImageContent(data, ImageType.PNG.getMimeType(), ImageType.PNG.getExtension()));
        } else if (PictureData.PictureType.WDP.equals(pt)) {
            LOG.warn("暂不处理 Microsoft Windows Media Photo image (.wdp) 图片文件");
        } else {
            getContents().add(new ImageContent(data, mimeType, extension.substring(extension.indexOf(".") + 1)));
        }
    }

}
