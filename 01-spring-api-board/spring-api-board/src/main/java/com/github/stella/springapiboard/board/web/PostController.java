package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.CreatePostRequest;
import com.github.stella.springapiboard.board.dto.PostDto;
import com.github.stella.springapiboard.board.dto.UpdatePostRequest;
import com.github.stella.springapiboard.board.dto.PostSearchDtos;
import com.github.stella.springapiboard.board.service.PostService;
import com.github.stella.springapiboard.common.api.ApiResponse;
import com.github.stella.springapiboard.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(@PathVariable Long id, HttpServletRequest request) {
        PostDto dto = postService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> list(Pageable pageable, HttpServletRequest request) {
        Page<PostDto> page = postService.list(pageable);
        PageResponse<PostDto> body = PageResponse.from(page);
        return ResponseEntity.ok(ApiResponse.success(body, request.getRequestURI()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> search(PostSearchDtos.PostSearchCondition condition,
                                                                     Pageable pageable,
                                                                     HttpServletRequest request) {
        Page<PostDto> page = postService.search(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> create(@Valid @RequestBody CreatePostRequest req,
                                                       HttpServletRequest request) {
        PostDto created = postService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, request.getRequestURI()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdatePostRequest req,
                                                       HttpServletRequest request) {
        PostDto updated = postService.update(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, HttpServletRequest request) {
        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, request.getRequestURI()));
    }
}
