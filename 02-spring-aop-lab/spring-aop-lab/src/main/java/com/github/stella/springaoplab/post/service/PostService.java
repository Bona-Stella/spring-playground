package com.github.stella.springaoplab.post.service;

import com.github.stella.springaoplab.aop.LogExecutionTime;
import com.github.stella.springaoplab.common.error.CustomException;
import com.github.stella.springaoplab.common.error.ErrorCode;
import com.github.stella.springaoplab.post.domain.Post;
import com.github.stella.springaoplab.post.dto.CreatePostRequest;
import com.github.stella.springaoplab.post.dto.PostDto;
import com.github.stella.springaoplab.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    @LogExecutionTime
    public PostDto create(CreatePostRequest req) {
        Post saved = postRepository.save(new Post(req.title(), req.content()));
        return PostDto.from(saved);
    }

    public PostDto get(Long id) {
        return PostDto.from(postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND)));
    }

    public List<PostDto> list() {
        return postRepository.findAll().stream().map(PostDto::from).toList();
    }

    @Transactional
    @LogExecutionTime
    public PostDto updateTitle(Long id, String title) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.setTitle(title);
        return PostDto.from(post);
    }

    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        postRepository.deleteById(id);
    }

    // ===== Self-invocation demo =====
    public String outerCallForDemo() {
        // 내부 호출 - 프록시를 거치지 않으므로 @LogExecutionTime이 적용되지 않음
        return innerAnnotatedWork();
    }

    @LogExecutionTime
    public String innerAnnotatedWork() {
        // 일부러 약간의 지연을 줘서 AOP 시간 측정 확인
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        return "done";
    }
}
