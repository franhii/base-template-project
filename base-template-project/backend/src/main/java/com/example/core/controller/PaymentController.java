package com.example.core.controller;

import com.example.core.dto.CreatePaymentRequest;
import com.example.core.dto.PaymentDTO;
import com.example.core.dto.UploadReceiptRequest;
import com.example.core.model.Payment;
import com.example.core.model.User;
import com.example.core.repository.PaymentRepository;
import com.example.core.repository.UserRepository;
import com.example.core.service.PaymentService;
import com.example.core.util.MercadoPagoWebhookValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MercadoPagoWebhookValidator webhookValidator;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    // URL del frontend (localhost para desarrollo, se cambiará en producción)
    private static final String FRONTEND_URL = "http://localhost:5173";

    public PaymentController(PaymentService paymentService,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             MercadoPagoWebhookValidator webhookValidator) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.webhookValidator = webhookValidator;
    }

    // ======================================================
    // ✅ CREAR PAGO
    // ======================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.ok(toDTO(payment));
    }

    // ======================================================
    // ✅ SUBIR COMPROBANTE (pagos manuales)
    // ======================================================
    @PostMapping("/upload-receipt")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> uploadReceipt(
            @Valid @RequestBody UploadReceiptRequest request) {

        Payment payment = paymentService.uploadReceipt(
                request.getPaymentId(),
                request.getReceiptUrl(),
                request.getNotes()
        );

        return ResponseEntity.ok(toDTO(payment));
    }

    // ======================================================
    // ✅ APROBAR PAGO (vendedor/admin)
    // ======================================================
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> approvePayment(@PathVariable String id) {
        Payment payment = paymentService.approvePayment(id);
        return ResponseEntity.ok(toDTO(payment));
    }

    // ======================================================
    // ✅ RECHAZAR PAGO (vendedor/admin)
    // ======================================================
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> rejectPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String reason = body.get("reason");
        Payment payment = paymentService.rejectPayment(id, reason);
        return ResponseEntity.ok(toDTO(payment));
    }

    // ======================================================
    // ✅ VER PAGOS PENDIENTES (vendedor/admin)
    // ======================================================
    @GetMapping("/pending-list")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getPendingPayments(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        List<Payment> payments = paymentRepository.findByTenantAndStatus(
                user.getTenant(),
                Payment.PaymentStatus.PENDING
        );

        List<PaymentDTO> dtos = payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ======================================================
    // 🔔 WEBHOOK DE MERCADOPAGO
    // ======================================================
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> mercadoPagoWebhook(
            @RequestHeader(value = "x-signature", required = false) String signatureHeader,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody String requestBody) {

        log.info("🔔 Webhook recibido de MercadoPago");
        log.info("   Signature Header: {}", signatureHeader);
        log.info("   Request ID: {}", requestId);
        log.info("   Request Body: {}", requestBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, new TypeReference<>() {});
            String type = (String) payload.get("type");

            log.info("   Tipo de evento: {}", type);

            if ("payment".equals(type)) {
                Map<String, Object> dataMap = (Map<String, Object>) payload.get("data");
                String paymentId = String.valueOf(dataMap.get("id"));

                log.info("💳 Payment ID recibido de MP: {}", paymentId);

                // 🔎 Obtener detalles completos del pago desde Mercado Pago
                Map<String, Object> paymentData = paymentService.getMercadoPagoPaymentDetails(paymentId);

                if (paymentData == null || paymentData.isEmpty()) {
                    log.warn("⚠️ No se pudieron obtener datos del pago ID={}", paymentId);
                    return ResponseEntity.ok().build();
                }

                // Extraer campos de forma segura
                String status = (String) paymentData.getOrDefault("status", "unknown");
                String externalRef = (String) paymentData.getOrDefault("external_reference", null);

                log.info("✅ Datos MP -> status={}, external_reference={}", status, externalRef);

                if (externalRef != null && !externalRef.isEmpty()) {
                    // Procesar el webhook usando el external_reference (Order ID)
                    paymentService.processMercadoPagoWebhookExternalRef(externalRef, status);
                    log.info("✅ Webhook procesado exitosamente para orderId={}", externalRef);
                } else {
                    log.warn("⚠️ No se encontró external_reference en el pago ID={}", paymentId);
                }
            } else {
                log.info("ℹ️ Tipo de evento ignorado: {}", type);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ Error procesando webhook MP: {}", e.getMessage(), e);
            return ResponseEntity.ok().build(); // Retornar 200 para que MP no reintente
        }
    }

    // ======================================================
    // 🔄 REDIRECTS PARA MERCADOPAGO (Success/Failure/Pending)
    // ======================================================

    /**
     * Endpoint al que MercadoPago redirige cuando el pago es exitoso.
     * Este endpoint redirige al frontend local del usuario.
     */
    @GetMapping("/success")
    public void handleSuccess(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) String collection_id,
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String payment_type,
            @RequestParam(required = false) String merchant_order_id,
            @RequestParam(required = false) String preference_id,
            @RequestParam(required = false) String site_id,
            @RequestParam(required = false) String processing_mode,
            @RequestParam(required = false) String merchant_account_id,
            HttpServletResponse response) throws IOException {

        log.info("✅ Pago exitoso recibido");
        log.info("   payment_id={}", payment_id);
        log.info("   external_reference={}", external_reference);
        log.info("   collection_status={}", collection_status);

        // Construir URL de redirección al frontend con todos los parámetros
        StringBuilder redirectUrl = new StringBuilder(FRONTEND_URL + "/success?");

        appendParam(redirectUrl, "payment_id", payment_id);
        appendParam(redirectUrl, "external_reference", external_reference);
        appendParam(redirectUrl, "collection_id", collection_id);
        appendParam(redirectUrl, "collection_status", collection_status);
        appendParam(redirectUrl, "payment_type", payment_type);
        appendParam(redirectUrl, "merchant_order_id", merchant_order_id);

        // Remover el último '&' si existe
        String finalUrl = redirectUrl.toString();
        if (finalUrl.endsWith("&")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }

        log.info("🔗 Redirigiendo a: {}", finalUrl);
        response.sendRedirect(finalUrl);
    }

    /**
     * Endpoint al que MercadoPago redirige cuando el pago falla.
     */
    @GetMapping("/failure")
    public void handleFailure(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) String collection_id,
            @RequestParam(required = false) String collection_status,
            HttpServletResponse response) throws IOException {

        log.warn("❌ Pago fallido");
        log.warn("   payment_id={}", payment_id);
        log.warn("   external_reference={}", external_reference);

        StringBuilder redirectUrl = new StringBuilder(FRONTEND_URL + "/failure?");

        appendParam(redirectUrl, "payment_id", payment_id);
        appendParam(redirectUrl, "external_reference", external_reference);
        appendParam(redirectUrl, "collection_id", collection_id);
        appendParam(redirectUrl, "collection_status", collection_status);

        String finalUrl = redirectUrl.toString();
        if (finalUrl.endsWith("&")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }

        log.info("🔗 Redirigiendo a: {}", finalUrl);
        response.sendRedirect(finalUrl);
    }

    /**
     * Endpoint al que MercadoPago redirige cuando el pago queda pendiente.
     */
    @GetMapping("/pending")
    public void handlePending(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) String collection_id,
            @RequestParam(required = false) String collection_status,
            HttpServletResponse response) throws IOException {

        log.info("⏳ Pago pendiente");
        log.info("   payment_id={}", payment_id);
        log.info("   external_reference={}", external_reference);

        StringBuilder redirectUrl = new StringBuilder(FRONTEND_URL + "/pending?");

        appendParam(redirectUrl, "payment_id", payment_id);
        appendParam(redirectUrl, "external_reference", external_reference);
        appendParam(redirectUrl, "collection_id", collection_id);
        appendParam(redirectUrl, "collection_status", collection_status);

        String finalUrl = redirectUrl.toString();
        if (finalUrl.endsWith("&")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }

        log.info("🔗 Redirigiendo a: {}", finalUrl);
        response.sendRedirect(finalUrl);
    }

    // ======================================================
    // 🛠️ HELPER METHODS
    // ======================================================

    /**
     * Helper para agregar parámetros a la URL solo si no son null
     */
    private void appendParam(StringBuilder url, String key, String value) {
        if (value != null && !value.isEmpty()) {
            url.append(key).append("=").append(value).append("&");
        }
    }

    /**
     * Convierte Payment a PaymentDTO
     */
    private PaymentDTO toDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder().getId());
        dto.setMethod(payment.getMethod().name());
        dto.setStatus(payment.getStatus().name());
        dto.setAmount(payment.getAmount());
        dto.setExternalId(payment.getExternalId());
        dto.setPaymentLink(payment.getPaymentLink());
        dto.setReceiptUrl(payment.getReceiptUrl());
        dto.setReceiptNotes(payment.getReceiptNotes());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setConfirmedAt(payment.getConfirmedAt());
        return dto;
    }
}