package com.github.stella.springredisjob.common.lock.aop;

import com.github.stella.springredisjob.common.error.CustomException;
import com.github.stella.springredisjob.common.error.ErrorCode;
import com.github.stella.springredisjob.common.lock.DistributedLock;
import com.github.stella.springredisjob.common.lock.LockProperties;
import com.github.stella.springredisjob.common.lock.annotation.DistributedLockable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Order(0) // 트랜잭션 시작 전에 잠금 시도
public class DistributedLockAspect {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    private final DistributedLock distributedLock;
    private final LockProperties lockProperties;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public DistributedLockAspect(DistributedLock distributedLock, LockProperties lockProperties) {
        this.distributedLock = distributedLock;
        this.lockProperties = lockProperties;
    }

    @Around("@annotation(com.github.stella.springredisjob.common.lock.annotation.DistributedLockable)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        DistributedLockable ann = method.getAnnotation(DistributedLockable.class);

        String evaluatedKey = evaluateKey(ann.key(), method, pjp.getArgs());
        String prefix = ann.prefix().isEmpty() ? lockProperties.getPrefix() : ann.prefix();
        String finalKey = prefix + evaluatedKey;

        long ttlSec = ann.ttlSeconds() > 0 ? ann.ttlSeconds() : lockProperties.getLeaseSeconds();
        Long waitSec = ann.waitSeconds() >= 0 ? ann.waitSeconds() : lockProperties.getWaitSeconds();

        boolean acquired = false;
        try {
            acquired = distributedLock.lock(finalKey, Duration.ofSeconds(Math.max(1, ttlSec)), waitSec);
            if (!acquired) {
                if (ann.onFail() == DistributedLockable.OnFail.SKIP) {
                    // 메서드가 void면 그냥 스킵
                    if (void.class.equals(method.getReturnType())) {
                        log.debug("[LOCK][SKIP] key={} method={}.{}", finalKey,
                                method.getDeclaringClass().getSimpleName(), method.getName());
                        return null;
                    }
                }
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            log.debug("[LOCK][ACQUIRED] key={} method={}.{}", finalKey,
                    method.getDeclaringClass().getSimpleName(), method.getName());
            return pjp.proceed();
        } finally {
            if (acquired) {
                try {
                    distributedLock.unlock(finalKey);
                    log.debug("[LOCK][RELEASED] key={}", finalKey);
                } catch (Exception e) {
                    log.warn("[LOCK][RELEASE_FAIL] key={} err={}", finalKey, e.getMessage());
                }
            }
        }
    }

    private String evaluateKey(String spel, Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        Expression expression = parser.parseExpression(spel);
        Object value = expression.getValue(context);
        return String.valueOf(value);
    }
}
