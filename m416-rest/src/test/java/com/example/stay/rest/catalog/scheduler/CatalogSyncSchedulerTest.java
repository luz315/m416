package com.example.stay.rest.catalog.scheduler;

import com.example.stay.catalog.application.usecase.SyncSupplierStaysUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.*;

class CatalogSyncSchedulerTest {
    @Test
    @DisplayName("기동 전 주기 실행은 건너뛰고 기동 동기화 후 반복 실행한다")
    void startupAndScheduled() {
        var sync = mock(SyncSupplierStaysUseCase.class);
        when(sync.execute()).thenReturn(List.of());
        var scheduler = new CatalogSyncScheduler(sync, true);
        scheduler.scheduledSync();
        verifyNoInteractions(sync);
        scheduler.run(null);
        scheduler.scheduledSync();
        verify(sync, times(2)).execute();
    }

    @Test
    @DisplayName("기동 동기화를 끌 수 있고 실행 실패 후에도 다음 실행을 허용한다")
    void recoversAfterFailure() {
        var sync = mock(SyncSupplierStaysUseCase.class);
        when(sync.execute()).thenThrow(new IllegalStateException("실패")).thenReturn(List.of());
        var scheduler = new CatalogSyncScheduler(sync, false);
        scheduler.run(null);
        verifyNoInteractions(sync);
        scheduler.scheduledSync();
        scheduler.scheduledSync();
        verify(sync, times(2)).execute();
    }
}
