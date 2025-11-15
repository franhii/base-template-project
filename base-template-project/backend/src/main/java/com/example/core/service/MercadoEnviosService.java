package com.example.core.service;

import com.example.core.dto.*;
import com.example.core.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MercadoEnviosService {

    private static final String ML_API_BASE_URL = "https://api.mercadolibre.com";

    @Value("${mercadopago.access-token}")
    private String accessToken; // Mismo token de MercadoPago

    private final RestTemplate restTemplate;

    public MercadoEnviosService() {
        this.restTemplate = new RestTemplate();
    }

    // ========== COTIZAR ENVÍO ==========

    /**
     * Cotiza opciones de envío con MercadoEnvíos
     *
     * @param request Datos de la cotización
     * @return Lista de opciones de envío disponibles
     */
    public ShippingQuoteResponse calculateShipping(ShippingCalculationRequest request) {
        try {
            // Según documentación: https://developers.mercadolibre.com.ar/es_ar/envios
            // Endpoint para cotizar costos de envío
            String url = ML_API_BASE_URL + "/sites/MLA/shipping_costs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<ShippingCalculationRequest> entity = new HttpEntity<>(request, headers);

            log.info("Cotizando envío ML: {} → {}", request.getZipCodeFrom(), request.getZipCodeTo());
            log.info("URL: {}", url);
            log.info("Request body: {}", request);

            ResponseEntity<MercadoLibreShippingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MercadoLibreShippingResponse.class
            );

            if (response.getBody() == null || response.getBody().getOptions() == null) {
                log.warn("MercadoEnvíos retornó respuesta vacía");
                throw new BadRequestException("No hay opciones de envío disponibles");
            }

            List<ShippingOptionDTO> options = response.getBody().getOptions().stream()
                    .map(ShippingOptionDTO::fromMercadoLibre)
                    .collect(Collectors.toList());

            log.info("Opciones de envío obtenidas: {}", options.size());

            return ShippingQuoteResponse.builder()
                    .options(options)
                    .originPostalCode(request.getZipCodeFrom())
                    .destinationPostalCode(request.getZipCodeTo())
                    .build();

        } catch (RestClientException e) {
            log.error("Error al cotizar envío con MercadoEnvíos", e);
            throw new BadRequestException("Error al calcular envío: " + e.getMessage());
        }
    }

    // ========== BUSCAR OPCIÓN POR ID ==========

    /**
     * Busca una opción de envío específica por su ID
     */
    public ShippingOptionDTO findShippingOption(
            ShippingCalculationRequest request,
            Long shippingMethodId) {

        ShippingQuoteResponse quote = calculateShipping(request);

        return quote.getOptions().stream()
                .filter(opt -> opt.getShippingMethodId().equals(shippingMethodId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Método de envío no válido: " + shippingMethodId));
    }

    // ========== VALIDAR ENVÍO ==========

    /**
     * Valida que haya opciones de envío disponibles para los CPs dados
     */
    public boolean isShippingAvailable(String zipCodeFrom, String zipCodeTo) {
        try {
            ShippingCalculationRequest request = ShippingCalculationRequest.builder()
                    .zipCodeFrom(zipCodeFrom)
                    .zipCodeTo(zipCodeTo)
                    .dimensions("10x10x10,100") // Dimensiones mínimas
                    .listCost(BigDecimal.valueOf(1000))
                    .freeShipping(false)
                    .build();

            ShippingQuoteResponse quote = calculateShipping(request);
            return quote.getOptions() != null && !quote.getOptions().isEmpty();

        } catch (Exception e) {
            log.error("Error validando disponibilidad de envío", e);
            return false;
        }
    }

    // ========== CREAR ENVÍO (FUTURO) ==========

    /**
     * Crea un envío en MercadoEnvíos después de confirmar el pago
     * TODO: Implementar cuando se necesite tracking automático
     */
    public String createShipment(String orderId, Long shippingMethodId) {
        // Implementar cuando sea necesario
        // POST /shipments con datos de la orden
        log.info("Crear shipment para orden {} con método {}", orderId, shippingMethodId);
        return null;
    }

    // ========== TRACKING (FUTURO) ==========

    /**
     * Obtiene información de tracking de un envío
     * TODO: Implementar cuando se necesite seguimiento
     */
    public Object getShipmentTracking(String shipmentId) {
        // GET /shipments/{shipmentId}
        log.info("Obtener tracking de shipment {}", shipmentId);
        return null;
    }

    // ========== DEBUG: INVESTIGAR CPs VÁLIDOS ==========

    /**
     * Investigar métodos de shipping disponibles y códigos postales válidos
     * Usa el endpoint correcto de la documentación oficial
     */
    public void investigateValidPostalCodes() {
        String[] knownPostalCodes = {
            "1000", // CABA Centro
            "1001", // CABA Monserrat  
            "1426", // CABA Belgrano
            "2600", // Venado Tuerto, Santa Fe
            "7600", // Mar del Plata, Buenos Aires
            "5000", // Córdoba Capital
            "4000", // San Miguel de Tucumán
            "3000", // Santa Fe Capital
            "8300", // Neuquén Capital
            "9120", // Puerto Madryn, Chubut
            "1900", // La Plata, Buenos Aires
            "2000", // Rosario, Santa Fe
            "5500", // Mendoza Capital
            "4400", // Salta Capital
        };

        String baseZip = "1000"; // CABA como origen base

        log.info("=== INVESTIGANDO MÉTODOS DE SHIPPING EN MERCADOENVÍOS ===");
        log.info("🔑 Token configurado: {}", accessToken != null ? "✅ SÍ" : "❌ NO");
        log.info("🔑 Token length: {}", accessToken != null ? accessToken.length() : 0);
        
        // Primero obtenemos los métodos de shipping disponibles
        String methodsUrl = ML_API_BASE_URL + "/sites/MLA/shipping_methods";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            log.info("🔍 Consultando métodos de shipping disponibles...");
            ResponseEntity<String> methodsResponse = restTemplate.exchange(
                methodsUrl,
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            log.info("📦 Métodos de shipping disponibles: {}", methodsResponse.getBody());
            
        } catch (Exception e) {
            log.error("❌ Error al consultar métodos de shipping", e);
        }

        for (String postalCode : knownPostalCodes) {
            try {
                boolean available = isShippingAvailable(baseZip, postalCode);
                log.info("CP {} - {}", postalCode, available ? "✅ VÁLIDO" : "❌ INVÁLIDO");
                
                // Pausa para no saturar la API
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.error("Error probando CP {}: {}", postalCode, e.getMessage());
            }
        }
        
        log.info("=== FIN INVESTIGACIÓN CPs ===");
    }
}