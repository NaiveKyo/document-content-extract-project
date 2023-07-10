package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * .pdf 文件内容抽取器实现
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:41
 */
@Slf4j
public class PDFFileContentExtractor extends AbstractContentExtractor {

    public PDFFileContentExtractor(InputStream docByteStream) {
        super(docByteStream);
    }

    @Override
    protected void doExtract() {
        PDDocument pdfDocument = null;
        Exception markEx = null;
        try {
            pdfDocument = PDDocument.load(getDocByteStream());
            if (!pdfDocument.isEncrypted()) {
                int numberOfPages = pdfDocument.getNumberOfPages();
                System.out.println("总页数: " + numberOfPages);
                for (int i = 0; i < numberOfPages; i++) {
                    PDPage pdfPage = pdfDocument.getPage(i);
                    PDResources resources = pdfPage.getResources();
                    Iterable<COSName> xObjectNames = resources.getXObjectNames();
                    for (COSName name : xObjectNames) {
                        if (resources.isImageXObject(name)) {
                            COSInputStream inputStream = resources.getXObject(name).getStream().createInputStream();
                            
                        } else {
                            PDXObject xObject = resources.getXObject(name);
                            COSStream cosObject = xObject.getCOSObject();
                            String text = cosObject.toTextString();
                            System.out.println(text);
                        }

                    }
                }
            } else {
                log.error("无法读取被加密的 pdf 文件");
            }
        } catch (IOException e) {
            markEx = e;
        } finally {
            try {
                if (pdfDocument != null)
                    pdfDocument.close();
            } catch (IOException e) {
                markEx = e;
            }
        }
        if (markEx != null)
            throw new RuntimeException(markEx);
    }
}
