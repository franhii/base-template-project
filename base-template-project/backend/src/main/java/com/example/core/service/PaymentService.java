package com.example.core.service;

import com.example.core.dto.CreatePaymentRequest;
import com.example.core.model.Item;
import com.example.core.model.Order;
import com.example.core.model.OrderItem;
import com.example.core.model.Payment;
import com.example.core.repository.OrderRepository;
import com.example.core.repository.PaymentRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RestClient restClient;

    @Value("${mercadopago.access-token}")
    private String mercadoPagoAccessToken;

    @Value("${mercadopago.webhook-url}")
    private String webhookUrl;

    @Value("${mercadopago.success-url}")
    private String successUrl;

    @Value("${mercadopago.failure-url}")
    private String failureUrl;

    @Value("${mercadopago.pending-url}")
    private String pendingUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          RestClient.Builder builder) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.restClient = builder.build();
    }

    // ======================================================
    // ✅ CREACIÓN DE PAGO (MP o Manual)
    // ======================================================
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        logger.info("🔄 Creando pago para orden: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("Payment already exists for this order");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTenant(order.getTenant());
        payment.setMethod(Payment.PaymentMethod.valueOf(request.getMethod()));
        payment.setAmount(order.getTotal());
        payment.setStatus(Payment.PaymentStatus.PENDING);

        // 🔹 Si es MercadoPago, crear preferencia
        if (payment.getMethod() == Payment.PaymentMethod.MERCADO_PAGO) {
            try {
                logger.info("<ANTES DE createMercadoPagoPreference> Mercado Pago");
                Map<String, String> mpResult = createMercadoPagoPreference(order);
                payment.setExternalId(mpResult.get("preferenceId"));
                payment.setPaymentLink(mpResult.get("initPoint"));
                payment.setExternalStatus("pending");
            } catch (Exception e) {
                logger.error("❌ Error creando preferencia MP: {}", e.getMessage(), e);
                throw new RuntimeException("Error creating MercadoPago preference: " + e.getMessage());
            }
        }

        return paymentRepository.save(payment);
    }

    // ======================================================
    // ✅ SUBIR COMPROBANTE (pagos manuales)
    // ======================================================
    @Transactional
    public Payment uploadReceipt(String paymentId, String receiptUrl, String notes) {
        logger.info("📄 Subiendo comprobante para pago: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setReceiptUrl(receiptUrl);
        payment.setReceiptNotes(notes);
        payment.setStatus(Payment.PaymentStatus.PENDING); // Pendiente de revisión

        logger.info("✅ Comprobante guardado. URL: {}", receiptUrl);
        return paymentRepository.save(payment);
    }

    // ======================================================
    // ✅ APROBAR PAGO (manual)
    // ======================================================
    @Transactional
    public Payment approvePayment(String paymentId) {
        logger.info("✅ Aprobando pago manual: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.APPROVED);
        payment.setConfirmedAt(LocalDateTime.now());

        if (payment.getOrder() != null) {
            payment.getOrder().setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(payment.getOrder());
        }

        logger.info("💰 Pago aprobado y orden confirmada");
        return paymentRepository.save(payment);
    }

    // ======================================================
    // ✅ RECHAZAR PAGO
    // ======================================================
    @Transactional
    public Payment rejectPayment(String paymentId, String reason) {
        logger.info("❌ Rechazando pago: {} - Razón: {}", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.REJECTED);
        payment.setReceiptNotes(reason);

        return paymentRepository.save(payment);
    }

    // ======================================================
    // ✅ MERCADO PAGO: Crear preferencia
    // ======================================================
    private Map<String, String> createMercadoPagoPreference(Order order) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(mercadoPagoAccessToken);
        List<PreferenceItemRequest> itemsMP = order.getItems().stream()
                .map(orderItem -> PreferenceItemRequest.builder()
                        .id(orderItem.getId()) // tu id interno del producto
                        .title(orderItem.getItemName() + " x " + orderItem.getQuantity()+" $"+orderItem.getItem().getPrice())
                        .quantity(orderItem.getQuantity())
                        .unitPrice(orderItem.getPriceAtPurchase()) // precio unitario
                        .currencyId("ARS")
                        .pictureUrl(orderItem.getItem().getImageUrl()) // 🔹 agrega esto si tu OrderItem tiene campo de imagen
                        .description("Cantidad: " + orderItem.getQuantity() + " - Subtotal: $" + orderItem.getOrder().getTotal())
                        .build())
                .toList();


        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(itemsMP)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(String.valueOf(order.getId()))
                .notificationUrl(webhookUrl)
                .build();

        Preference preference = new PreferenceClient().create(preferenceRequest);

        Map<String, String> result = new HashMap<>();
        result.put("preferenceId", preference.getId());
        result.put("initPoint", preference.getInitPoint());
        return result;
    }



    // ======================================================
    // ✅ MERCADO PAGO: Procesar Webhook
    // ======================================================
    @Transactional
    public void processMercadoPagoWebhook(String paymentId, String status) {
        logger.info("🔔 Webhook MP recibido: paymentId={}, status={}", paymentId, status);

        Payment payment = paymentRepository.findByExternalId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setExternalStatus(status);

        switch (status) {
            case "approved":
                payment.setStatus(Payment.PaymentStatus.APPROVED);
                payment.setConfirmedAt(LocalDateTime.now());
                payment.getOrder().setStatus(Order.OrderStatus.CONFIRMED);
                break;
            case "rejected":
            case "cancelled":
                payment.setStatus(Payment.PaymentStatus.REJECTED);
                break;
            case "in_process":
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                break;
            default:
                payment.setStatus(Payment.PaymentStatus.PENDING);
        }

        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
    }

    public Map<String, Object> getMercadoPagoPaymentDetails(String paymentId) {
        logger.info("🔍 Consultando detalles del pago en Mercado Pago: {}", paymentId);

        try {
            // 🔗 Endpoint oficial de MercadoPago
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;

            // 🧾 Realizar la solicitud con el Access Token
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mercadoPagoAccessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.isEmpty()) {
                logger.warn("⚠️ Respuesta vacía desde Mercado Pago para paymentId={}", paymentId);
                return Map.of();
            }

            // 🔍 Log opcional para debugging
            logger.info("✅ Detalles de pago obtenidos: id={}, status={}, external_reference={}",
                    response.get("id"), response.get("status"), response.get("external_reference"));

            return response;

        } catch (Exception e) {
            logger.error("❌ Error al consultar detalles de pago MP: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }


    @Transactional
    public void processMercadoPagoWebhookExternalRef(String externalReference, String status) {
        logger.info("🔔 Procesando webhook MP por external_reference={} status={}", externalReference, status);

        // 1️⃣ Buscar la orden local por externalReference
        Order order = orderRepository.findById(externalReference)
                .orElseThrow(() -> new RuntimeException("Order not found by external_reference: " + externalReference));

        // 2️⃣ Buscar el pago local asociado a la orden
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + order.getId()));

        // 3️⃣ Evitar reprocesar un estado idéntico (MP puede mandar el mismo webhook varias veces)
        if (payment.getExternalStatus() != null && payment.getExternalStatus().equalsIgnoreCase(status)) {
            logger.warn("⚠️ Webhook duplicado ignorado para orderId={}, status={}", order.getId(), status);
            return;
        }

        // 4️⃣ Actualizar el estado recibido
        payment.setExternalStatus(status);

        switch (status) {
            case "approved":
                payment.setStatus(Payment.PaymentStatus.APPROVED);
                payment.setConfirmedAt(LocalDateTime.now());
                order.setStatus(Order.OrderStatus.CONFIRMED);
                break;

            case "rejected":
            case "cancelled":
                payment.setStatus(Payment.PaymentStatus.REJECTED);
                order.setStatus(Order.OrderStatus.CANCELLED); // opcional, si querés mantenerlo sincronizado
                break;

            case "in_process":
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                break;

            default:
                logger.warn("⚠️ Estado desconocido recibido desde MP: {}", status);
                payment.setStatus(Payment.PaymentStatus.PENDING);
        }

        // 5️⃣ Guardar cambios
        paymentRepository.save(payment);
        orderRepository.save(order);

        logger.info("✅ Webhook procesado correctamente: paymentId={}, orderId={}, nuevoStatus={}",
                payment.getId(), order.getId(), payment.getStatus());
    }




}
