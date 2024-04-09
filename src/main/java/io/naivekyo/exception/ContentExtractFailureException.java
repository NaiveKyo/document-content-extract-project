package io.naivekyo.exception;

import java.io.IOException;

/**
 * 抽取文档内容过程中出现的异常
 */
public class ContentExtractFailureException extends IOException {

    private static final long serialVersionUID = 4468558990902832803L;

    public ContentExtractFailureException() {
        super();
    }

    public ContentExtractFailureException(String message) {
        super(message);
    }

    public ContentExtractFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentExtractFailureException(Throwable cause) {
        super(cause);
    }
    
}
