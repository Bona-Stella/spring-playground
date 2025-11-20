package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.CategoryDtos;
import com.github.stella.springapiboard.board.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryDtos.CategoryDto>>> list(Pageable pageable, HttpServletRequest request) {
        Page<CategoryDtos.CategoryDto> page = categoryService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDtos.CategoryDto>> get(@PathVariable Long id, HttpServletRequest request) {
        var dto = categoryService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDtos.CategoryDto>> create(@Valid @RequestBody CategoryDtos.CreateCategoryRequest req,
                                                                        HttpServletRequest request) {
        var created = categoryService.create(req);
        // ResponseEntity.created -> Body(X)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, request.getRequestURI()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDtos.CategoryDto>> update(@PathVariable Long id,
                                                                        @Valid @RequestBody CategoryDtos.UpdateCategoryRequest req,
                                                                        HttpServletRequest request) {
        var updated = categoryService.update(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, HttpServletRequest request) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, request.getRequestURI()));
    }
}
