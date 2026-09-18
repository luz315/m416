package com.example.stay.catalog.application.service;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.common.exception.InvalidCatalogException;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Named
@RequiredArgsConstructor
public class CatalogMappingWriter {
    private final StayMappingRepository stays;
    private final RoomMappingRepository rooms;
    private final CatalogStateRepository states;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void write(String supplier, List<CatalogHotel> hotels) {
        List<Long> stayIds = stays.findAllBySupplier(supplier).stream().map(StayMapping::getId).toList();
        rooms.findAllByStayIds(stayIds).forEach(mapping -> {
            mapping.deactivate();
            rooms.save(mapping);
        });
        stays.findAllBySupplier(supplier).forEach(mapping -> {
            mapping.deactivate();
            stays.save(mapping);
        });

        Set<String> stayCodes = new HashSet<>();
        for (CatalogHotel hotel : hotels) {
            if (!stayCodes.add(hotel.stayCode())) {
                throw new InvalidCatalogException("중복 숙소 코드");
            }
            StayMapping stay = stays.findBySupplierAndStayCode(supplier, hotel.stayCode())
                    .map(existing -> {
                        existing.activate();
                        return stays.save(existing);
                    })
                    .orElseGet(() -> stays.save(StayMapping.create(supplier, hotel.stayCode())));

            Set<String> roomCodes = new HashSet<>();
            for (CatalogHotel.CatalogRoom room : hotel.rooms()) {
                if (!roomCodes.add(room.code())) {
                    throw new InvalidCatalogException("중복 객실 코드");
                }
                rooms.findByStayIdAndRoomCode(stay.getId(), room.code()).ifPresentOrElse(existing -> {
                    existing.activate();
                    rooms.save(existing);
                }, () -> rooms.save(RoomMapping.create(stay.getId(), room.code())));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        CatalogState state = states.findBySupplier(supplier).orElseGet(() -> CatalogState.create(supplier, now));
        state.updateSyncedAt(now);
        states.save(state);
    }
}
