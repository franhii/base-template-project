package com.example.core.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailySalesDTO {
    private LocalDate date;
    private BigDecimal total;
    private Integer orderCount;
}