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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MercadoPagoWebhookValidator webhookValidator;


    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    public PaymentController(PaymentService paymentService,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             MercadoPagoWebhookValidator webhookValidator) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.webhookValidator = webhookValidator;
    }

    // Crear pago
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.ok(toDTO(payment));
    }

    // Subir comprobante
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

    // Aprobar pago (vendedor/admin)
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> approvePayment(@PathVariable String id) {
        Payment payment = paymentService.approvePayment(id);
        return ResponseEntity.ok(toDTO(payment));
    }

    // Rechazar pago (vendedor/admin)
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<PaymentDTO> rejectPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String reason = body.get("reason");
        Payment payment = paymentService.rejectPayment(id, reason);
        return ResponseEntity.ok(toDTO(payment));
    }

    // Ver pagos pendientes (vendedor/admin)
    @GetMapping("/pending")
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
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> mercadoPagoWebhook(
            @RequestHeader(value = "x-signature", required = false) String signatureHeader,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody String requestBody) {

        logger.info("🔔 Webhook recibido de MercadoPago");
        logger.info("   Signature Header: {}", signatureHeader);
        logger.info("   Request Body: {}", requestBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, new TypeReference<>() {});
            String type = (String) payload.get("type");

            logger.info("   Tipo de evento: {}", type);

            if ("payment".equals(type)) {
                Map<String, Object> dataMap = (Map<String, Object>) payload.get("data");
                String paymentId = String.valueOf(dataMap.get("id"));

                logger.info("💬 Payment ID recibido de MP: {}", paymentId);

                // 🔎 Obtener detalles del pago desde Mercado Pago
                Map<String, Object> paymentData = paymentService.getMercadoPagoPaymentDetails(paymentId);
                if (paymentData == null) {
                    logger.warn("⚠️ No se pudieron obtener datos del pago ID={}", paymentId);
                    return ResponseEntity.ok().build();
                }

                // Extraer campos de forma segura
                String status = (String) paymentData.getOrDefault("status", "unknown");
                String externalRef = (String) paymentData.getOrDefault("external_reference", null);

                logger.info("✅ Datos MP -> status={}, external_reference={}", status, externalRef);

                if (externalRef != null) {
                    paymentService.processMercadoPagoWebhookExternalRef(externalRef, status);
                } else {
                    logger.warn("⚠️ No se encontró external_reference en el pago ID={}", paymentId);
                }
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("❌ Error procesando webhook MP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }



    // Mapper
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