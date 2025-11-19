package com.github.stella.springredisjob.external.web;

import com.github.stella.springredisjob.common.api.ApiResponse;
import com.github.stella.springredisjob.external.mock.ExternalService;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import com.github.stella.springredisjob.external.result.ExternalResultService;
import com.github.stella.springredisjob.external.result.dto.ExternalResultDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture 기반 논블로킹 외부 호출 데모 컨트롤러
 * 경로를 기존 컨트롤러와 구분하여 "/api/external/mock/async-future"로 구성
 */
@RestController
@RequestMapping("/api/external/mock/async-future")
public class ExternalAsyncController {
    private final ExternalService externalService;
    private final ExternalResultService resultService;

    public ExternalAsyncController(ExternalService externalService, ExternalResultService resultService) {
        this.externalService = externalService;
        this.resultService = resultService;
    }

    // 1) 결과를 바로 반환하는 논블로킹 엔드포인트
    @GetMapping
    public CompletableFuture<ResponseEntity<ApiResponse<ExternalWeatherResponse>>> get(
            @RequestParam String city,
            HttpServletRequest request
    ) {
        return externalService.fetchAsyncFuture(city)
                .thenApply(res -> ResponseEntity.ok(ApiResponse.success(res, request.getRequestURI())));
    }

    // 2) 결과를 DB에 저장한 뒤 저장 레코드를 반환하는 논블로킹 엔드포인트
    @PostMapping("/save")
    public CompletableFuture<ResponseEntity<ApiResponse<ExternalResultDto>>> save(
            @RequestParam String city,
            HttpServletRequest request
    ) {
        return externalService.fetchAsyncFuture(city)
                .thenApply(resultService::saveFromResponse)
                .thenApply(dto -> ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI())));
    }
}
