package com.github.stella.springredisjob.external.mock;

import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherRequest;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class FakeExternalApiClient {
    private static final Logger log = LoggerFactory.getLogger(FakeExternalApiClient.class);
    private final Random random = new Random();

    @Value("${external.mock.minDelayMs:100}")
    private int minDelayMs;
    @Value("${external.mock.maxDelayMs:800}")
    private int maxDelayMs;
    @Value("${external.mock.failureRate:0.2}")
    private double failureRate;
    @Value("${external.mock.timeoutMs:700}")
    private int timeoutMs;

    public ExternalWeatherResponse fetchWeather(ExternalWeatherRequest req) throws Exception {
        int delay = minDelayMs + random.nextInt(Math.max(1, (maxDelayMs - minDelayMs + 1)));
        boolean shouldFail = random.nextDouble() < failureRate;

        // Simulate processing delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

        if (delay > timeoutMs) {
            log.debug("[FakeExternal] timeout simulated (delay={}ms > timeout={}ms)", delay, timeoutMs);
            throw new RuntimeException("EXTERNAL_TIMEOUT");
        }
        if (shouldFail) {
            log.debug("[FakeExternal] failure simulated");
            throw new RuntimeException("EXTERNAL_FAILURE");
        }

        double temp = -5 + random.nextDouble() * 40; // -5 ~ 35C
        String desc = temp > 28 ? "hot" : temp < 5 ? "cold" : "mild";
        return new ExternalWeatherResponse(
                req.city(),
                Math.round(temp * 10.0) / 10.0,
                desc,
                java.time.ZonedDateTime.now().toString()
        );
    }
}
