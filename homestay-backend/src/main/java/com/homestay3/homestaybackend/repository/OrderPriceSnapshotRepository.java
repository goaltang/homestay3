package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.OrderPriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderPriceSnapshotRepository extends JpaRepository<OrderPriceSnapshot, Long> {

    Optional<OrderPriceSnapshot> findByOrderId(Long orderId);
    
    boolean existsByOrderId(Long orderId);
}
