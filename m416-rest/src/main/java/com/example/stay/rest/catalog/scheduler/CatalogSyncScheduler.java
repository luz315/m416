package com.example.stay.rest.catalog.scheduler;

import com.example.stay.catalog.application.usecase.SyncSupplierStaysUseCase;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.sync.enabled", havingValue = "true", matchIfMissing = true)
public class CatalogSyncScheduler implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CatalogSyncScheduler.class);
    private final SyncSupplierStaysUseCase sync;
    private final boolean syncOnStartup;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean started;

    public CatalogSyncScheduler(SyncSupplierStaysUseCase sync,
            @Value("${catalog.sync-on-startup:true}") boolean syncOnStartup) {
        this.sync = sync;
        this.syncOnStartup = syncOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (syncOnStartup) synchronize();
        started = true;
    }

    @Scheduled(fixedDelayString = "${catalog.sync.fixed-delay:PT6H}",
            initialDelayString = "${catalog.sync.initial-delay:PT6H}")
    public void scheduledSync() {
        if (started) synchronize();
    }

    private void synchronize() {
        if (!running.compareAndSet(false, true)) {
            log.warn("카탈로그 동기화가 이미 실행 중이므로 이번 실행을 건너뜁니다.");
            return;
        }
        long startedAt = System.nanoTime();
        try {
            var failures = sync.execute();
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (failures.isEmpty()) {
                log.info("카탈로그 동기화 완료. elapsedMs={}", elapsedMs);
            } else {
                log.warn("일부 공급사 카탈로그 동기화 실패. failures={}, elapsedMs={}", failures, elapsedMs);
            }
        } catch (RuntimeException error) {
            log.error("카탈로그 동기화 실행 중 내부 오류 발생", error);
        } finally {
            running.set(false);
        }
    }
}
