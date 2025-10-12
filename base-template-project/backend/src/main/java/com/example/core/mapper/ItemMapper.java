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
    ServiceDTO toServiceDTO(ServiceItem service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    @Mapping(target = "tenant", ignore = true)
    ServiceItem fromServiceDTO(ServiceDTO dto);
}