package com.lakeserl.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.product_service.entity.ProcessedKafkaEvent;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, String> {
}
