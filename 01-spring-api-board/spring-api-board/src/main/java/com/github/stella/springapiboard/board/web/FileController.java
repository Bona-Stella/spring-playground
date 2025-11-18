package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.FileDtos;
import com.github.stella.springapiboard.board.service.FileService;
import com.github.stella.springapiboard.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileDtos.FileDto>> upload(@RequestPart("file") MultipartFile file,
                                                                @RequestParam(value = "postId", required = false) Long postId,
                                                                HttpServletRequest request) {
        var dto = fileService.upload(file, postId);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileDtos.FileDto>> get(@PathVariable Long id, HttpServletRequest request) {
        var dto = fileService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto, request.getRequestURI()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var meta = fileService.get(id);
        Resource resource = fileService.loadAsResource(id);
        String filename = URLEncoder.encode(meta.originalName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(meta.contentType()))
                .contentLength(meta.size())
                .body(resource);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<List<FileDtos.FileDto>>> listByPost(@PathVariable Long postId,
                                                                          HttpServletRequest request) {
        var list = fileService.listByPost(postId);
        return ResponseEntity.ok(ApiResponse.success(list, request.getRequestURI()));
    }
}
