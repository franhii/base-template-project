package com.example.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para cotizar envío solo con Código Postal de DESTINO (preview en carrito)
 * El CP de origen se obtiene de la configuración del tenant
 */
@Data
public class ShippingQuoteRequest {

    @NotNull(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El código postal debe tener 4 dígitos")
    private String postalCode; // CP de DESTINO (cliente)

    @NotNull(message = "El total de la orden es obligatorio")
    @Positive(message = "El total debe ser mayor a 0")
    private BigDecimal orderTotal;

    // Opcional: dimensiones del paquete (si no se envían, usa valores default)
    // La mayoría de e-commerce NO pide esto al cliente
    private Integer width;  // cm
    private Integer height; // cm
    private Integer length; // cm
    private Integer weight; // gramos
}