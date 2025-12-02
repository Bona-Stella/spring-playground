package com.github.stella.springredisjob.external.mock;

import com.github.stella.springredisjob.common.error.CustomException;
import com.github.stella.springredisjob.common.error.ErrorCode;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherRequest;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ExternalService {
    private static final Logger log = LoggerFactory.getLogger(ExternalService.class);

    private final FakeExternalApiClient client;

    public ExternalService(FakeExternalApiClient client) {
        this.client = client;
    }

    // 캐시 적용된 동기 호출
    @Cacheable(value = "ext:weather", key = "#city")
    public ExternalWeatherResponse getWeatherCached(String city) {
        return fetchWithPolicy(city);
    }

    // 정책(Resilience4j: 재시도/서킷) 적용, 캐시 미사용
    @CircuitBreaker(name = "externalMock", fallbackMethod = "fallbackExternal")
    @Retry(name = "externalMock")
    public ExternalWeatherResponse fetchWithPolicy(String city) {
        try {
            return client.fetchWeather(new ExternalWeatherRequest(city));
        } catch (Exception e) {
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }

    // Resilience4j fallback — 동일 시그니처 + Throwable 마지막 파라미터
    @SuppressWarnings("unused")
    private ExternalWeatherResponse fallbackExternal(String city, Throwable e) {
        log.debug("[External][Fallback] city={} cause={}", city, e.toString());
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // RAW 호출 (정책/캐시 모두 우회)
    public ExternalWeatherResponse fetchRaw(String city) throws Exception {
        return client.fetchWeather(new ExternalWeatherRequest(city));
    }

    // 비동기 호출 (가상 스레드 기반 @Async)
    @Async
    public void fetchAsync(String city) {
        try {
            ExternalWeatherResponse res = getWeatherCached(city);
            log.info("[External][ASYNC] fetched city={} temp={} desc={} at {}", res.city(), res.temperatureC(), res.description(), res.fetchedAt());
        } catch (Exception e) {
            log.warn("[External][ASYNC] failed for city={}: {}", city, e.getMessage());
        }
    }

    // CompletableFuture 기반 비동기 호출 (논블로킹 응답용)
    @Async
    public CompletableFuture<ExternalWeatherResponse> fetchAsyncFuture(String city) {
        try {
            ExternalWeatherResponse res = getWeatherCached(city);
            return CompletableFuture.completedFuture(res);
        } catch (Exception e) {
            CompletableFuture<ExternalWeatherResponse> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
    }
}
