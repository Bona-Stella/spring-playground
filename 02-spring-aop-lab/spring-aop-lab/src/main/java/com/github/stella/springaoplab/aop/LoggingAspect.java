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

    @Pointcut("@annotation(com.github.stella.springaoplab.aop.LogExecutionTime)")
    public void logExecutionTimePointcut() {}

    @Around("logExecutionTimePointcut()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
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
