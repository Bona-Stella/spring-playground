package com.github.stella.springsecurityjwt.security;

import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import com.github.stella.springsecurityjwt.common.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very simple in-memory sliding-window rate limiter per IP+path.
 * For demo only. In production, prefer bucket4j/Redis or API gateway.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${ratelimit.max-requests:100}") int maxRequests,
            @Value("${ratelimit.window-millis:300000}") long windowMillis
    ) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMillis = Math.max(1000, windowMillis);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = buildKey(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> deque = requestTimes.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            // purge old
            long threshold = now - windowMillis;
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                writeTooMany(response, request);
                return;
            }
            deque.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request) {
        String ip = getClientIp(request);
        String path = request.getRequestURI();
        return ip + "|" + path;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return Objects.toString(request.getRemoteAddr(), "unknown");
    }

    private void writeTooMany(HttpServletResponse response, HttpServletRequest request) throws IOException {
        ErrorResponse body = ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS, request.getRequestURI());
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = "{" +
                "\"success\":false," +
                "\"status\":" + body.status() + "," +
                "\"code\":\"" + body.code() + "\"," +
                "\"message\":\"" + body.message().replace("\"","'") + "\"," +
                "\"timestamp\":\"" + body.timestamp() + "\"," +
                "\"path\":\"" + body.path() + "\"" +
                "}";
        response.getWriter().write(json);
    }
}
