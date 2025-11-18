package com.github.stella.springaoplab.post.repository;

import com.github.stella.springaoplab.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    long countByTitleStartingWith(String prefix);
}
