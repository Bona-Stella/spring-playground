package com.github.stella.springredisjob.external.mock;

import com.github.stella.springredisjob.common.api.ApiResponse;
import com.github.stella.springredisjob.common.error.CustomException;
import com.github.stella.springredisjob.common.error.ErrorCode;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherRequest;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ExternalService {
    private static final Logger log = LoggerFactory.getLogger(ExternalService.class);

    private final FakeExternalApiClient client;
    private final ExternalRetryPolicy retryPolicy;
    private final SimpleCircuitBreaker circuitBreaker;

    public ExternalService(FakeExternalApiClient client, ExternalRetryPolicy retryPolicy, SimpleCircuitBreaker circuitBreaker) {
        this.client = client;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
    }

    // 캐시 적용된 동기 호출
    @Cacheable(value = "ext:weather", key = "#city")
    public ExternalWeatherResponse getWeatherCached(String city) {
        return fetchWithPolicy(city);
    }

    // 정책(재시도/서킷) 적용하지만 캐시 미사용
    public ExternalWeatherResponse fetchWithPolicy(String city) {
        if (!circuitBreaker.allowRequest()) {
            log.debug("[External] circuit OPEN: request blocked for city={}", city);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        try {
            ExternalWeatherResponse res = retryPolicy.executeWithRetry(() -> {
                try {
                    return client.fetchWeather(new ExternalWeatherRequest(city));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            circuitBreaker.onSuccess();
            return res;
        } catch (Exception e) {
            circuitBreaker.onFailure();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
}
