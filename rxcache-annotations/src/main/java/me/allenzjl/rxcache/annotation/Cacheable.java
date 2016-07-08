package me.allenzjl.rxcache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Allen on 2016/6/30.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Cacheable {

    int STRATEGY_READ_CACHE_ONLY_IF_EXIST = 0;

    int STRATEGY_LOAD_SOURCE_AFTER_READ_CACHE = 1;

    String value() default "";

    String subType() default "";

    long expired() default 0;

    int strategy() default STRATEGY_READ_CACHE_ONLY_IF_EXIST;

    String[] extraKeys() default {};
}
