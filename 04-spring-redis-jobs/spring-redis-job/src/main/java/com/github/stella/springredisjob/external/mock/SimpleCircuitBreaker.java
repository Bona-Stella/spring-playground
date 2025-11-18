package com.github.stella.springredisjob.external.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 아주 단순한 서킷브레이커 구현 (Closed → Open → Half-Open)
 */
@Component
public class SimpleCircuitBreaker {
    private static final Logger log = LoggerFactory.getLogger(SimpleCircuitBreaker.class);

    public enum State { CLOSED, OPEN, HALF_OPEN }

    @Value("${external.mock.cb.failureThreshold:5}")
    private int failureThreshold;

    @Value("${external.mock.cb.openWindowMs:10000}")
    private long openWindowMs;

    @Value("${external.mock.cb.halfOpenMaxCalls:2}")
    private int halfOpenMaxCalls;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openSince = 0L;
    private int halfOpenTrialCalls = 0;

    public synchronized boolean allowRequest() {
        switch (state) {
            case CLOSED -> { return true; }
            case OPEN -> {
                long now = System.currentTimeMillis();
                if (now - openSince >= openWindowMs) {
                    state = State.HALF_OPEN;
                    halfOpenTrialCalls = 0;
                    log.debug("[CB] transition OPEN → HALF_OPEN");
                    return true; // allow a trial call
                }
                return false;
            }
            case HALF_OPEN -> {
                if (halfOpenTrialCalls < Math.max(1, halfOpenMaxCalls)) {
                    halfOpenTrialCalls++;
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        if (state != State.CLOSED) {
            state = State.CLOSED;
            log.debug("[CB] transition {} → CLOSED", state);
        }
    }

    public synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN) {
            // half-open에서 실패 → 즉시 open
            openCircuit();
            return;
        }
        if (state == State.CLOSED && consecutiveFailures >= Math.max(1, failureThreshold)) {
            openCircuit();
        }
    }

    private void openCircuit() {
        state = State.OPEN;
        openSince = System.currentTimeMillis();
        log.debug("[CB] transition → OPEN (failures={})", consecutiveFailures);
    }

    public synchronized State getState() { return state; }
}
