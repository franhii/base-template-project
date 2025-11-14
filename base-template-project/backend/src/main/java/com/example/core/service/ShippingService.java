package com.example.core.service;

import com.example.core.dto.ShippingQuoteRequest;
import com.example.core.dto.ShippingQuoteResponse;
import com.example.core.dto.ShippingOptionDTO;
import com.example.core.model.Address;
import com.example.core.model.Tenant;
import com.example.core.exception.BadRequestException;
import com.example.core.repository.AddressRepository;
import com.example.core.repository.TenantRepository;
import com.example.core.context.TenantContext;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.resources.merchantorder.MerchantOrderShippingOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final AddressRepository addressRepository;
    private final TenantRepository tenantRepository;

    @Value("${mercadopago.access-token}")
    private String mercadoPagoAccessToken;

    // Dimensiones y peso por defecto (paquete estándar e-commerce)
    private static final int DEFAULT_WIDTH = 30;   // cm
    private static final int DEFAULT_HEIGHT = 20;  // cm
    private static final int DEFAULT_LENGTH = 10;  // cm
    private static final int DEFAULT_WEIGHT = 1000; // 1kg en gramos

    /**
     * Cotiza opciones de envío solo con CP de DESTINO (preview en carrito)
     * El CP de origen se obtiene de la config del tenant actual
     */
    public ShippingQuoteResponse quoteByPostalCode(ShippingQuoteRequest request) {
        // Obtener tenant actual desde context
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BadRequestException("No se pudo identificar el tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BadRequestException("Tenant no encontrado"));

        // Obtener CP de origen desde las features/config del tenant
        String originPostalCode = tenant.getConfig().getPostalCode();

        if (originPostalCode == null || originPostalCode.isEmpty()) {
            throw new BadRequestException("El negocio no tiene código postal configurado. Contacta al administrador.");
        }

        log.info("Cotizando envío desde CP {} (negocio: {}) a CP {} (cliente)",
                originPostalCode, tenant.getBusinessName(), request.getPostalCode());

        return calculateShipping(originPostalCode, request.getPostalCode(), request.getOrderTotal(), request);
    }

    /**
     * Cotiza opciones de envío con Address completa (checkout)
     */
    public ShippingQuoteResponse quoteShipping(String addressId, BigDecimal orderTotal) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BadRequestException("Dirección no encontrada"));

        // Validar que la dirección sea del tenant actual
        String currentTenantId = TenantContext.getCurrentTenant();
        if (!address.getTenant().getId().equals(currentTenantId)) {
            throw new BadRequestException("Dirección no válida");
        }

        Tenant tenant = tenantRepository.findById(currentTenantId)
                .orElseThrow(() -> new BadRequestException("Tenant no encontrado"));

        String originPostalCode = tenant.getConfig().getPostalCode();
        if (originPostalCode == null || originPostalCode.isEmpty()) {
            throw new BadRequestException("El negocio no tiene código postal configurado");
        }

        log.info("Cotizando envío desde {} a {} para address {}",
                originPostalCode, address.getPostalCode(), addressId);

        return calculateShipping(originPostalCode, address.getPostalCode(), orderTotal, null);
    }

    /**
     * Valida si hay envío disponible a una dirección
     */
    public boolean isShippingAvailable(String addressId) {
        try {
            ShippingQuoteResponse response = quoteShipping(addressId, BigDecimal.valueOf(100));
            return response != null && !response.getOptions().isEmpty();
        } catch (Exception e) {
            log.warn("Envío no disponible para address {}: {}", addressId, e.getMessage());
            return false;
        }
    }

    /**
     * Método privado que hace el cálculo real con MercadoEnvíos usando el SDK
     */
    private ShippingQuoteResponse calculateShipping(String fromPostalCode,
                                                    String toPostalCode,
                                                    BigDecimal orderTotal,
                                                    ShippingQuoteRequest request) {
        try {
            // Configurar el SDK de MercadoPago
            MercadoPagoConfig.setAccessToken(mercadoPagoAccessToken);

            // Usar dimensiones del request o defaults
            int width = (request != null && request.getWidth() != null) ? request.getWidth() : DEFAULT_WIDTH;
            int height = (request != null && request.getHeight() != null) ? request.getHeight() : DEFAULT_HEIGHT;
            int length = (request != null && request.getLength() != null) ? request.getLength() : DEFAULT_LENGTH;
            int weight = (request != null && request.getWeight() != null) ? request.getWeight() : DEFAULT_WEIGHT;

            // Crear el cliente de Shipment de MercadoEnvíos
            //ShipmentClient client = new ShipmentClient();

            // Construir el request para cotizar
            Map<String, Object> shippingRequest = new HashMap<>();

            // Origen
            Map<String, String> from = new HashMap<>();
            from.put("zip_code", fromPostalCode);
            shippingRequest.put("from", from);

            // Destino
            Map<String, String> to = new HashMap<>();
            to.put("zip_code", toPostalCode);
            shippingRequest.put("to", to);

            // Dimensiones
            String dimensions = String.format("%dx%dx%d,%d", width, height, length, weight);
            shippingRequest.put("dimensions", dimensions);

            // Precio del item
            shippingRequest.put("item_price", orderTotal.doubleValue());

            log.debug("Request a MercadoEnvíos: {}", shippingRequest);

            // Llamar a la API para obtener opciones de envío
            // Nota: El SDK de ML no tiene método directo para /shipments/options
            // Usamos MerchantOrderClient como alternativa
            MerchantOrderClient merchantOrderClient = new MerchantOrderClient();

            // TODO: Implementar llamada correcta según documentación de ML
            // Por ahora, construir respuesta con opciones default

            List<ShippingOptionDTO> options = new ArrayList<>();

            // TEMPORAL: Crear opciones de ejemplo
            // En producción, parsear la respuesta real del SDK
            options.add(ShippingOptionDTO.builder()
                    .shippingMethodId(100009L)
                    .name("Estándar")
                    .cost(calculateEstimatedCost(fromPostalCode, toPostalCode, "standard"))
                    .isFree(false)
                    .estimatedDeliveryDays("5-7 días hábiles")
                    .speed("standard")
                    .build());

            options.add(ShippingOptionDTO.builder()
                    .shippingMethodId(100012L)
                    .name("Express")
                    .cost(calculateEstimatedCost(fromPostalCode, toPostalCode, "express"))
                    .isFree(false)
                    .estimatedDeliveryDays("2-3 días hábiles")
                    .speed("express")
                    .build());

            return ShippingQuoteResponse.builder()
                    .options(options)
                    .originPostalCode(fromPostalCode)
                    .destinationPostalCode(toPostalCode)
                    .build();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cotizar envío", e);
            throw new BadRequestException("Error al calcular opciones de envío: " + e.getMessage());
        }
    }

    /**
     * Calcula un costo estimado basado en la distancia entre CPs
     * (Simplificación para desarrollo - en producción usar respuesta real de ML)
     */
    private BigDecimal calculateEstimatedCost(String fromCP, String toCP, String speed) {
        // Lógica simplificada: basarse en diferencia de CPs
        try {
            int from = Integer.parseInt(fromCP);
            int to = Integer.parseInt(toCP);
            int distance = Math.abs(from - to);

            // Costo base
            double baseCost = 1000.0;

            // Incremento por distancia (aprox $10 cada 100 unidades de CP)
            double distanceCost = (distance / 100.0) * 10.0;

            // Multiplicador por velocidad
            double speedMultiplier = "express".equals(speed) ? 1.8 : 1.0;

            double totalCost = (baseCost + distanceCost) * speedMultiplier;

            return BigDecimal.valueOf(Math.round(totalCost * 100.0) / 100.0);

        } catch (Exception e) {
            return BigDecimal.valueOf("express".equals(speed) ? 2500.0 : 1500.0);
        }
    }
}