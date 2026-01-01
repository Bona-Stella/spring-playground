package com.github.stella.springttdrest.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 유저의 내역을 시간 역순(최신순)으로 조회
    List<PointHistory> findAllByUserIdOrderByUpdateMillisDesc(Long userId);
}