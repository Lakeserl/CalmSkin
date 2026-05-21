package com.lakeserl.notification_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.notification_service.entity.UserContact;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, UUID> {
}
