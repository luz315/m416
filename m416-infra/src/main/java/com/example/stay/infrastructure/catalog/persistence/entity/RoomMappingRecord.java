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
@Table(name = "room_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stay_id", "room_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMappingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "stay_id", nullable = false)
    private Long stayId;
    @Column(name = "room_code", nullable = false)
    private String roomCode;
    @Column(nullable = false)
    private boolean active;
}
