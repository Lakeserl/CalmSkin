package com.lakeserl.notification_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.notification_service.entity.NotificationTemplate;
import com.lakeserl.notification_service.enums.NotificationChannel;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findFirstByCodeAndChannelAndLocaleAndIsActiveTrue(
            String code, NotificationChannel channel, String locale);

    Optional<NotificationTemplate> findFirstByCodeAndChannelAndIsActiveTrue(
            String code, NotificationChannel channel);
}
