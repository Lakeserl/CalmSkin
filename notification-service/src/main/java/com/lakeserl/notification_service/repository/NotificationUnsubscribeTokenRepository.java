package com.lakeserl.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.notification_service.entity.NotificationUnsubscribeToken;

@Repository
public interface NotificationUnsubscribeTokenRepository
        extends JpaRepository<NotificationUnsubscribeToken, String> {
}
