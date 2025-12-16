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

@Aspect // 1. "이 클래스는 부가 기능(Aspect)을 담당해"라고 선언
@Component // 스프링 빈으로 등록
public class DistributedLockAspect {

    // RedissonClient: Redis와 통신하여 락을 걸고 푸는 도구
    private final RedissonClient redissonClient;

    // ExpressionParser: SpEL 표현식(문자열로 된 변수명 등)을 해석하는 파서
    // 예: "#orderDto.id" 라는 문자열을 실제 객체의 id 값으로 바꿔줍니다.
    private final ExpressionParser parser = new SpelExpressionParser();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    // 2. @Around: 타겟 메서드 실행 "전"과 "후"를 모두 제어합니다.
    // 조건: @DistributedLock 어노테이션이 붙은 메서드만 가로챕니다.
    @Around("@annotation(com.github.stella.springmsamq.common.lock.DistributedLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        // 3. 현재 실행되려는 메서드의 정보(이름, 파라미터 등)를 가져옵니다.
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 메서드 위에 붙어있는 @DistributedLock 어노테이션 자체를 가져옵니다. (설정값을 읽기 위해)
        DistributedLock ann = method.getAnnotation(DistributedLock.class);

        // 4. 락을 걸 "키(Key)" 이름을 만듭니다.
        // 어노테이션에 적힌 표현식(예: "'product:' + #id")을 가져옴
        String keyExpr = ann.key();

        // 표현식을 실제 값으로 변환 (예: "product:100")
        String key = evaluateKey(keyExpr, signature.getParameterNames(), pjp.getArgs());

        // 5. Redisson에서 락 객체를 가져옵니다. (아직 잠근 건 아님)
        RLock lock = redissonClient.getLock(key);

        boolean locked = false;
        try {
            // 6. 락 획득 시도 (Try Lock)
            // waitMs: 락을 얻을 때까지 기다릴 시간 (이 시간 넘으면 포기)
            // leaseMs: 락을 얻은 후, 작업을 마칠 때까지 이 시간이 지나면 강제로 락 해제 (데드락 방지)
            locked = lock.tryLock(ann.waitMs(), ann.leaseMs(), TimeUnit.MILLISECONDS);

            if (!locked) {
                // 락을 못 얻었으면(누군가 이미 작업 중이면) 예외 발생
                throw new IllegalStateException("Failed to acquire lock: " + key);
            }

            // 7. 락 획득 성공! -> 원래 실행하려던 비즈니스 메서드를 실행합니다.
            // pjp.proceed()가 호출되는 순간 Service 코드가 실행됩니다.
            return pjp.proceed();

        } finally {
            // 8. 뒷정리 (매우 중요)
            // 락을 획득했고(locked), 현재 쓰레드가 락을 잡고 있다면(isHeldByCurrentThread)
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock(); // 락을 해제합니다.
            }
        }
    }

    // SpEL 파싱 헬퍼 메서드
    // 메서드 파라미터 이름과 값을 매핑해서 표현식을 해석합니다.
    private String evaluateKey(String expr, String[] paramNames, Object[] args) {
        // 문맥(Context) 준비: 변수들을 담을 그릇
        StandardEvaluationContext ctx = new StandardEvaluationContext();

        // 파라미터 이름과 실제 값을 매칭해서 문맥에 넣습니다.
        // 예: 파라미터명 "id" -> 값 100
        for (int i = 0; i < paramNames.length; i++) {
            ctx.setVariable(paramNames[i], args[i]);
        }

        // 파서가 문맥을 보고 표현식을 실제 문자열 값으로 바꿉니다.
        Expression e = parser.parseExpression(expr);
        Object val = e.getValue(ctx);
        return String.valueOf(val);
    }
}
