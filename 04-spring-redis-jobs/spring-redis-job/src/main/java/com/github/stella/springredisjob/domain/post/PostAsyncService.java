package com.github.stella.springredisjob.domain.post;

import com.github.stella.springredisjob.domain.post.dto.CreatePostRequest;
import com.github.stella.springredisjob.domain.post.dto.PostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
public class PostAsyncService {
    private static final Logger log = LoggerFactory.getLogger(PostAsyncService.class);
    private final PostService postService;

    public PostAsyncService(PostService postService) {
        this.postService = postService;
    }

    // 비동기 조회 (가상 스레드 @Async)
    @Async
    public CompletableFuture<PostDto> getAsync(Long id) {
        /*
        try {
            PostDto dto = postService.get(id);
            return CompletableFuture.completedFuture(dto);
        } catch (Exception e) {
            CompletableFuture<PostDto> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
        */
        return CompletableFuture.completedFuture(postService.get(id));
    }

    // 비동기 생성 (가상 스레드 @Async)
    @Async
    @Transactional
    public CompletableFuture<PostDto> createAsync(CreatePostRequest req) {
        /*
        try {
            PostDto created = postService.create(req);
            return CompletableFuture.completedFuture(created);
        } catch (Exception e) {
            CompletableFuture<PostDto> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
        */
        return CompletableFuture.completedFuture(postService.create(req));
    }

    // 부가처리 예시: 감사 로그 비동기 기록
    @Async
    public void auditAsync(PostDto dto) {
        log.info("[POST][AUDIT][ASYNC] id={} title={}", dto.id(), dto.title());
    }
}
