package io.naivekyo.content;

/**
 * 文档内容接口 <br/>
 * TODO: 不同文档中会含有各类数据, 比如纯文本、图片(常规图片和文档中使用的特殊图片)、表格、附件 etc.
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:22
 */
public interface DocContent {

    /**
     * 获取原始内容 (文本/图片base64编码字符串)
     * @return 内容
     */
    String getContent();

    /**
     * 获取包装后的内容（文本/图片）
     * @return 使用 html 标签修饰的内容
     */
    String getHTMLWrapContent();

    /**
     * 返回当前内容的类型
     * @return 内容的类型
     */
    ContentType getType();
    
}
