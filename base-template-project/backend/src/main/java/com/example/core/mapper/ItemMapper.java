package com.example.core.mapper;

import com.example.core.dto.ProductDTO;
import com.example.core.dto.ServiceDTO;
import com.example.core.model.Product;
import com.example.core.model.ServiceItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "itemType", constant = "PRODUCT")
    @Mapping(target = "type", source = "type")
    ProductDTO toProductDTO(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "weight", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    @Mapping(target = "tenant", ignore = true)
    Product fromProductDTO(ProductDTO dto);

    @Mapping(target = "itemType", constant = "SERVICE")
    @Mapping(target = "scheduleType", source = "scheduleType")
    @Mapping(target = "availableDays", source = "availableDays")
    @Mapping(target = "workStartTime", source = "workStartTime")
    @Mapping(target = "workEndTime", source = "workEndTime")
    @Mapping(target = "slotIntervalMinutes", source = "slotIntervalMinutes")
    ServiceDTO toServiceDTO(ServiceItem service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    @Mapping(target = "tenant", ignore = true)
    // ✅ IMPORTANTE: Mapear los campos de booking
    @Mapping(target = "availableDays", source = "availableDays")
    @Mapping(target = "workStartTime", source = "workStartTime")
    @Mapping(target = "workEndTime", source = "workEndTime")
    @Mapping(target = "slotIntervalMinutes", source = "slotIntervalMinutes")
    ServiceItem fromServiceDTO(ServiceDTO dto);
}