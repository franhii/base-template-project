package com.example.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends ItemDTO {
    private Integer stock;
    private String sku;
    private String type;
}