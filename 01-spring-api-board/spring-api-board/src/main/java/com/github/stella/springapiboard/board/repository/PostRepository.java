package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<Post> findWithRelationsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    Page<Post> findAll(Pageable pageable);
}
