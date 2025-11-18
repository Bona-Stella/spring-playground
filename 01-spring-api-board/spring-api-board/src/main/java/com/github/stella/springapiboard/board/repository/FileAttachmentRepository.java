package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    List<FileAttachment> findByPost_Id(Long postId);
}
