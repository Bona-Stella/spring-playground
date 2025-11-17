package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.dto.CreatePostRequest;
import com.github.stella.springapiboard.board.dto.PostDto;
import com.github.stella.springapiboard.board.dto.UpdatePostRequest;
import com.github.stella.springapiboard.board.repository.PostRepository;
import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostDto create(CreatePostRequest req) {
        Post post = new Post(req.title(), req.content(), req.author());
        Post saved = postRepository.save(post);
        return PostDto.from(saved);
    }

    public PostDto get(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return PostDto.from(post);
    }

    @Transactional
    public PostDto update(Long id, UpdatePostRequest req) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        post.update(req.title(), req.content());
        return PostDto.from(post);
    }

    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        postRepository.deleteById(id);
    }

    public Page<PostDto> list(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostDto::from);
    }
}
