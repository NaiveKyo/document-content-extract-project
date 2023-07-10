package io.naivekyo.extractor;

import io.naivekyo.content.DocContent;

import java.util.List;

/**
 * 文档内容抽取接口
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:33
 */
public interface ContentExtractor {

    /**
     * 对文档进行数据抽取, 返回所有的内容
     * @return {@link io.naivekyo.content.DocContent}
     */
    List<DocContent> extract();
    
}
