package com.example.core.model;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TenantConfig {
    // Configuración visual
    private String logo;
    private String primaryColor;
    private String secondaryColor;

    // Categorías del negocio
    private List<String> categories;

    // Features principales (tipadas)
    private Features features;

    // Configuración custom/extendida (para casos especiales)
    private Map<String, Object> customConfig;

    @Data
    public static class Features {
        // Core features (todos los negocios)
        private boolean products = false;
        private boolean services = false;

        // E-commerce features
        private boolean cart = true;
        private boolean checkout = true;

        // Delivery/Shipping
        private boolean delivery = false;
        private DeliveryConfig deliveryConfig;

        // Booking/Reservas
        private boolean booking = false;
        private BookingConfig bookingConfig;

        // Payments
        private boolean mercadoPago = true;
        private boolean cashPayment = true;
        private boolean bankTransfer = true;

        // Recurring (suscripciones)
        private boolean recurringPayments = false;

        // Multi-vendor (marketplace)
        private boolean multiVendor = false;

        // Landing page builder
        private boolean landingPages = false;

        // WhatsApp integration
        private boolean whatsappIntegration = true;
        private String whatsappNumber;
    }

    @Data
    public static class DeliveryConfig {
        private Double freeDeliveryThreshold; // Envío gratis desde $X
        private Double deliveryCost;
        private Integer maxDeliveryRadius; // Radio en km
    }

    @Data
    public static class BookingConfig {
        private Integer slotDurationMinutes; //
        private String workingHoursStart; //
        private String workingHoursEnd;   //
        private List<String> workingDays; // ["LUNES", "MARTES", ...]
    }
}