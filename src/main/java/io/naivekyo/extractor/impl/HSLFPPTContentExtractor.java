package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hslf.record.EscherTextboxWrapper;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.Slide;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.Resources;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * .ppt 后缀的 ppt 文件内容抽取器实现
 */
public class HSLFPPTContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(HSLFPPTContentExtractor.class);
    
    public HSLFPPTContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        SlideShow spSlideShow = null;
        Exception mark = null;
        try {
            spSlideShow = SlideShowFactory.create(getDocByteStream());
            
            if (spSlideShow instanceof HSLFSlideShow) {
                HSLFSlideShow hslfSlideShow = (HSLFSlideShow) spSlideShow;
                
                List<HSLFPictureData> pictureDataList = hslfSlideShow.getPictureData();
                
                List<HSLFSlide> slides = hslfSlideShow.getSlides();
                
                for (int i = 0; i < slides.size(); i++) {
                    HSLFSlide currentSlide = slides.get(i);
                    Slide slideRecord = currentSlide.getSlideRecord();
                    
                    List<List<HSLFTextParagraph>> textParagraphs = currentSlide.getTextParagraphs();
                    for (List<HSLFTextParagraph> paragraph : textParagraphs) {
                        for (HSLFTextParagraph p : paragraph) {
                            List<HSLFTextRun> textRuns = p.getTextRuns();
                            for (HSLFTextRun run : textRuns) {
                                String rawText = run.getRawText();
                                System.out.println(rawText);
                            }
                        }
                    }
                }
            } else {
                LOG.error(String.format("无效的 ppt 类型, 当前抽取器处理 HSLF 类型的 ppt, 待处理文档为 %s", spSlideShow.getClass().getName()));
            }
            
            // 抽取所有文本
            SlideShowExtractor extractor = new SlideShowExtractor(spSlideShow);
            extractor.setMasterByDefault(true);
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
    
}
