package com.github.stella.springredisjob.domain.post.dto;

import com.github.stella.springredisjob.domain.post.Post;

public record PostDto(Long id, String title, String content) {
    public static PostDto from(Post p) {
        return new PostDto(p.getId(), p.getTitle(), p.getContent());
    }
}
