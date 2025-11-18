package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.dto.CreatePostRequest;
import com.github.stella.springapiboard.board.dto.PostDto;
import com.github.stella.springapiboard.board.dto.PostSearchDtos;
import com.github.stella.springapiboard.board.dto.UpdatePostRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostDto create(CreatePostRequest req);

    PostDto get(Long id);

    PostDto update(Long id, UpdatePostRequest req);

    void delete(Long id);

    Page<PostDto> list(Pageable pageable);

    Page<PostDto> search(PostSearchDtos.PostSearchCondition condition, Pageable pageable);
}
