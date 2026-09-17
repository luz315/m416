package com.example.stay.infrastructure.catalog.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stay_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"supplier", "stay_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StayMappingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(nullable = false)
    private String supplier;
    @Column(name = "stay_code", nullable = false)
    private String stayCode;
    @Column(nullable = false)
    private boolean active;
}
