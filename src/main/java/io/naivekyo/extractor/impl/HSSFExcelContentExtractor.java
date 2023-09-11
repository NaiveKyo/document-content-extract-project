package io.naivekyo.extractor.impl;

import io.naivekyo.extractor.AbstractContentExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>.xls 后缀的 Excel 文件内容抽取器实现</p>
 * @author NaiveKyo
 * @since 1.0
 */
public class HSSFExcelContentExtractor extends AbstractContentExtractor {
	
	private static final Log LOG = LogFactory.getLog(HSSFExcelContentExtractor.class);

	public HSSFExcelContentExtractor(InputStream docByteStream) {
		super(docByteStream);
	}

	@Override
	protected void doExtract() {
		HSSFWorkbook hssfWorkbook = null;
		Exception markEx = null;
		try {
			hssfWorkbook = new HSSFWorkbook(getDocByteStream());
			int sheets = hssfWorkbook.getNumberOfSheets();
			if (sheets ==  0)
				throw new IllegalArgumentException("没有检测到有效的 sheet 表格, 不规范的 Excel 文件");
			for (int i = 0; i < sheets; i++) {
				HSSFSheet sheet = hssfWorkbook.getSheetAt(i);
				for (Row row : sheet) {
					for (Cell cell : row) {
						new CellReference(row.getRowNum(), cell.getColumnIndex());
						
					}
				}
			}
		} catch (Exception e) {
			markEx = e;
		} finally {
			try {
				if (hssfWorkbook != null)
					hssfWorkbook.close();
			} catch (IOException e) {
				markEx = e;
			}
		}
		if (markEx != null)
			throw new RuntimeException(markEx);
	}
}
