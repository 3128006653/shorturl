package com.demo.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String key() default "";
    int limit() default 10;
    String algorithm() default "sliding";
}