package com.lakeserl.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "product_tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_tag", columnNames = {"product_id", "tag_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
