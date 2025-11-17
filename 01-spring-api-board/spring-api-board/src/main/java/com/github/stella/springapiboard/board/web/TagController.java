package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.TagDtos;
import com.github.stella.springapiboard.board.service.TagService;
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
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TagDtos.TagDto>>> list(Pageable pageable, HttpServletRequest request) {
        Page<TagDtos.TagDto> page = tagService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDtos.TagDto>> get(@PathVariable Long id, HttpServletRequest request) {
        var dto = tagService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagDtos.TagDto>> create(@Valid @RequestBody TagDtos.CreateTagRequest req,
                                                              HttpServletRequest request) {
        var created = tagService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, request.getRequestURI()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDtos.TagDto>> update(@PathVariable Long id,
                                                              @Valid @RequestBody TagDtos.UpdateTagRequest req,
                                                              HttpServletRequest request) {
        var updated = tagService.update(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, HttpServletRequest request) {
        tagService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, request.getRequestURI()));
    }
}
