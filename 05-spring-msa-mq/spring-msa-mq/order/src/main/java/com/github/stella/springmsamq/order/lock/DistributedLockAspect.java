package com.github.stella.springmsamq.order.lock;

import com.github.stella.springmsamq.common.lock.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(com.github.stella.springmsamq.common.lock.DistributedLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        DistributedLock ann = method.getAnnotation(DistributedLock.class);

        String keyExpr = ann.key();
        String key = evaluateKey(keyExpr, signature.getParameterNames(), pjp.getArgs());

        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(ann.waitMs(), ann.leaseMs(), TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new IllegalStateException("Failed to acquire lock: " + key);
            }
            return pjp.proceed();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String evaluateKey(String expr, String[] paramNames, Object[] args) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            ctx.setVariable(paramNames[i], args[i]);
        }
        Expression e = parser.parseExpression(expr);
        Object val = e.getValue(ctx);
        return String.valueOf(val);
    }
}
