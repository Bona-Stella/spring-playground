package com.github.stella.springapiboard.board.web;

import com.github.stella.springapiboard.board.dto.StatsDtos;
import com.github.stella.springapiboard.board.service.StatsService;
import com.github.stella.springapiboard.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/posts/daily")
    public ResponseEntity<ApiResponse<List<StatsDtos.DailyCount>>> dailyPosts(@RequestParam(defaultValue = "30") int days,
                                                                              HttpServletRequest request) {
        var list = statsService.dailyPosts(days);
        return ResponseEntity.ok(ApiResponse.success(list, request.getRequestURI()));
    }

    @GetMapping("/categories/top")
    public ResponseEntity<ApiResponse<List<StatsDtos.TopItem>>> topCategories(@RequestParam(defaultValue = "5") int limit,
                                                                              HttpServletRequest request) {
        var list = statsService.topCategories(limit);
        return ResponseEntity.ok(ApiResponse.success(list, request.getRequestURI()));
    }

    @GetMapping("/tags/top")
    public ResponseEntity<ApiResponse<List<StatsDtos.TopItem>>> topTags(@RequestParam(defaultValue = "5") int limit,
                                                                        HttpServletRequest request) {
        var list = statsService.topTags(limit);
        return ResponseEntity.ok(ApiResponse.success(list, request.getRequestURI()));
    }

    @GetMapping("/authors/top")
    public ResponseEntity<ApiResponse<List<StatsDtos.TopItem>>> topAuthors(@RequestParam(defaultValue = "5") int limit,
                                                                           HttpServletRequest request) {
        var list = statsService.topAuthors(limit);
        return ResponseEntity.ok(ApiResponse.success(list, request.getRequestURI()));
    }
}
