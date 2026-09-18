package com.example.stay.infrastructure.common.config;

import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.application.service.CatalogMappingWriter;
import com.example.stay.catalog.application.usecase.impl.SyncSupplierStaysUseCaseImpl;
import com.example.stay.search.application.usecase.impl.SearchAvailableStaysUseCaseImpl;
import com.example.stay.infrastructure.catalog.persistence.CatalogPersistenceConfiguration;
import com.example.stay.infrastructure.http.supplier.config.SupplierWebClientConfiguration;
import com.example.stay.infrastructure.http.supplier.normalization.OfferNormalizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({CatalogPersistenceConfiguration.class, SupplierWebClientConfiguration.class})
@ComponentScan(basePackageClasses = {CatalogMappingWriter.class, SyncSupplierStaysUseCaseImpl.class, SearchAvailableStaysUseCaseImpl.class})
public class IntegrationConfiguration {
    @Bean
    OfferNormalizer offerNormalizer(StayMappingRepository stays, RoomMappingRepository rooms) {
        return new OfferNormalizer(stays, rooms);
    }
}
