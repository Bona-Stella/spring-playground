package com.github.stella.springmsamq.common.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /** SpEL expression to build lock key, e.g., "'order:product:' + #productId" */
    String key();

    /** max time to wait for lock (ms) */
    long waitMs() default 3000L;

    /** lease time before auto-unlock (ms) */
    long leaseMs() default 10000L;
}
