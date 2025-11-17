package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.domain.Category;
import com.github.stella.springapiboard.board.dto.CategoryDtos;
import com.github.stella.springapiboard.board.repository.CategoryRepository;
import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Page<CategoryDtos.CategoryDto> list(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(CategoryDtos.CategoryDto::from);
    }

    public CategoryDtos.CategoryDto get(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return CategoryDtos.CategoryDto.from(c);
    }

    @Transactional
    public CategoryDtos.CategoryDto create(CategoryDtos.CreateCategoryRequest req) {
        Category c = new Category(req.name(), req.slug(), req.description());
        Category saved = categoryRepository.save(c);
        return CategoryDtos.CategoryDto.from(saved);
    }

    @Transactional
    public CategoryDtos.CategoryDto update(Long id, CategoryDtos.UpdateCategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        c.update(req.name(), req.slug(), req.description());
        return CategoryDtos.CategoryDto.from(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }
}
