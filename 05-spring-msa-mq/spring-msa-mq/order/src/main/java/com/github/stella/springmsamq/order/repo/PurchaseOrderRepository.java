package com.github.stella.springmsamq.order.repo;

import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
}
