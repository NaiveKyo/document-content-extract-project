package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>.xlsx 后缀的 Excel 文件内容抽取器实现</p>
 * <p>针对数据量非常大的 Excel 文件, 为了降低内存使用、提高处理速度, 可以考虑以下方式: </p>
 * <ul>
 *     <li>1. 使用 SAX 的方式去读取 see https://roytuts.com/how-to-read-large-excel-file-using-apache-poi/</li>
 *     <li>2. 使用 poi 内置的 stream api</li>
 *     <li>3. 使用其他 Excel 相关的工具包</li>
 * </ul>
 * @author NaiveKyo
 * @since 1.0
 */
public class XSSFExcelContentExtractor extends AbstractContentExtractor {
	
	private static final Log LOG = LogFactory.getLog(XSSFExcelContentExtractor.class);
	
	public XSSFExcelContentExtractor(InputStream docByteStream) {
		super(docByteStream);
	}

	@Override
	protected void doExtract() {
		XSSFWorkbook xssfWorkbook = null;
		Exception markEx = null;
		try {
			xssfWorkbook = new XSSFWorkbook(getDocByteStream());
			
		} catch (Exception e) {
			markEx = e;
		} finally {
			try {
				if (xssfWorkbook != null)
					xssfWorkbook.close();
			} catch (IOException e) {
				markEx = e;
			}
		}
		if (markEx != null)
			throw new RuntimeException(markEx);
	}
}
