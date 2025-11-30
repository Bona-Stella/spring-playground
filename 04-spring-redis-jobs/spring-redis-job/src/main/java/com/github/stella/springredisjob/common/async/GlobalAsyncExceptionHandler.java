package com.github.stella.springredisjob.common.async;

import com.github.stella.springredisjob.pubsub.NotifyPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @Async 메서드(void 반환 등)에서 발생한 처리되지 않은 예외를 중앙에서 수집/로깅합니다.
 * 필요 시 간단 알림을 notify 채널로 발행합니다.
 */
@Component
public class GlobalAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalAsyncExceptionHandler.class);
    private final NotifyPublisher notifyPublisher;

    public GlobalAsyncExceptionHandler(NotifyPublisher notifyPublisher) {
        this.notifyPublisher = notifyPublisher;
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        // 상세 로깅
        log.error("[ASYNC][UNCAUGHT] method={}#{}, params={}, error={}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.toString(), ex);

        // 가벼운 알림(노이즈 방지를 위해 메시지 요약)
        try {
            String summary = String.format("[ASYNC][UNCAUGHT] %s#%s: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), ex.getClass().getSimpleName());
            notifyPublisher.publish("notify", summary);
        } catch (Exception ignored) {
            // 알림 발행 중 오류는 무시
        }
    }
}
