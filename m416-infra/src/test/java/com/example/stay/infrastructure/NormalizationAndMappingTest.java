package com.example.stay.infrastructure;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.application.service.CatalogMappingWriter;
import com.example.stay.catalog.application.usecase.SyncSupplierStaysUseCase;
import com.example.stay.catalog.application.usecase.impl.SyncSupplierStaysUseCaseImpl;
import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.common.exception.InvalidCatalogException;
import com.example.stay.common.exception.InvalidSupplierResponseException;
import com.example.stay.infrastructure.catalog.persistence.entity.StayMappingRecord;
import com.example.stay.infrastructure.catalog.persistence.repository.CatalogStateJpaRepository;
import com.example.stay.infrastructure.catalog.persistence.repository.CatalogStateRepositoryImpl;
import com.example.stay.infrastructure.catalog.persistence.repository.RoomMappingJpaRepository;
import com.example.stay.infrastructure.catalog.persistence.repository.RoomMappingRepositoryImpl;
import com.example.stay.infrastructure.catalog.persistence.repository.StayMappingJpaRepository;
import com.example.stay.infrastructure.catalog.persistence.repository.StayMappingRepositoryImpl;
import com.example.stay.infrastructure.http.supplier.a.dto.SupplierAResponse;
import com.example.stay.infrastructure.http.supplier.b.dto.SupplierBResponse;
import com.example.stay.infrastructure.http.supplier.normalization.OfferNormalizer;
import com.example.stay.search.domain.model.SearchCriteria;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.domain.model.Offer;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(NormalizationAndMappingTest.JpaTestConfiguration.class)
class NormalizationAndMappingTest {
    @Autowired SyncSupplierStaysUseCase catalogSync;
    @Autowired CatalogFixture catalogFixture;
    @Autowired StayMappingRepository stays;
    @Autowired RoomMappingRepository rooms;
    @Autowired CatalogStateRepository states;
    @Autowired OfferNormalizer normalizer;

    final LocalDate start = LocalDate.of(2026, 9, 1);

    @BeforeEach
    void setup() {
        store("A", List.of(hotel("H1"), hotel("H2")));
        store("B", List.of(hotel("H1")));
    }

    CatalogHotel hotel(String code) {
        return new CatalogHotel(code, "숙소", List.of(new CatalogHotel.CatalogRoom("R1", "객실", 2)));
    }

    @Test
    @DisplayName("공급사와 숙소까지 포함한 매핑 키를 사용하고 재수신·재생성에도 ID를 보존한다")
    void scopedStableIdentifiers() {
        var a = findRoom("A", "H1", "R1");
        assertNotEquals(a.getId(), findRoom("A", "H2", "R1").getId());
        assertNotEquals(a.getStayId(), findRoom("B", "H1", "R1").getStayId());

        store("A", List.of(hotel("H1")));
        assertMapping(a, findRoom("A", "H1", "R1"));
        assertTrue(findRoomOptional("A", "H2", "R1").isEmpty());

        store("A", List.of(hotel("H2"), hotel("H1")));
        assertMapping(a, findRoom("A", "H1", "R1"));
    }

    @Test
    @DisplayName("카탈로그 저장 중 오류가 나면 기존 매핑을 롤백한다")
    void atomicCatalog() {
        assertThrows(InvalidCatalogException.class, () -> store("A", List.of(hotel("H3"), hotel("H3"))));
        assertEquals(List.of("H1", "H2"), stayCodes("A"));
    }

    @Test
    @DisplayName("빈 카탈로그도 수신 완료로 기록하고 재등장한 객실의 ID를 보존한다")
    void emptyCatalogAndReactivation() {
        var original = findRoom("A", "H1", "R1");
        var otherSupplier = findRoom("B", "H1", "R1");
        assertFalse(states.existsBySupplier("UNKNOWN"));

        store("A", List.of());

        assertTrue(states.existsBySupplier("A"));
        assertTrue(stayCodes("A").isEmpty());
        assertTrue(findRoomOptional("A", "H1", "R1").isEmpty());
        assertMapping(otherSupplier, findRoom("B", "H1", "R1"));

        store("A", List.of(hotel("H1")));
        assertMapping(original, findRoom("A", "H1", "R1"));
    }

    @Test
    @DisplayName("객실 중복으로 저장에 실패하면 신규 숙소와 객실도 함께 롤백한다")
    void duplicateRoomRollsBack() {
        var original = findRoom("A", "H1", "R1");
        var room = new CatalogHotel.CatalogRoom("R2", "객실", 2);
        var invalid = new CatalogHotel("H3", "숙소", List.of(room, room));

        assertThrows(InvalidCatalogException.class, () -> store("A", List.of(invalid)));

        assertEquals(List.of("H1", "H2"), stayCodes("A"));
        assertMapping(original, findRoom("A", "H1", "R1"));
        assertTrue(findRoomOptional("A", "H3", "R2").isEmpty());
    }

