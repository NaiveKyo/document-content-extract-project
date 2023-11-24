package io.naivekyo.util;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Apache Tika 工具类
 */
public final class TikaFacadeUtil {
	
	// ======================= 几种常用文件的 media type =======================
	
	private static final String PDF_MEDIA_TYPE = "application/pdf";
	
	private static final String WORD_OLE2_DOC_MEDIA_TYPE = "application/msword";
	
	private static final String WORD_OOXML_DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	
	private static final String OOXML_MEDIA_TYPE = "application/x-tika-ooxml";
	
	private static final String TXT_MEDIA_TYPE = "text/plain";

	private TikaFacadeUtil() {
	}
	
	private static class TikaSingleton {
		// 根据需要构造 TikaConfig 对象, 目前使用默认配置
		private static final Tika INSTANCE = new Tika();
	}

	/**
	 * 单例
	 */
	private static Tika getTikaInstance() {
		return TikaSingleton.INSTANCE;
	}

	/**
	 * 获取文件的 media 类型, 传入的 stream 应当支持 {@link InputStream#markSupported() mark feature}, 以便后续继续使用。<br/>
	 * 本方法只会读取 stream 中特定的几个字节, 读取后恢复原位置, 方法不负责关闭 stream
	 * @param is 文件流
	 * @return 文档的 media type
	 * @throws IOException IO 相关异常
	 */
	public static String detectFileMediaType(InputStream is) throws IOException {
		if (!is.markSupported()) {
			throw new IOException("file content stream must support mark feature");
		}
		return getTikaInstance().detect(TikaInputStream.get(is));
	}

	/**
	 * 判断文档是否是 pdf 类型文件
	 * @param is 文档输入流, 必须支持 {@link InputStream#markSupported() mark feature}
	 * @return true or false
	 * @throws IOException IO 异常
	 */
	public static boolean isPdf(InputStream is) throws IOException {
		String type = detectFileMediaType(is);
		return PDF_MEDIA_TYPE.equals(type);
	}
	
	public static boolean isPdf(String mediaType) {
		return PDF_MEDIA_TYPE.equals(mediaType);
	}

	/**
	 * 判断文档是否是 doc 类型文件
	 * @param is is 文档输入流, 必须支持 {@link InputStream#markSupported() mark feature}
	 * @return true or false
	 * @throws IOException IO 异常
	 */
	public static boolean isOLE2Word(InputStream is) throws IOException {
		String type = detectFileMediaType(is);
		return WORD_OLE2_DOC_MEDIA_TYPE.equals(type);
	}

	public static boolean isOLE2Word(String mediaType) {
		return WORD_OLE2_DOC_MEDIA_TYPE.equals(mediaType);
	}
	
	/**
	 * 判断文档是否是 docx 类型文件
	 * @param is is 文档输入流, 必须支持 {@link InputStream#markSupported() mark feature}
	 * @return true or false
	 * @throws IOException IO 异常
	 */
	public static boolean isOOXMLWord(InputStream is) throws IOException {
		String type = detectFileMediaType(is);
		return WORD_OOXML_DOCX_MEDIA_TYPE.equals(type);
	}

	public static boolean isOOXMLWord(String mediaType) {
		return WORD_OOXML_DOCX_MEDIA_TYPE.equals(mediaType) || OOXML_MEDIA_TYPE.equals(mediaType);
	}

	/**
	 * 判断文档是否是 txt 类型文件
	 * @param is is 文档输入流, 必须支持 {@link InputStream#markSupported() mark feature}
	 * @return true or false
	 * @throws IOException IO 异常
	 */
	public static boolean isTxt(InputStream is) throws IOException {
		String type = detectFileMediaType(is);
		return TXT_MEDIA_TYPE.equals(type);
	}
	
}
