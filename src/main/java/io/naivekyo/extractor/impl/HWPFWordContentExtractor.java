package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import io.naivekyo.support.IOUtils;
import io.naivekyo.support.word.ImageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.IOException;
import java.io.InputStream;

/**
 * .doc 后缀的 Word 文件内容抽取器实现
 * @author NaiveKyo
 * @since 1.0
 */
public class HWPFWordContentExtractor extends AbstractContentExtractor {

    private static final Log LOG = LogFactory.getLog(HWPFWordContentExtractor.class);
    
    public HWPFWordContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        HWPFDocument hwpfDocument = null;
        Exception markEx = null;
        try {
            hwpfDocument = new HWPFDocument(this.getDocByteStream());
            Range range = hwpfDocument.getRange();
            PicturesTable picturesTable = hwpfDocument.getPicturesTable();
            int numParagraphs = range.numParagraphs();
            // 处理所有的段落
            for (int i = 0; i < numParagraphs; i++) {
                Paragraph paragraph = range.getParagraph(i);
                int pRuns = paragraph.numCharacterRuns();
                StringBuilder sb = null;
                for (int r = 0; r < pRuns; r++) {
                    CharacterRun characterRun = paragraph.getCharacterRun(r);
                    if (picturesTable.hasPicture(characterRun)) {
                        // 处理 .doc 文件中包含的图片
                        Picture picture = picturesTable.extractPicture(characterRun, true);
                        byte[] imgBytes = picture.getContent();
                        String mimeType = picture.getMimeType();
                        ImageType imageType = ImageType.lookupByMimeType(mimeType);
                        if (ImageType.UNKNOWN.equals(imageType)) {
                            LOG.error(String.format("处理 .doc 文件时发现未知的图片类型, 图片 mime-type: %s", mimeType));
                            continue;
                        } else if (ImageType.WMF.equals(imageType)) {
                            imgBytes = IOUtils.convertWMFToPNG(imgBytes);
                            mimeType = ImageType.PNG.getMimeType();
                        } else if (ImageType.EMF.equals(imageType)) {
                            imgBytes = IOUtils.convertEMFToPNG(imgBytes);
                            mimeType = ImageType.PNG.getMimeType();
                        }
                        this.getContents().add(new ImageContent(imgBytes, mimeType, imageType.getName()));
                    } else {
                        // 处理文本
                        String runText = characterRun.text();
                        if (!ContentHelper.checkValidText(runText))
                            continue;
                        if (sb == null)
                            sb = new StringBuilder();
                        sb.append(runText);
                    }
                }
                if (sb != null) {
                    this.getContents().add(new TextContent(ContentHelper.cleanExtractedText(sb.toString())));
                }
            }
        } catch (IOException e) {
            markEx = e;
        } finally {
            try {
                if (hwpfDocument != null)
                    hwpfDocument.close();
            } catch (IOException e) {
                markEx = e;
            }
        }
        if (markEx != null)
            throw new RuntimeException(markEx);
    }
}
