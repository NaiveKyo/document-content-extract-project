package io.naivekyo.support.text;

import io.naivekyo.constant.LanguageEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言相关的语句分割符
 */
public class LanguageEndpoints {
    
    private static final Map<String, String[]> ENDPOINT_MAP = new HashMap<>();
    
    static {
        loadDefaultEndpoint();
        loadCNEndpoint();
        loadENEndpoint();
    }

    private static void loadDefaultEndpoint() {
        ENDPOINT_MAP.put(LanguageEnum.UNKNOWN.getLang(), new String[] {
                "\n\n", "\n", System.getProperty("line.separator"), " "
        });
    }

    private static void loadCNEndpoint() {
        ENDPOINT_MAP.put(LanguageEnum.ZH_CN.getLang(), new String[]{
                "\n\n", "\n", System.getProperty("line.separator"), "。", "……", "？", "\\?", "\\.", " "
        });
    }
    
    private static void loadENEndpoint() {
        ENDPOINT_MAP.put(LanguageEnum.EN.getLang(), new String[]{
                "\n\n", "\n", System.getProperty("line.separator"), "\\.", "……", "\\?", " "
        });
    }

    /**
     * 获取默认分割字符集
     */
    public static String[] getDefault() {
        return ENDPOINT_MAP.get(LanguageEnum.UNKNOWN.getLang());
    }

    /**
     * 获取特定语种相关的分割字符串, 未匹配则返回 null
     * @param language 语言类型 {@link LanguageEnum}
     */
    public static String[] getLanguageSet(LanguageEnum language) {
        return ENDPOINT_MAP.get(language.getLang());
    }
    
}
