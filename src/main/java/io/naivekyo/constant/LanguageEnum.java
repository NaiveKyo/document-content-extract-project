package io.naivekyo.constant;

/**
 * 语言枚举
 */
public enum LanguageEnum {

    /**
     * 未知语种
     */
    UNKNOWN("unknown"),
    
    /**
     * 中文
     */
    ZH_CN("zh_cn"),

    /**
     * 英文
     */
    EN("en");

    private final String lang;

    LanguageEnum(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }
    
}
