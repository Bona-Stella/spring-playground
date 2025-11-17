package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
