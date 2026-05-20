package com.lakeserl.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.payment_service.models.entity.ProcessedKafkaEvent;

@Repository
public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, String> {
}
