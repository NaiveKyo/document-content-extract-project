package io.naivekyo.content;

import io.naivekyo.support.function.ContentConverter;

/**
 * <p>文档内容接口</p>
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:22
 */
public interface DocContent {

    /**
     * 获取原始内容
     * @return 内容
     */
    String getContent();

    /**
     * 使用 HTML 标签对文档内容进行修饰
     * @return 使用 html 标签修饰的内容
     */
    String getHTMLWrapContent();

    /**
     * 使用 HTML 标签对文档内容进行修饰, 并采用可能存在的转换函数
     * @param converter 转换函数, null 则使用 {@link #getHTMLWrapContent()} 策略
     * @return 使用 html 标签修饰的内容
     */
    String getHTMLWrapContent(ContentConverter<DocContent, String> converter);

    /**
     * 返回当前文档内容的类型
     * @return 内容的类型
     */
    ContentType getType();

    /**
     * 使用自定义的转换器对文档内容进行加工, 最终转换为字符串
     * @param converter 转换器, 为 null 时采用 {@link #getHTMLWrapContent()} 策略
     * @return 加工后的文本
     */
    default String getWrapContent(ContentConverter<DocContent, String> converter) {
        if (converter == null)
            return getHTMLWrapContent();
        return converter.apply(this);
    }
    
}
