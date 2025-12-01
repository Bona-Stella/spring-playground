package com.github.stella.springredisjob.common.lock.annotation;

import java.lang.annotation.*;

/**
 * AOP 기반 분산 락 획득을 위한 애노테이션.
 * key는 SpEL을 지원합니다. 예) "'job:rebuild-cache'", "'post:' + #id" 등
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLockable {
    /**
     * SpEL 기반 키 표현식 (필수)
     */
    String key();

    /**
     * 락 키 프리픽스. 기본값은 전역 설정(app.lock.prefix), 명시 시 우선
     */
    String prefix() default ""; // 빈 값이면 properties 사용

    /**
     * 락 보유 시간(초). -1이면 전역 설정(app.lock.leaseSeconds) 사용
     */
    long ttlSeconds() default -1;

    /**
     * 락 대기 시간(초). -1이면 전역 설정(app.lock.waitSeconds) 사용
     */
    long waitSeconds() default -1;

    /**
     * 락 획득 실패 시 동작 정책
     */
    OnFail onFail() default OnFail.FAIL_EXCEPTION;

    enum OnFail {
        FAIL_EXCEPTION,
        SKIP
    }
}
