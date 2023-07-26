package io.naivekyo.content;

import io.naivekyo.content.impl.ImageContent;
import io.naivekyo.content.impl.TableContent;
import io.naivekyo.content.impl.TextContent;
import io.naivekyo.support.function.ContentConverter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档内容 helper class, 包含一些属性和工具方法
 * @author NaiveKyo
 * @since 1.0
 */
public final class ContentHelper {
    
    private ContentHelper() {
    }

    // ================================== text content ============================

    /**
     * 空字符串
     */
    public static final String EMPTY_STR = "";

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
    private static final List<String> WORD_NO_VALID_TEXT = Arrays.asList("\r", "\t", "\n", "\u0007", "\u000B", "*");

    // ================================== image content ============================

    /**
     * 图片内容的 html 渲染字符串, 使用 .doc-image-box css 类标记
     */
    private static final String IMAGE_HTML_WRAPPER = "<div class=\"doc-image-box\"><img src=\"data:image/%s;base64,%s\"></img></div>";
    
    // ================================== pdf content ============================
    

    // ================================== static convenient methods ============================

    /**
     * 对 TextContent 内容进行转义, 去除潜在的危险脚本
     * @param text 文档文本内容
     * @return 转义后的文本
     */
    public static String escapeTextContent(TextContent text) {
        if (text == null)
            throw new NullPointerException("文本内容不能为 null");
        return escapeTextContent(text, null);
    }

    /**
     * 对 TextContent 内容进行转义, 去除潜在的危险脚本, 同时采用可能存在的转换器处理经过转义后的文本信息
     * @param text 文档文本内容
     * @param converter 转换器, 为 null 时不生效
     * @return 转义后的文本
     */
    public static String escapeTextContent(TextContent text, ContentConverter<DocContent, String> converter) {
        if (text == null)
            throw new NullPointerException("文本内容不能为 null");
        String content = text.getRawContent();
        content = safeHTMLString(content);
        content = WORD_TAB_SYMBOL_PATTERN.matcher(content).replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;");
       
        if (converter != null) {
            // TODO 为了保证内容的 immutable, 后续考虑不使用 setter 方法, 而是使用 clone 对象或者序列化机制适配转换函数
            text.setRawContent(content);
            content = converter.apply(text);
        }
        
        return content;
    }

    /**
     * 使用 p 标签修饰指定文本
     * @param text 目标文本数据
     * @return p 标签修饰的 html 段落
     */
    public static String convertTextToHTML(String text) {
        return String.format(TEXT_TO_HTML_WRAPPER, text);
    }

    /**
     * 对文本内容中可能包含的 html 标签进行转义
     * @param content 文本内容
     * @return 转义后的字符串
     */
    public static String safeHTMLString(String content) {
        Matcher matcher = HTML_LABEL_PATTERN.matcher(content);
        if (matcher.matches())
            content = matcher.replaceAll(SAFE_SCRIPT_STR);
        content = LEFT_ANGLE_BRACKET_PATTERN.matcher(content).replaceAll("&lt;");
        content = RIGHT_ANGLE_BRACKET_PATTERN.matcher(content).replaceAll("&gt;");
        return content;
    }

    /**
     * 使用 html 标签包装 ImageContent 数据
     * @param image 文档图片内容
     * @return 使用 img 标签修饰的 base64 形式的图片
     */
    public static String convertImageContentToHtml(ImageContent image) {
        return convertImageContentToHtml(image, null);
    }

    /**
     * 使用自定义的转换函数处理文档图片内容, 转换函数无效时返回图片的 base64 字符串
     * @param image 文档图片内容
     * @param converter 自定义转换函数, 为 null 时将图片数据转换为 base64 字符串, 并使用 html 标签修饰
     * @return  转换后的图片内容字符串
     */
    public static String convertImageContentToHtml(ImageContent image, ContentConverter<DocContent, String> converter) {
        if (converter == null)
            return String.format(IMAGE_HTML_WRAPPER, image.getFileType(), base64Encode(image.getRawData()));
        else 
            return converter.apply(image);
    }

    /**
     * 将 TableContent 内容输出为 html 格式
     * @param table 文档表格内容
     * @return  使用 table 标签渲染的表格内容
     */
    public static String convertTableDataToHtml(TableContent table) {
        return convertTableDataToHtml(table, null);
    }

    /**
     * 将 TableContent 内容输出为 html 格式, 并采用可能存在的转换器对每个表格项进行处理
     * @param table table 文档表格内容
     * @param converter 自定义转换函数, 为 null 不生效, 否则对每个表格项适配转换函数
     * @return 使用 table 标签渲染的表格内容
     */
    public static String convertTableDataToHtml(TableContent table, ContentConverter<DocContent, String> converter) {
        List<List<TextContent>> tableData = table.getRawContent();
        int row = table.getRowSize();
        int col = table.getColSize();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"doc-table-box\"><table>");
        for (int i = 0; i < row; i++) {
            sb.append("<tr>");
            List<TextContent> tRow = tableData.get(i);
            for (int j = 0; j < col; j++) {
                String text = EMPTY_STR;
                if (col > tRow.size()) {
                    // 如果当前行比最大列数要小, 剩余的部分填充空白
                    if (j > tRow.size() - 1)
                        text = EMPTY_STR;
                    else {
                        TextContent textContent = tRow.get(j);
                        if (converter == null)
                            text = escapeTextContent(textContent);
                        else 
                            text = escapeTextContent(textContent, converter);
                    }
                } else {
                    TextContent textContent = tRow.get(j);
                    if (converter == null)
                        text = textContent.getHTMLWrapContent();
                    else
                        text = textContent.getHTMLWrapContent(converter);
                }
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
     * 清除抽取文本中包含的特殊字符 {@link ContentHelper#WORD_SPECIAL_SYMBOL_PATTERN}
     * @param wordText 原始文本
     * @return 清洗后的文本
     */
    public static String cleanExtractedText(String wordText) {
        if (wordText == null)
            throw new NullPointerException("文本内容不能为 null");
        return WORD_SPECIAL_SYMBOL_PATTERN.matcher(wordText).replaceAll("");
    }

    /**
     * 判断文本是否包含有效的字符, 标准为: 不为  null 且含有至少一个非空字符
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
     * 校验从文档中提取的文本是否有效 <br/>
     * 要求文本不为 null, 且拥有至少一个非空字符, 字符串不能为特定的特殊字符 {@link ContentHelper#WORD_NO_VALID_TEXT}
     * @param content 文本内容
     * @return true 表示有效, 反之则是 false
     */
    public static boolean checkValidText(String content) {
        return content != null && !WORD_NO_VALID_TEXT.contains(content) && hasText(content);
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
     * 使用 Java ImageIO 根据图片数据获取图片的文件类型, 如 png, jpg 等等
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
    
}
