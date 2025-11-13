package com.example.core.dto;

import com.mercadopago.resources.merchantorder.MerchantOrderShippingOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingOptionDTO {
    private Long shippingMethodId;
    private String name;
    private BigDecimal cost;
    private boolean isFree;
    private String estimatedDeliveryDate;
    private String estimatedDeliveryDays;
    private String speed; // "standard" o "express"

    // Método estático para convertir desde ShippingOption de ML
    public static ShippingOptionDTO fromMercadoLibre(MerchantOrderShippingOption option) {
        String deliveryDate = null;
        String deliveryDays = null;

        if (option.getEstimatedDelivery() != null) {
            OffsetDateTime delivery = option.getEstimatedDelivery().getDate();
            String from = option.getEstimatedDelivery().getTimeFrom();
            String to = option.getEstimatedDelivery().getTimeTo();

            // 📅 Fecha estimada de entrega
            if (delivery != null) {
                deliveryDate = delivery.toLocalDate().toString();
                // Calcula días desde hoy
                long daysBetween = ChronoUnit.DAYS.between(OffsetDateTime.now().toLocalDate(), delivery.toLocalDate());
                if (daysBetween >= 0) {
                    deliveryDays = daysBetween + (daysBetween == 1 ? " día" : " días");
                }
            } else if (from != null && to != null) {
                deliveryDays = "Entre " + from + " y " + to;
            }
        }

        return ShippingOptionDTO.builder()
                .shippingMethodId(option.getId())
                .name(option.getName())
                .cost(option.getCost())
                .isFree(option.getCost() != null && option.getCost().compareTo(BigDecimal.ZERO) == 0)
                .estimatedDeliveryDate(deliveryDate)
                .estimatedDeliveryDays(deliveryDays)
                .speed(option.getSpeed().getShipping().toString())
                .build();
    }
}
