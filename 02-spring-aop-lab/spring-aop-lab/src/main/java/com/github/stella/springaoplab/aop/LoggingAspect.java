package com.github.stella.springaoplab.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // 범위를 Annotation으로 선언 후 함수(별명)로 명명
    @Pointcut("@annotation(com.github.stella.springaoplab.aop.LogExecutionTime)")
    public void logExecutionTimePointcut() {}

    // 범위 설정된 함수(별명)을 사용해서 AOP 적용할 함수 적용
    @Around("logExecutionTimePointcut()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            // Function Name    / Return Type       / Description
            // getArgs()        / Object[] (배열)   / 메소드에 전달된 매개변수(인자) 목록을 가져옵니다.
            // getSignature()   / Signature         / 실행되는 메소드의 이름, 리턴 타입 등 시그니처 정보를 담고 있습니다.
            // getTarget()      / Object            / 실제 비즈니스 로직을 담고 있는 대상 객체(Target Object)를 반환합니다.
            // getThis()        / Object            / AOP가 적용된 프록시(Proxy) 객체 자체를 반환합니다.
            // proceed()        / Object            / 타겟 메소드를 원래대로 실행합니다. 반환값은 비즈니스 로직의 실행 결과입니다.
            log.info("[AOP] Start: {} args={}", pjp.getSignature(), pjp.getArgs());
            Object result = pjp.proceed();
            long end = System.currentTimeMillis();
            log.info("[AOP] End: {} elapsed={}ms", pjp.getSignature(), (end - start));
            return result;
        } catch (Throwable t) {
            long end = System.currentTimeMillis();
            log.info("[AOP] Exception in {} after {}ms: {}", pjp.getSignature(), (end - start), t.toString());
            throw t;
        }
    }
}
