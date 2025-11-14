package com.example.core.model;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TenantConfig {

    // ========== Branding Visual ==========
    private String logo;                    // URL del logo
    private String favicon;                 // URL del favicon
    private String primaryColor;            // Color principal (#667eea)
    private String secondaryColor;          // Color secundario (#764ba2)
    private String accentColor;             // Color de acento (#10b981)
    private String fontFamily;              // "Roboto", "Montserrat", etc.

    // ========== Personalización Avanzada ==========
    private String customCssUrl;            // URL a CSS custom (opcional)
    private String headerLayout;            // "horizontal", "vertical", "minimal"
    private String footerContent;           // HTML custom para footer

    // ========== Información del Negocio ==========
    private String businessDescription;     // Descripción para SEO
    private String contactEmail;            // Email de contacto
    private String contactPhone;            // Teléfono de contacto
    private String address;                 // Dirección física
    private SocialMedia socialMedia;        // Redes sociales

    // ========== Categorías del negocio ==========
    private List<String> categories;

    // ========== Features principales ==========
    private Features features;

    // ========== Configuración custom/extendida ==========
    private Map<String, Object> customConfig;

    // ========== Traducciones custom (i18n) ==========
    private Map<String, String> translations;


    // Se agrega para saber el costo (deesde donde esta el local hasta el destino del cliente)
    private String postalCode;

    // Getter y Setter
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    // ========== Clase para Redes Sociales ==========
    @Data
    public static class SocialMedia {
        private String facebook;
        private String instagram;
        private String twitter;
        private String linkedin;
        private String youtube;
        private String tiktok;
    }

    // ========== Features ==========
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

        // Newsletter
        private boolean newsletter = false;

        // Reviews/Ratings
        private boolean reviews = false;

        // Blog
        private boolean blog = false;
    }

    @Data
    public static class DeliveryConfig {
        private Double freeDeliveryThreshold;    // Envío gratis desde $X
        private Double deliveryCost;              // Costo fijo de envío
        private Integer maxDeliveryRadius;        // Radio en km
        private List<String> deliveryZones;       // Zonas de reparto
    }

    @Data
    public static class BookingConfig {
        private Integer slotDurationMinutes;      // Duración de cada slot
        private String workingHoursStart;         // "09:00"
        private String workingHoursEnd;           // "18:00"
        private List<String> workingDays;         // ["LUNES", "MARTES", ...]
        private Integer maxBookingsPerDay;        // Límite diario
    }
}