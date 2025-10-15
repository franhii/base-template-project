package com.example.core.service;

import com.example.core.dto.CreatePaymentRequest;
import com.example.core.model.Order;
import com.example.core.model.Payment;
import com.example.core.repository.OrderRepository;
import com.example.core.repository.PaymentRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RestClient restClient;


    @Value("${mercadopago.access-token}")
    private String mercadoPagoAccessToken;

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

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verificar que no exista ya un pago para esta orden
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("Payment already exists for this order");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTenant(order.getTenant());
        payment.setMethod(Payment.PaymentMethod.valueOf(request.getMethod()));
        payment.setAmount(order.getTotal());
        payment.setStatus(Payment.PaymentStatus.PENDING);

        // Si es MercadoPago, crear preferencia
        if (payment.getMethod() == Payment.PaymentMethod.MERCADO_PAGO) {
            try {
                String paymentLink = createMercadoPagoPreference(order);
                payment.setPaymentLink(paymentLink);
            } catch (Exception e) {
                throw new RuntimeException("Error creating MercadoPago preference: " + e.getMessage());
            }
        }

        return paymentRepository.save(payment);
    }

    private String createMercadoPagoPreference(Order order) throws Exception {
        // Configurar SDK de MercadoPago
        MercadoPagoConfig.setAccessToken(mercadoPagoAccessToken);

        // Crear items para la preferencia
        List<PreferenceItemRequest> items = new ArrayList<>();

        order.getItems().forEach(orderItem -> {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(orderItem.getItem().getId())
                    .title(orderItem.getItemName())
                    .description(orderItem.getItemType())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getPriceAtPurchase())
                    .build();
            items.add(item);
        });

        // Configurar URLs de retorno
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

        // Crear preferencia
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(order.getId()) // Referencia a tu orden
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        return preference.getInitPoint(); // URL de pago
    }

    @Transactional
    public Payment uploadReceipt(String paymentId, String receiptUrl, String notes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setReceiptUrl(receiptUrl);
        payment.setReceiptNotes(notes);
        payment.setStatus(Payment.PaymentStatus.PENDING); // Pendiente de revisión

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment approvePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.APPROVED);
        payment.setConfirmedAt(LocalDateTime.now());

        // Actualizar estado de la orden
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment rejectPayment(String paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.REJECTED);
        payment.setReceiptNotes(reason);

        // Opcional: restaurar stock si era producto
        // TODO: implementar lógica de restauración de stock

        return paymentRepository.save(payment);
    }

    // Procesar webhook de MercadoPago
    @Transactional
    public void processMercadoPagoWebhook(String paymentId, String status) {
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

    // 🔹 Consultar estado real en Mercado Pago
    public String getMercadoPagoPaymentStatus(String paymentId) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("https://api.mercadopago.com/v1/payments/{id}", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mercadoPagoAccessToken)
                    .retrieve()
                    .body(Map.class);

            if (body != null && body.get("status") != null) {
                return body.get("status").toString();
            }else{
                throw new RuntimeException("Payment not found");
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}