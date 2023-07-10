package io.naivekyo.content;

import org.apache.poi.xwpf.usermodel.Document;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档内容 helper class, 包含一些属性和工具方法
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:25
 */
public final class ContentHelper {
    
    private ContentHelper() {
    }

    // ================================== text content ============================

    /**
     * 文本内容转换为 p 标签包含的 html 内容
     */
    private static final String TEXT_TO_HTML_WRAPPER = "<p>%s</p>";

    /**
     * 对文本内容中的 html 标签进行转义, 防止恶意代码
     */
    private static final String SAFE_SCRIPT_STR = "&lt;$1&gt;";

    /**
     * 匹配文本内容中 html 标签的模式
     */
    private static final Pattern HTML_LABEL_PATTERN = Pattern.compile("<(.*?)>");

    private static final Pattern LEFT_ANGLE_BRACKET_PATTERN = Pattern.compile("<");

    private static final Pattern RIGHT_ANGLE_BRACKET_PATTERN = Pattern.compile(">");

    // ================================== word content ============================

    /**
     * word 文本特殊符号匹配模式
     */
    private static final Pattern WORD_SPECIAL_SYMBOL_PATTERN = Pattern.compile("\r\n|[\r\n]|\u0007|\u000B");

    /**
     * word 制表符匹配模式
     */
    private static final Pattern WORD_TAB_SYMBOL_PATTERN = Pattern.compile("\t");

    /**
     * word 中抽取的无效文本
     */
    private static final List<String> WORD_NO_VALID_TEXT = Arrays.asList("\r", "\t", "\n", "\u0007", "\u000B");

    // ================================== image content ============================

    /**
     * 图片内容的 html 渲染字符串, 使用 .doc-image-box css 类标记
     */
    private static final String IMAGE_HTML_WRAPPER = "<div class=\"doc-image-box\"><img src=\"data:image/%s;base64,%s\"></img></div>";

    /**
     * poi-ooxml 中定义的 .docx 文件中嵌入的图片的类型 <br/>
     * 参考: {@link Document}
     */
    private static final Map<Integer, String> XWPF_PICTURE_TYPE;

    static {
        XWPF_PICTURE_TYPE = new HashMap<>();
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_EMF, "emf");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_WMF, "wmf");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_PICT, "pict");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_JPEG, "jpeg");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_PNG, "png");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_DIB, "dib");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_GIF, "gif");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_TIFF, "tiff");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_EPS, "eps");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_BMP, "bmp");
        XWPF_PICTURE_TYPE.put(Document.PICTURE_TYPE_WPG, "wpg");
    }

    // ================================== static convenient methods ============================

    /**
     * 使用 html 标签包装 {@link io.naivekyo.content.impl.TextContent} 内容
     * @param content 文本内容
     * @return html 内容
     */
    public static String convertTextToHTML(String content) {
        if (content == null)
            throw new RuntimeException("文本内容不能为 null");
        Matcher matcher = HTML_LABEL_PATTERN.matcher(content);
        if (matcher.matches()) {
            content = matcher.replaceAll(SAFE_SCRIPT_STR);
        }
        content = LEFT_ANGLE_BRACKET_PATTERN.matcher(content).replaceAll("&lt;");
        content = RIGHT_ANGLE_BRACKET_PATTERN.matcher(content).replaceAll("&gt;");
        content = WORD_TAB_SYMBOL_PATTERN.matcher(content).replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;");
        return String.format(TEXT_TO_HTML_WRAPPER, content);
    }

    /**
     * 使用 html 标签包装 {@link io.naivekyo.content.impl.ImageContent} 数据
     * @param bytes 图片字节数组
     * @param type 图片的类型, 全小写, 比如 png
     * @return html 内容
     */
    public static String convertImageToHtml(byte[] bytes, String type) {
        return String.format(IMAGE_HTML_WRAPPER, type, base64Encode(bytes));
    }

    /**
     * 将 {@link io.naivekyo.content.impl.TableContent} 内容输出为 html 格式
     * @param tableData 表格数据
     * @param row   最大行
     * @param col   最大列
     * @return  html 内容
     */
    public static String convertTableDataToHtml(List<List<String>> tableData, int row, int col) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"doc-table-box\"><table>");
        for (int i = 0; i < row; i++) {
            sb.append("<tr>");
            List<String> tRow = tableData.get(i);
            for (int j = 0; j < col; j++) {
                String text = null;
                if (col > tRow.size()) {
                    if (j > tRow.size() - 1)
                        text = "";
                    else
                        text = tRow.get(j);
                } else
                    text = tRow.get(j);
                sb.append("<td>");
                sb.append(text);
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }

    /**
     * 清除从 word 文件抽取的文本中包含的特殊字符
     * @param wordText 原始文本
     * @return 清洗后的文本
     */
    public static String cleanWordText(String wordText) {
        if (wordText == null)
            throw new RuntimeException("文本内容不能为 null");
        // String cleanText;
        // cleanText = WORD_SPECIAL_SYMBOL_PATTERN.matcher(wordText).replaceAll("");
        // return WORD_TAB_SYMBOL_PATTERN.matcher(cleanText).replaceAll("    ");
        return WORD_SPECIAL_SYMBOL_PATTERN.matcher(wordText).replaceAll("");
    }

    /**
     * 判断文本是否包含有效的字符, 标准为: 不为  null 且含有至少一个非空字符的字符
     * @param str 目标字符序列
     * @return true 表示含有有效文本, 反之 false
     */
    public static boolean hasText(CharSequence str) {
        if (str == null)
            return false;
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验从 word 中提取的文本是否有效
     * @param content 文本内容
     * @return true 表示有效, 反之则是 false
     */
    public static boolean checkWordValidText(String content) {
        return content != null && !WORD_NO_VALID_TEXT.contains(content);
    }

    /**
     * 对字节数组进行 base64 编码并转换为字符串, 使用 JDK 编码工具
     * @param bytes 目标字节数字
     * @return base64 字符串
     */
    public static String base64Encode(byte[] bytes) {
        byte[] encode = Base64.getEncoder().encode(bytes);
        return new String(encode, StandardCharsets.UTF_8);
    }

    /**
     * 根据图片数据获取图片的文件类型, 如 png, jpg 等等
     * @param imageBytes    图片字节数据
     * @return  文件类型, 返回 null 表示没找到匹配的图片处理器
     * @throws IOException IO 异常
     */
    public static String getImageFileType(byte[] imageBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
        ImageReader reader = null;
        while (imageReaders.hasNext()) {
            reader = imageReaders.next();
        }
        if (reader == null)
            return null;
        return reader.getFormatName();
    }

    /**
     * 获取 docx word 文件内嵌的图片类型
     * @param type {@link Document#PICTURE_TYPE_PNG} etc.
     * @return docx 文件中图片类型, 比如 png, jpeg, 没有则返回 null
     */
    public static String getXWPFPictureType(Integer type) {
        return XWPF_PICTURE_TYPE.get(type);
    }
}
