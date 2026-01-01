package com.github.stella.springttdrest.controller;

import com.github.stella.springttdrest.domain.PointHistory;
import com.github.stella.springttdrest.domain.UserPoint;
import com.github.stella.springttdrest.dto.PointChargeRequest;
import com.github.stella.springttdrest.dto.PointUseRequest;
import com.github.stella.springttdrest.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private final PointService pointService;

    @PatchMapping("/charge")
    public UserPoint charge(@RequestBody PointChargeRequest request) {
        // 실제 서비스 로직 호출
        return pointService.charge(request.userId(), request.amount());
    }

    @PatchMapping("/use")
    public UserPoint use(@RequestBody PointUseRequest request) {
        return pointService.use(request.userId(), request.amount());
    }

    // 포인트 내역 조회
    @GetMapping("/{id}/histories")
    public List<PointHistory> history(@PathVariable Long id) {
        return pointService.getHistory(id);
    }
}