package com.example.stay.search.application.port.out;

import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.search.domain.model.Offer;
import com.example.stay.search.domain.model.SearchCriteria;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface SupplierClient {
    String supplier();
    CompletionStage<List<CatalogHotel>> catalog();
    CompletionStage<List<Offer>> search(List<String> hotelCodes, SearchCriteria criteria);
}
