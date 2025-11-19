package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.CreatePostRequest;
import com.github.stella.springapiboard.board.dto.PostDto;
import com.github.stella.springapiboard.board.dto.UpdatePostRequest;
import com.github.stella.springapiboard.board.dto.PostSearchDtos;
import com.github.stella.springapiboard.board.service.PostService;
import com.github.stella.springapiboard.common.api.ApiResponse;
import com.github.stella.springapiboard.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Posts", description = "게시글 API: CRUD, 검색")
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "게시글 단건 조회", description = "ID로 게시글을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(
            @Parameter(description = "게시글 ID", example = "1", required = true)
            @PathVariable Long id,
            // hidden = true -> Swagger에 노출 안함
            @Parameter(hidden = true) HttpServletRequest request) {
        PostDto dto = postService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @Operation(
            summary = "게시글 페이지 조회",
            description = "페이지네이션으로 게시글 목록을 조회합니다. 정렬은 `sort=필드,ASC|DESC` 형식으로 지정합니다. 예: `sort=createdAt,DESC`"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> list(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) HttpServletRequest request) {
        Page<PostDto> page = postService.list(pageable);
        PageResponse<PostDto> body = PageResponse.from(page);
        return ResponseEntity.ok(ApiResponse.success(body, request.getRequestURI()));
    }

    @Operation(
            summary = "게시글 검색",
            description = "키워드(제목/내용), 작성자, 카테고리ID, 태그ID(AND 조건), 작성일 기간(from~to)으로 검색합니다. \n"
                    + "예: `/api/v1/posts/search?keyword=jpa&author=stella&categoryId=1&tagIds=2&tagIds=3&from=2025-01-01&to=2025-12-31`"
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> search(
            @ParameterObject PostSearchDtos.PostSearchCondition condition,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) HttpServletRequest request) {
        Page<PostDto> page = postService.search(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), request.getRequestURI()));
    }

    @Operation(
            summary = "게시글 생성",
            description = "새 게시글을 생성합니다. 선택적으로 카테고리/태그를 지정할 수 있습니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> create(@Valid @RequestBody CreatePostRequest req,
                                                       @Parameter(hidden = true) HttpServletRequest request) {
        PostDto created = postService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, request.getRequestURI()));
    }

    @Operation(
            summary = "게시글 수정",
            description = "제목/내용 및 카테고리/태그를 수정합니다. 태그는 전달 시 전체 재설정됩니다."
    )
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> update(
            @Parameter(description = "게시글 ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest req,
            @Parameter(hidden = true) HttpServletRequest request) {
        PostDto updated = postService.update(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, request.getRequestURI()));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 ID", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true) HttpServletRequest request) {
        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, request.getRequestURI()));
    }
}