    @Test
    @DisplayName("A는 세금을 더한 연박 총액, B는 제공한 총액을 보존한다")
    void totalPriceAndBreakfast() {
        var query = new SearchCriteria(start, start.plusDays(3), 2, 0);
        var a = normalizer.fromA(new SupplierAResponse.Item("H1", "ignored", "R1", "ignored", 2, false, "KRW", List.of(
                new SupplierAResponse.DailyRate(start, 3, 120000L, 12000L), new SupplierAResponse.DailyRate(start.plusDays(1), 1, 150000L, 15000L),
                new SupplierAResponse.DailyRate(start.plusDays(2), 5, 120000L, 12000L))), query);
        var b = normalizer.fromB(new SupplierBResponse.Item("H1", "ignored", "R1", "ignored", 2, true, "KRW", 452000L, true, List.of(
                new SupplierBResponse.Inventory(start, 3), new SupplierBResponse.Inventory(start.plusDays(1), 1), new SupplierBResponse.Inventory(start.plusDays(2), 5))), query);
        assertEquals(429000, a.price().totalIncludingTax());
        assertEquals(452000, b.price().totalIncludingTax());
        assertFalse(a.breakfastIncluded());
        assertTrue(b.breakfastIncluded());
        assertEquals(1, a.availableRooms());
    }

    @Test
    @DisplayName("중복 날짜와 세금 미포함 표시는 정규화 오류로 거부한다")
    void invalidSupplierData() {
        var query = new SearchCriteria(start, start.plusDays(2), 2, 0);
        var duplicate = new SupplierBResponse.Item("H1", "숙소", "R1", "객실", 2, true, "KRW", 100L, true, List.of(
                new SupplierBResponse.Inventory(start, 1), new SupplierBResponse.Inventory(start, 1)));
        assertThrows(InvalidSupplierResponseException.class, () -> normalizer.fromB(duplicate, query));
        var untaxed = new SupplierBResponse.Item("H1", "숙소", "R1", "객실", 2, true, "KRW", 100L, false, List.of());
        assertThrows(InvalidSupplierResponseException.class, () -> normalizer.fromB(untaxed, query));
    }

    private List<String> stayCodes(String supplier) {
        return stays.findAllActiveBySupplier(supplier).stream().map(StayMapping::getStayCode).toList();
    }

    private void store(String supplier, List<CatalogHotel> hotels) {
        catalogFixture.catalogs.put(supplier, hotels);
        catalogSync.execute();
    }

    private RoomMapping findRoom(String supplier, String stayCode, String roomCode) {
        return findRoomOptional(supplier, stayCode, roomCode).orElseThrow();
    }

    private Optional<RoomMapping> findRoomOptional(String supplier, String stayCode, String roomCode) {
        return stays.findBySupplierAndStayCode(supplier, stayCode)
                .filter(StayMapping::isActive)
                .flatMap(stay -> rooms.findByStayIdAndRoomCode(stay.getId(), roomCode))
                .filter(RoomMapping::isActive);
    }

    private static void assertMapping(RoomMapping expected, RoomMapping actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getStayId(), actual.getStayId());
        assertEquals(expected.getRoomCode(), actual.getRoomCode());
        assertEquals(expected.isActive(), actual.isActive());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = StayMappingJpaRepository.class)
    static class JpaTestConfiguration {
        @Bean DataSource dataSource() {
            var source = new DriverManagerDataSource();
            source.setDriverClassName("org.h2.Driver");
            source.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
            return source;
        }

        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(StayMappingRecord.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            var properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
        }

        @Bean StayMappingRepository stayMappingRepository(StayMappingJpaRepository repository) { return new StayMappingRepositoryImpl(repository); }
        @Bean RoomMappingRepository roomMappingRepository(RoomMappingJpaRepository repository) { return new RoomMappingRepositoryImpl(repository); }
        @Bean CatalogStateRepository catalogStateRepository(CatalogStateJpaRepository repository) { return new CatalogStateRepositoryImpl(repository); }
        @Bean CatalogFixture catalogFixture() {
            return new CatalogFixture();
        }
        @Bean SupplierClient supplierA(CatalogFixture fixture) {
            return new FixtureSupplierClient("A", fixture);
        }
        @Bean SupplierClient supplierB(CatalogFixture fixture) {
            return new FixtureSupplierClient("B", fixture);
        }
        @Bean CatalogMappingWriter catalogMappingWriter(StayMappingRepository stays, RoomMappingRepository rooms,
                                                        CatalogStateRepository states) {
            return new CatalogMappingWriter(stays, rooms, states);
        }
        @Bean SyncSupplierStaysUseCase catalogSync(List<SupplierClient> clients, CatalogMappingWriter writer) {
            return new SyncSupplierStaysUseCaseImpl(clients, writer);
        }
        @Bean OfferNormalizer offerNormalizer(StayMappingRepository stays, RoomMappingRepository rooms) {
            return new OfferNormalizer(stays, rooms);
        }
    }

    static class CatalogFixture {
        final java.util.Map<String, List<CatalogHotel>> catalogs = new HashMap<>();
    }

    private record FixtureSupplierClient(String supplier, CatalogFixture fixture) implements SupplierClient {
        @Override
        public CompletionStage<List<CatalogHotel>> catalog() {
            return CompletableFuture.completedFuture(fixture.catalogs.getOrDefault(supplier, List.of()));
        }

        @Override
        public CompletionStage<List<Offer>> search(List<String> codes, SearchCriteria criteria) {
            return CompletableFuture.completedFuture(List.of());
        }
    }
}
