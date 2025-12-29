package com.github.stella.springttdrest.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(Long userId);
}