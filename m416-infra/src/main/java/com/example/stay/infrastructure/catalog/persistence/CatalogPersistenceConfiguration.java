package com.example.stay.infrastructure.catalog.persistence;

import com.example.stay.infrastructure.catalog.persistence.entity.StayMappingRecord;
import com.example.stay.infrastructure.catalog.persistence.repository.CatalogStateRepositoryImpl;
import com.example.stay.infrastructure.catalog.persistence.repository.RoomMappingRepositoryImpl;
import com.example.stay.infrastructure.catalog.persistence.repository.StayMappingRepositoryImpl;
import com.example.stay.infrastructure.catalog.persistence.repository.StayMappingJpaRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = StayMappingRecord.class)
@EnableJpaRepositories(basePackageClasses = StayMappingJpaRepository.class)
@Import({StayMappingRepositoryImpl.class, RoomMappingRepositoryImpl.class, CatalogStateRepositoryImpl.class})
public class CatalogPersistenceConfiguration {
}
