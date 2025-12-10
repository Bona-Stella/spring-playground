package com.github.stella.springmsamq.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.common.ErrorCode;
import com.github.stella.springmsamq.common.ErrorResponse;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.Map;

@Component
@Order(-2)
public class GatewayErrorHandler extends AbstractErrorWebExceptionHandler {

    public GatewayErrorHandler(ErrorAttributes errorAttributes,
                               WebProperties webProperties,
                               ApplicationContext applicationContext,
                               ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        // "어떤 에러가 오든(all), renderErrorResponse 메서드로 보내라"
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private ServerResponse.BodyBuilder withJson(HttpStatus status) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON);
    }

    private reactor.core.publisher.Mono<ServerResponse> build(ServerRequest request, HttpStatus status, ErrorCode code) {
        String traceId = request.exchange().getRequest().getId();
        String message = code.message() + " [traceId=" + traceId + "]";
        ErrorResponse body = new ErrorResponse(
                false,
                status.value(),
                code.name(),
                message,
                java.time.ZonedDateTime.now().toString(),
                request.path()
        );
        return withJson(status).body(BodyInserters.fromValue(body));
    }

    private reactor.core.publisher.Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = super.getError(request);

        // 기본 상태/코드 결정
        HttpStatus status;
        ErrorCode code;

        if (error instanceof OAuth2AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
            code = ErrorCode.UNAUTHORIZED;
        } else if (error instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = mapStatusToCode(status);
        } else if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            status = HttpStatus.BAD_REQUEST;
            code = ErrorCode.INVALID_INPUT;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = ErrorCode.INTERNAL_SERVER_ERROR;
        }

        // 필요 시 에러 속성 로깅용으로만 사용
        Map<String, Object> attrs = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        // (여기서는 attrs를 응답 바디에 노출하지 않습니다)

        return build(request, status, code);
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
