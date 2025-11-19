package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.domain.Category;
import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.domain.Tag;
import com.github.stella.springapiboard.board.dto.CreatePostRequest;
import com.github.stella.springapiboard.board.dto.PostDto;
import com.github.stella.springapiboard.board.dto.UpdatePostRequest;
import com.github.stella.springapiboard.board.dto.PostSearchDtos;
import com.github.stella.springapiboard.board.repository.PostQueryRepository;
import com.github.stella.springapiboard.board.repository.CategoryRepository;
import com.github.stella.springapiboard.board.repository.PostRepository;
import com.github.stella.springapiboard.board.repository.TagRepository;
import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostQueryRepository postQueryRepository;
    public PostServiceImpl(PostRepository postRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           PostQueryRepository postQueryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.postQueryRepository = postQueryRepository;
    }

    @Override
    @Transactional
    public PostDto create(CreatePostRequest req) {
        Post post = new Post(req.title(), req.content(), req.author());

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
            post.changeCategory(category);
        }
        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(req.tagIds());
            tags.forEach(post::addTag);
        }

        Post saved = postRepository.save(post);
        return PostDto.from(saved);
    }

    @Override
    public PostDto get(Long id) {
        Post post = postRepository.findWithRelationsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return PostDto.from(post);
    }

    @Override
    @Transactional
    public PostDto update(Long id, UpdatePostRequest req) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        post.update(req.title(), req.content());

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
            post.changeCategory(category);
        }
        if (req.tagIds() != null) {
            post.getTags().clear();
            if (!req.tagIds().isEmpty()) {
                List<Tag> tags = tagRepository.findAllById(req.tagIds());
                tags.forEach(post::addTag);
            }
        }
        return PostDto.from(post);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        postRepository.deleteById(id);
    }

    @Override
    public Page<PostDto> list(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostDto::from);
    }

    @Override
    public Page<PostDto> search(PostSearchDtos.PostSearchCondition condition, Pageable pageable) {
        return postQueryRepository.search(condition, pageable).map(PostDto::from);
    }
}