package io.naivekyo.extractor.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.extractor.AbstractContentExtractor;
import lombok.extern.slf4j.Slf4j;
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
 * @version 1.0
 * @since 2023/7/10 22:37
 */
@Slf4j
public class HWPFWordContentExtractor extends AbstractContentExtractor {
    
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
                        if (mimeType.equals("image/x-wmf")) {
                            // TODO x-wmf 格式的图片 Java ImageIO 没有提供对应的处理工具, 需要注册对应的 provider 才可以处理
                            continue;
                        }
                        String formatName = ContentHelper.getImageFileType(imgBytes);
                        if (formatName == null || "".equals(formatName))
                            log.error("word 类型: doc, 解析文件中的图片内容时, 未找到对应的 ImageReader, 图片 mime-type: {}", mimeType);
                        else
                            this.getContents().add(new ImageContent(imgBytes, mimeType, formatName));
                    } else {
                        // 处理文本
                        String runText = characterRun.text();
                        if (!ContentHelper.checkWordValidText(runText))
                            continue;
                        if (sb == null)
                            sb = new StringBuilder();
                        sb.append(runText);
                    }
                }
                if (sb != null) {
                    String text = sb.toString();
                    if (ContentHelper.hasText(text)) {
                        this.getContents().add(new TextContent(ContentHelper.cleanWordText(text)));
                    }
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
