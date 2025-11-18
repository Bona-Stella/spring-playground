package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.domain.FileAttachment;
import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.dto.FileDtos;
import com.github.stella.springapiboard.board.repository.FileAttachmentRepository;
import com.github.stella.springapiboard.board.repository.PostRepository;
import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FileService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final PostRepository postRepository;
    private final Path uploadDir;

    public FileService(FileAttachmentRepository fileAttachmentRepository,
                       PostRepository postRepository,
                       @Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.postRepository = postRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + this.uploadDir, e);
        }
    }

    @Transactional
    public FileDtos.FileDto upload(MultipartFile file, Long postId) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Post post = null;
        if (postId != null) {
            post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        }

        String originalName = sanitize(file.getOriginalFilename());
        String savedName = UUID.randomUUID() + getExtension(originalName);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        long size = file.getSize();

        Path target = uploadDir.resolve(savedName);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }

        FileAttachment fa = new FileAttachment(originalName, savedName, contentType, size, target.toString(), post);
        FileAttachment saved = fileAttachmentRepository.save(fa);
        return FileDtos.FileDto.from(saved);
    }

    public FileDtos.FileDto get(Long id) {
        FileAttachment fa = fileAttachmentRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return FileDtos.FileDto.from(fa);
    }

    public List<FileDtos.FileDto> listByPost(Long postId) {
        return fileAttachmentRepository.findByPost_Id(postId).stream().map(FileDtos.FileDto::from).toList();
    }

    public Resource loadAsResource(Long id) {
        FileAttachment fa = fileAttachmentRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Resource res = new FileSystemResource(fa.getPath());
        if (!res.exists()) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return res;
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        String clean = filename.replace("\\", "/");
        clean = clean.substring(clean.lastIndexOf('/') + 1);
        clean = clean.replaceAll("[\r\n]", "");
        return StringUtils.hasText(clean) ? clean : "file";
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}
