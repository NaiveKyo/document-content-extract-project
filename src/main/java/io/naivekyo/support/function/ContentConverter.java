package io.naivekyo.support.function;

import io.naivekyo.content.DocContent;

/**
 * <p>函数式接口: 文档内容的转换函数</p>
 * <p>该函数适用于将文档内容转换为字符序列</p>
 * @author NaiveKyo
 * @since 1.0
 */
@FunctionalInterface
public interface ContentConverter<D extends DocContent, C extends CharSequence> {
    
    C apply(D content);
    
}
