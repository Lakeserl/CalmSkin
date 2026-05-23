package com.lakeserl.user_service.model.entity;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import com.lakeserl.user_service.model.enums.RoleName;

import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "hibernate_sequence", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NaturalId
    @Column(name = "roles", length = 60)
    private RoleName name;

}
