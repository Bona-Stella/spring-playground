package com.github.stella.springttdrest.controller;

import com.github.stella.springttdrest.domain.PointHistory;
import com.github.stella.springttdrest.domain.UserPoint;
import com.github.stella.springttdrest.dto.PointChargeRequest;
import com.github.stella.springttdrest.dto.PointUseRequest;
import com.github.stella.springttdrest.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private final PointService pointService;

    @PatchMapping("/charge")
    public UserPoint charge(Principal principal, @RequestBody PointChargeRequest request) {
        long userId = Long.parseLong(principal.getName()); // "1" -> 1L 변환
        return pointService.charge(userId, request.amount());
    }

    @PatchMapping("/use")
    public UserPoint use(Principal principal, @RequestBody PointUseRequest request) {
        long userId = Long.parseLong(principal.getName());
        return pointService.use(userId, request.amount());
    }

    // URL 변경: /point/histories (내 거 조회)
    @GetMapping("/histories")
    public List<PointHistory> history(Principal principal) {
        long userId = Long.parseLong(principal.getName());
        return pointService.getHistory(userId);
    }
}