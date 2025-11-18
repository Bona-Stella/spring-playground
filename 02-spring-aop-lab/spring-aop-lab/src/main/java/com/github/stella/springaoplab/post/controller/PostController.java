package com.github.stella.springaoplab.post.controller;

import com.github.stella.springaoplab.common.api.ApiResponse;
import com.github.stella.springaoplab.post.dto.CreatePostRequest;
import com.github.stella.springaoplab.post.dto.PostDto;
import com.github.stella.springaoplab.post.dto.UpdateTitleRequest;
import com.github.stella.springaoplab.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostDto>>> list(HttpServletRequest request) {
        List<PostDto> posts = postService.list();
        return ResponseEntity.ok(ApiResponse.success(posts, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(@PathVariable Long id, HttpServletRequest request) {
        PostDto dto = postService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> create(@Valid @RequestBody CreatePostRequest req,
                                                       HttpServletRequest request) {
        PostDto dto = postService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(dto, request.getRequestURI()));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<ApiResponse<PostDto>> updateTitle(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateTitleRequest req,
                                                            HttpServletRequest request) {
        PostDto dto = postService.updateTitle(id, req.title());
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, HttpServletRequest request) {
        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, request.getRequestURI()));
    }

    // ===== AOP self-invocation demo =====
    @GetMapping("/demo/outer")
    public ResponseEntity<ApiResponse<String>> outer(HttpServletRequest request) {
        String result = postService.outerCallForDemo();
        return ResponseEntity.ok(ApiResponse.success(result, request.getRequestURI()));
    }

    @GetMapping("/demo/inner")
    public ResponseEntity<ApiResponse<String>> inner(HttpServletRequest request) {
        String result = postService.innerAnnotatedWork();
        return ResponseEntity.ok(ApiResponse.success(result, request.getRequestURI()));
    }
}
