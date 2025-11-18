package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.dto.PostSearchDtos.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryRepository {
    Page<Post> search(PostSearchCondition condition, Pageable pageable);
}
