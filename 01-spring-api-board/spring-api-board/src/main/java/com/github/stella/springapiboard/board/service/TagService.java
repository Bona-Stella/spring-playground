package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.domain.Tag;
import com.github.stella.springapiboard.board.dto.TagDtos;
import com.github.stella.springapiboard.board.repository.TagRepository;
import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Page<TagDtos.TagDto> list(Pageable pageable) {
        return tagRepository.findAll(pageable).map(TagDtos.TagDto::from);
    }

    public TagDtos.TagDto get(Long id) {
        Tag t = tagRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return TagDtos.TagDto.from(t);
    }

    @Transactional
    public TagDtos.TagDto create(TagDtos.CreateTagRequest req) {
        Tag t = new Tag(req.name(), req.slug());
        Tag saved = tagRepository.save(t);
        return TagDtos.TagDto.from(saved);
    }

    @Transactional
    public TagDtos.TagDto update(Long id, TagDtos.UpdateTagRequest req) {
        Tag t = tagRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        t.update(req.name(), req.slug());
        return TagDtos.TagDto.from(t);
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        tagRepository.deleteById(id);
    }
}
