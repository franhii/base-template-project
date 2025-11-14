package com.example.core.controller;

import com.example.core.dto.ShippingQuoteRequest;
import com.example.core.dto.ShippingQuoteResponse;
import com.example.core.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {

    private final ShippingService shippingService;

    /**
     * Cotizar envío solo con CP de DESTINO (público - para preview en carrito)
     * POST /api/shipping/quote-by-postalcode
     * Body: { "postalCode": "1234", "orderTotal": 1500.00 }
     */
    @PostMapping("/quote-by-postalcode")
    public ResponseEntity<ShippingQuoteResponse> quoteByPostalCode(
            @Valid @RequestBody ShippingQuoteRequest request
    ) {
        log.info("Cotizando envío a CP: {}", request.getPostalCode());
        ShippingQuoteResponse response = shippingService.quoteByPostalCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cotizar envío con Address completa (requiere auth - para checkout)
     * POST /api/shipping/quote
     * Body: { "addressId": "...", "orderTotal": 1500.00 }
     */
    @PostMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quoteShipping(
            @RequestParam String addressId,
            @RequestParam BigDecimal orderTotal
    ) {
        log.info("Cotizando envío para address: {}", addressId);
        ShippingQuoteResponse response = shippingService.quoteShipping(addressId, orderTotal);
        return ResponseEntity.ok(response);
    }

    /**
     * Validar disponibilidad de envío a una dirección (requiere auth)
     * GET /api/shipping/available/{addressId}
     */
    @GetMapping("/available/{addressId}")
    public ResponseEntity<Boolean> isShippingAvailable(@PathVariable String addressId) {
        boolean available = shippingService.isShippingAvailable(addressId);
        return ResponseEntity.ok(available);
    }
}