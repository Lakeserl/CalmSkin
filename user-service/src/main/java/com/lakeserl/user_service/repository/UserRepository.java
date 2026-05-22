package com.lakeserl.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lakeserl.user_service.model.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.fullName = :fullName")
    Optional<User> findByName(String fullName);
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(u) > 0 " +
            "THEN true " +
            "ELSE false " +
            "END FROM User u WHERE u.email = :email")
    Boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 " +
            "THEN true " +
            "ELSE false " +
            "END FROM User u WHERE u.phoneNumber = :phoneNumber")
    Boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT u.id FROM User u WHERE EXTRACT(MONTH FROM u.dateOfBirth) = :month "
            + "AND EXTRACT(DAY FROM u.dateOfBirth) = :day")
    List<UUID> findUserIdsByBirthday(@Param("month") int month, @Param("day") int day);

}
