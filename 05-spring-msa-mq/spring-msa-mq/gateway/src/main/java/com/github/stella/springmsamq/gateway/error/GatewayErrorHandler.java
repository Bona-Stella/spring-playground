package com.github.stella.springmsamq.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.common.ErrorCode;
import com.github.stella.springmsamq.common.ErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        ErrorCode code;

        if (ex instanceof OAuth2AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
            code = ErrorCode.UNAUTHORIZED;
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = mapStatusToCode(status);
        } else if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            status = HttpStatus.BAD_REQUEST;
            code = ErrorCode.INVALID_INPUT;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = ErrorCode.INTERNAL_SERVER_ERROR;
        }

        ErrorResponse body = new ErrorResponse(false,
                status.value(),
                code.name(),
                code.message(),
                java.time.ZonedDateTime.now().toString(),
                exchange.getRequest().getPath().value());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            String fallback = "{\"success\":false,\"status\":" + status.value() + ",\"code\":\"" + code.name() + "\",\"message\":\"" + code.message() + "\"}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        var resp = exchange.getResponse();
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        resp.setStatusCode(status);
        var buffer = resp.bufferFactory().wrap(bytes);
        return resp.writeWith(Mono.just(buffer));
    }

    private ErrorCode mapStatusToCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.INVALID_INPUT;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.ACCESS_DENIED;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case CONFLICT -> ErrorCode.DUPLICATE_RESOURCE;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}
