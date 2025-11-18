package com.github.stella.springredisjob.domain.post;

import com.github.stella.springredisjob.common.error.CustomException;
import com.github.stella.springredisjob.common.error.ErrorCode;
import com.github.stella.springredisjob.domain.post.dto.CreatePostRequest;
import com.github.stella.springredisjob.domain.post.dto.PostDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "post", key = "#id")
    @Transactional(readOnly = true)
    public PostDto get(Long id) {
        Post post = repository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return PostDto.from(post);
    }

    @CacheEvict(value = "post", key = "#result.id")
    @Transactional
    public PostDto create(CreatePostRequest req) {
        Post saved = repository.save(new Post(req.title(), req.content()));
        return PostDto.from(saved);
    }
}
