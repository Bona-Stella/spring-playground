package com.github.stella.springredisjob.domain.post;

import com.github.stella.springredisjob.common.api.ApiResponse;
import com.github.stella.springredisjob.domain.post.dto.CreatePostRequest;
import com.github.stella.springredisjob.domain.post.dto.PostDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/posts")
public class PostAsyncController {
    private final PostAsyncService postAsyncService;
    private final PostEventPublisher eventPublisher;

    public PostAsyncController(PostAsyncService postAsyncService, PostEventPublisher eventPublisher) {
        this.postAsyncService = postAsyncService;
        this.eventPublisher = eventPublisher;
    }

    // 논블로킹 조회: CompletableFuture 반환
    @GetMapping("/async/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<PostDto>>> getAsync(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return postAsyncService.getAsync(id)
                .thenApply(dto -> ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI())));
    }

    // 논블로킹 생성: CompletableFuture 반환(+ 비동기 감사 로그)
    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<ApiResponse<PostDto>>> createAsync(
            @Valid @RequestBody CreatePostRequest req,
            HttpServletRequest request
    ) {
        return postAsyncService.createAsync(req)
                .thenApply(dto -> {
                    // fire-and-forget 추가 작업(감사 로그 등)
                    postAsyncService.auditAsync(dto);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.created(dto, request.getRequestURI()));
                });
    }

    // 큐잉: Redis Pub/Sub으로 이벤트 발행 후 즉시 응답
    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<String>> queueCreate(
            @Valid @RequestBody CreatePostRequest req,
            HttpServletRequest request
    ) {
        eventPublisher.publishCreate(req.title(), req.content());
        return ResponseEntity.ok(ApiResponse.success("queued", request.getRequestURI()));
    }
}
