package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hslf.usermodel.HSLFAutoShape;
import org.apache.poi.hslf.usermodel.HSLFBackground;
import org.apache.poi.hslf.usermodel.HSLFComment;
import org.apache.poi.hslf.usermodel.HSLFNotes;
import org.apache.poi.hslf.usermodel.HSLFObjectData;
import org.apache.poi.hslf.usermodel.HSLFObjectShape;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFPictureShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSoundData;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.ObjectMetaData;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>.ppt 后缀的 ppt 文件内容抽取器实现</p>
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/">apache poi examples</a>
 * @see <a href="https://svn.apache.org/repos/asf/poi/trunk/poi-examples/src/main/java/org/apache/poi/examples/hslf/DataExtraction.java">DataExtraction.java</a>
 */
public class HSLFPPTContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(HSLFPPTContentExtractor.class);
    
    public HSLFPPTContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    private Map<String, Integer> shapeCounter = new HashMap<>();
    
    @Override
    protected void doExtract() {
        SlideShow spSlideShow = null;
        Exception mark = null;
        try {
            spSlideShow = SlideShowFactory.create(getDocByteStream());
            
            if (spSlideShow instanceof HSLFSlideShow) {
                HSLFSlideShow hslfSlideShow = (HSLFSlideShow) spSlideShow;
                
                // 所有图片数据
                List<HSLFPictureData> pictureDataList = hslfSlideShow.getPictureData();
                System.out.println("total picture number : " + pictureDataList.size());
                // 先持久化
                for (int i = 0; i < pictureDataList.size(); i++) {
                    HSLFPictureData p = pictureDataList.get(i);
                    String mimeType = p.getType().contentType;
                    String extension = p.getType().extension;
                    byte[] data = p.getData();
                    IOUtils.saveFile(data, String.format("C:\\%d%s", i, extension));
                }
                
                // TODO 所有音频数据
                HSLFSoundData[] soundDataList = hslfSlideShow.getSoundData();

                // 所有幻灯片
                List<HSLFSlide> slides = hslfSlideShow.getSlides();
                
                // 统计数量
                int picture = 0;
                int ole = 0;
                int table = 0;
                int auto = 0;
                int box = 0;
                
                for (HSLFSlide currentSlide : slides) {
                    System.out.println("number : " + currentSlide.getSlideNumber());
                    
                    // 处理备注文字
                    List<HSLFComment> comments = currentSlide.getComments();

                    // 处理注释文字
                    HSLFNotes notes = currentSlide.getNotes();

                    HSLFBackground background = currentSlide.getBackground();

                    // 当前幻灯片的所有内容
                    List<HSLFShape> shapeList = currentSlide.getShapes();
                    for (HSLFShape shape : shapeList) {
                        shapeCounter.compute(shape.getClass().getName(), (k, v) -> v == null ? 1 : v + 1);
                        System.out.println(shape.getClass().getName() + " --> " + shape.getShapeType() + " --> " + shape.getShapeName());
                        if (shape instanceof HSLFObjectShape) {
                            // 抽取 MS OLE(Object Linking and Embedding) documents
                            ole++;
                            HSLFObjectShape obj = (HSLFObjectShape) shape;
                            handleOLEShape(obj, ole);
                        } else if (shape instanceof HSLFPictureShape) {
                            picture++;
                            HSLFPictureShape pic = (HSLFPictureShape) shape;
                            System.out.println("pic : " + pic.getPictureIndex());
                            HSLFPictureData pd = pictureDataList.get(pic.getPictureIndex() - 1);
                            IOUtils.saveFile(pd.getData(), String.format("C:\\%d-pic%s", pic.getPictureIndex() - 1, pd.getType().extension));
                        } else if (shape instanceof HSLFTable) {
                            table++;
                            // TODO 处理表格文本
                            HSLFTable tableShape = (HSLFTable) shape;
                            int numberOfRows = tableShape.getNumberOfRows();
                            int numberOfColumns = tableShape.getNumberOfColumns();
                            
                        } else if (shape instanceof HSLFAutoShape) {
                            auto++;
                            // TODO 处理文本
                            HSLFAutoShape autoShape = (HSLFAutoShape) shape;
                            autoShape.getPlaceholder();
                            for (HSLFTextParagraph textParagraph : autoShape.getTextParagraphs()) {
                                for (HSLFTextRun run : textParagraph.getTextRuns()) {
                                    System.out.println(run.getRawText());
                                }
                            }
                        } else if (shape instanceof HSLFTextBox) {
                            box++;
                            // TODO 对应列表
                            HSLFTextBox textBox = (HSLFTextBox) shape;
                            System.out.println(textBox.getText());
                        } else {
                            LOG.warn(String.format("ppt 内容抽取, 当前幻灯片页码: %d, 待处理的 Shape 类型: %s", currentSlide.getSlideNumber(), shape.getClass().getName()));
                        }
                    }


                    List<List<HSLFTextParagraph>> textParagraphs = currentSlide.getTextParagraphs();
                    for (List<HSLFTextParagraph> paragraph : textParagraphs) {
                        for (HSLFTextParagraph p : paragraph) {
                            List<HSLFTextRun> textRuns = p.getTextRuns();
                            for (HSLFTextRun run : textRuns) {
                                String rawText = run.getRawText();
                                // System.out.println(rawText);
                            }
                        }
                    }
                }
                shapeCounter.entrySet().forEach(System.out::println);
                System.out.printf("picture: %d, ole: %d, table: %d, auto: %d, box: %d%n", picture, ole, table, auto, box);
            } else {
                LOG.error(String.format("无效的 ppt 类型, 当前抽取器处理 HSLF 类型的 ppt, 待处理文档为 %s", spSlideShow.getClass().getName()));
            }
            
            // 抽取所有文本
            SlideShowExtractor extractor = new SlideShowExtractor(spSlideShow);
            extractor.setMasterByDefault(false);
            extractor.setCommentsByDefault(true);
            extractor.setNotesByDefault(true);
            String text = extractor.getText();
            // System.out.println(text);
        } catch (IOException e) {
            mark = e;
        } finally {
            try {
                if (spSlideShow != null)
                    spSlideShow.close();
            } catch (IOException e) {
                mark = e;
            }
        }
        if (mark != null)
            throw new RuntimeException(mark);
    }

    /**
     * 处理 OLE document
     * @param ole Object Linking and Embedding
     * @see org.apache.poi.sl.usermodel.ObjectMetaData
     */
    private void handleOLEShape(HSLFObjectShape ole, int oleIndex) throws IOException {
        HSLFObjectData data = ole.getObjectData();
        String name = ole.getInstanceName();
        System.out.println("ole name: " + name);
        ObjectMetaData.Application lookup = ObjectMetaData.Application.lookup(ole.getProgId());
        switch (name == null ? "" : name) {
            case "Worksheet": {
                // TODO excel 区分 xls, xlsx
                if (lookup != null) {
                    if (lookup.equals(ObjectMetaData.Application.EXCEL_V8)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(".ppt 文件, 读取到 EXCEL_V8 OLE type");
                    } else if (lookup.equals(ObjectMetaData.Application.EXCEL_V12)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(".ppt 文件, 读取到 EXCEL_V12 OLE type");
                    } else {
                        LOG.error(String.format("读取 .ppt 文件时, 解析到未知的 excel OLE 类型: %s", lookup));
                    }
                }
                break;
            }
            case "Document": {
                // TODO word 区分 doc, docx
                if (lookup != null) {
                    if (lookup.equals(ObjectMetaData.Application.WORD_V8)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(".ppt 文件, 读取到 WORD_V8 OLE type");
                    } else if (lookup.equals(ObjectMetaData.Application.WORD_V12)) {
                        // TODO
                        if (LOG.isInfoEnabled())
                            LOG.info(".ppt 文件, 读取到 WORD_V12 OLE type");
                    } else {
                        LOG.error(String.format("读取 .ppt 文件时, 解析到未知的 word OLE 类型: %s", lookup));
                    }
                }
                break;
            }
            case "Visio": {
                // 作为图片处理
                handleVisio(ole, name, oleIndex);
                break;
            }
            case "PDF": {
                // TODO pdf 文件暂不处理
                break;
            }
            default: {
                // 未知的 OLE 类型
                LOG.error("处理 .ppt 文件时, 读取到未知类型的 OLE 文件");
            }
        }
    }

    /**
     * 处理 ppt 内嵌的 visio 文件, 直接作为图片处理
     * @param ole  HSLFObjectShape
     * @param name  ELO name
     */
    private void handleVisio(HSLFObjectShape ole, String name, int oleIndex) throws IOException {
        HSLFPictureData pictureData = ole.getPictureData();
        byte[] data = pictureData.getData();
        PictureData.PictureType pt = pictureData.getType();
        String extension = pt.extension;
        String mimeType = pt.contentType;
        // WMF 和 EMF 文件全部转换为 PNG 图片
        if (PictureData.PictureType.WMF.equals(pt)) {
            data = IOUtils.convertWMFToPNG(data);
            IOUtils.saveFile(data, String.format("C:\\%d-ole.png", oleIndex));
        } else if (PictureData.PictureType.EMF.equals(pt)) {
            data = IOUtils.convertEMFToPNG(data);
            IOUtils.saveFile(data, String.format("C:\\%d-ole.png", oleIndex));
        } else {
            IOUtils.saveFile(data, String.format("C:\\%d-ole%s", oleIndex, extension));
        }
    }
    
}
