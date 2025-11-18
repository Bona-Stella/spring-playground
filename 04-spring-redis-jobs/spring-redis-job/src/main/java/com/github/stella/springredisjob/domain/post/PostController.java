package com.github.stella.springredisjob.domain.post;

import com.github.stella.springredisjob.common.api.ApiResponse;
import com.github.stella.springredisjob.domain.post.dto.CreatePostRequest;
import com.github.stella.springredisjob.domain.post.dto.PostDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        PostDto dto = postService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> create(
            @Valid @RequestBody CreatePostRequest req,
            HttpServletRequest request
    ) {
        PostDto created = postService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, request.getRequestURI()));
    }
}
