package com.hybrid9.pg.Lipanasi.annotationx;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests();
    int timeWindow();
    RateLimitType type() default RateLimitType.API;

    enum RateLimitType {
        API, USER, IP
    }
}
