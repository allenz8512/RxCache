package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ClassName;

/**
 * The type Utils.
 */
public class Utils {

    public static final ClassName RX_OBSERVABLE_TYPE_NAME = ClassName.get("rx", "Observable");

    public static final ClassName RX_SUBSCRIBER_TYPE_NAME = ClassName.get("rx", "Subscriber");

    public static final ClassName RX_FUNC1_TYPE_NAME = ClassName.get("rx.functions", "Func1");

    public static final ClassName RX_ACTION1_TYPE_NAME = ClassName.get("rx.functions", "Action1");

    public static final ClassName RX_CACHE_REPOSITORY_TYPE_NAME = ClassName.get("me.allenzjl.rxcache.library", "CacheRepository");

    public static final ClassName RX_CACHE_UTILS_TYPE_NAME = ClassName.get("me.allenzjl.rxcache.library", "CacheUtils");

    public static final ClassName RX_JSON_PROCESSER_TYPE_NAME = ClassName.get("me.allenzjl.rxcache.library", "JSONProcessor");

    public static final ClassName RX_JSON_BUILDER_TYPE_NAME =
            ClassName.get("me.allenzjl.rxcache.library", "JSONProcessor", "Builder");

    public static final ClassName STRING_TYPE_NAME = ClassName.get(String.class);

    public static final String CACHEABLE_CLASS_NAME_SUFFIX = "$$CacheableHolder";

    public static boolean isStringEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static <T> boolean isArrayEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static String getCacheRepositoryFieldName(String packageName, String superClassName) {
        return "m" + superClassName + "Cache";
    }

    public static String getExtraKeyHolderFieldName(String packageName, String className) {
        return "m" + className + "Field";
    }
}
