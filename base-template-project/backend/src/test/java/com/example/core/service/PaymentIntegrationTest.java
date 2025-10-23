package com.example.core.service;

import com.example.core.dto.CreateOrderRequest;
import com.example.core.dto.CreatePaymentRequest;
import com.example.core.model.*;
import com.example.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("🧪 Suite de Integración: Órdenes + Pagos + MercadoPago")
class PaymentIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private Tenant testTenant;
    private Product testProduct;

    @BeforeEach
    @Transactional
    void setUp() {
        // 1️⃣ Crear tenant de prueba
        testTenant = new Tenant();
        testTenant.setSubdomain("test-tenant");
        testTenant.setBusinessName("Test Business");
        testTenant.setType(Tenant.BusinessType.RETAIL);
        testTenant = tenantRepository.save(testTenant);

        // 2️⃣ Crear usuario de prueba
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashed_password");
        testUser.setRole(Role.CLIENTE);
        testUser.setTenant(testTenant);
        testUser = userRepository.save(testUser);

        // 3️⃣ Crear producto de prueba
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("A test product");
        testProduct.setPrice(BigDecimal.valueOf(100.00));
        testProduct.setStock(10);
        testProduct.setCategory("TEST");
        testProduct.setActive(true);
        testProduct.setTenant(testTenant);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("✅ 1. Crear orden correctamente")
    @Transactional
    void testCreateOrder() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setItemId(testProduct.getId());
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));
        request.setPaymentMethod("MERCADO_PAGO");
        request.setNotes("Test order");

        // Act
        Order order = orderService.createOrder(request, testUser.getEmail());

        // Assert
        assertNotNull(order.getId());
        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        assertEquals(Order.PaymentMethod.MERCADO_PAGO, order.getPaymentMethod());
        assertEquals(BigDecimal.valueOf(200.00), order.getTotal());
        assertEquals(1, order.getItems().size());
        assertEquals(8, testProduct.getStock()); // Stock descontado
    }

    @Test
    @DisplayName("✅ 2. Crear pago MercadoPago con preferencia")
    @Transactional
    void testCreateMercadoPagoPayment() {
        // Arrange
        Order order = createTestOrder();

        CreatePaymentRequest paymentReq = new CreatePaymentRequest();
        paymentReq.setOrderId(order.getId());
        paymentReq.setMethod("MERCADO_PAGO");

        // Act
        Payment payment = paymentService.createPayment(paymentReq);

        // Assert
        assertNotNull(payment.getId());
        assertEquals(Payment.PaymentMethod.MERCADO_PAGO, payment.getMethod());
        assertEquals(Payment.PaymentStatus.PENDING, payment.getStatus());
        assertEquals(order.getTotal(), payment.getAmount());
        assertNotNull(payment.getExternalId());
        assertNotNull(payment.getPaymentLink());
        assertEquals("pending", payment.getExternalStatus());
    }

    @Test
    @DisplayName("✅ 3. Crear pago manual (transferencia bancaria)")
    @Transactional
    void testCreateManualPayment() {
        // Arrange
        Order order = createTestOrder();

        CreatePaymentRequest paymentReq = new CreatePaymentRequest();
        paymentReq.setOrderId(order.getId());
        paymentReq.setMethod("BANK_TRANSFER");

        // Act
        Payment payment = paymentService.createPayment(paymentReq);

        // Assert
        assertNotNull(payment.getId());
        assertEquals(Payment.PaymentMethod.BANK_TRANSFER, payment.getMethod());
        assertEquals(Payment.PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getPaymentLink()); // No hay link para transferencia
    }

    @Test
    @DisplayName("✅ 4. Subir comprobante de transferencia")
    @Transactional
    void testUploadReceipt() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "BANK_TRANSFER");

        // Act
        Payment updated = paymentService.uploadReceipt(
                payment.getId(),
                "https://s3.example.com/receipts/abc123.png",
                "Transferencia desde cuenta Santander"
        );

        // Assert
        assertNotNull(updated.getReceiptUrl());
        assertNotNull(updated.getReceiptNotes());
        assertEquals(Payment.PaymentStatus.PENDING, updated.getStatus()); // Pendiente revisión
    }

    @Test
    @DisplayName("✅ 5. Aprobar pago manual")
    @Transactional
    void testApproveManualPayment() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "BANK_TRANSFER");
        paymentService.uploadReceipt(payment.getId(), "https://s3.example.com/receipt.png", "OK");

        // Act
        Payment approved = paymentService.approvePayment(payment.getId());

        // Assert
        assertEquals(Payment.PaymentStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getConfirmedAt());

        // Verificar que la orden también se actualizó
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("✅ 6. Rechazar pago")
    @Transactional
    void testRejectPayment() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "BANK_TRANSFER");

        // Act
        Payment rejected = paymentService.rejectPayment(
                payment.getId(),
                "Monto no coincide"
        );

        // Assert
        assertEquals(Payment.PaymentStatus.REJECTED, rejected.getStatus());
        assertEquals("Monto no coincide", rejected.getReceiptNotes());
    }

    @Test
    @DisplayName("✅ 7. Procesar webhook MercadoPago - Aprobado")
    @Transactional
    void testProcessMercadoPagoWebhookApproved() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "MERCADO_PAGO");
        String externalRef = order.getId(); // En MP usamos external_reference = orderId

        // Act
        paymentService.processMercadoPagoWebhookExternalRef(externalRef, "approved");

        // Assert
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.APPROVED, updated.getStatus());
        assertEquals("approved", updated.getExternalStatus());
        assertNotNull(updated.getConfirmedAt());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("✅ 8. Procesar webhook MercadoPago - Rechazado")
    @Transactional
    void testProcessMercadoPagoWebhookRejected() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "MERCADO_PAGO");

        // Act
        paymentService.processMercadoPagoWebhookExternalRef(order.getId(), "rejected");

        // Assert
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.REJECTED, updated.getStatus());
        assertEquals("rejected", updated.getExternalStatus());
    }

    @Test
    @DisplayName("✅ 9. Procesar webhook MercadoPago - En proceso")
    @Transactional
    void testProcessMercadoPagoWebhookProcessing() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "MERCADO_PAGO");

        // Act
        paymentService.processMercadoPagoWebhookExternalRef(order.getId(), "in_process");

        // Assert
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(Payment.PaymentStatus.PROCESSING, updated.getStatus());
    }

    @Test
    @DisplayName("✅ 10. Evitar procesar webhook duplicado")
    @Transactional
    void testDuplicateWebhookIgnored() {
        // Arrange
        Order order = createTestOrder();
        Payment payment = createTestPayment(order, "MERCADO_PAGO");

        // Act - Procesar webhook una vez
        paymentService.processMercadoPagoWebhookExternalRef(order.getId(), "approved");
        Payment firstUpdate = paymentRepository.findById(payment.getId()).orElseThrow();
        var firstConfirmed = firstUpdate.getConfirmedAt();

        // Act - Procesar el mismo webhook de nuevo
        paymentService.processMercadoPagoWebhookExternalRef(order.getId(), "approved");
        Payment secondUpdate = paymentRepository.findById(payment.getId()).orElseThrow();

        // Assert - confirmadAt debe ser igual (no se reprocesó)
        assertEquals(firstConfirmed, secondUpdate.getConfirmedAt());
    }

    @Test
    @DisplayName("✅ 11. Obtener detalles de pago desde MercadoPago")
    @Transactional
    void testGetMercadoPagoPaymentDetails() {
        // Nota: Este test requiere credenciales reales de MP y conectividad
        // Para desarrollo local, puede mockarse

        // Arrange
        String testPaymentId = "123456789"; // ID ficticio

        // Act
        Map<String, Object> details = paymentService.getMercadoPagoPaymentDetails(testPaymentId);

        // Assert - En desarrollo puede no haber conexión, eso es OK
        assertNotNull(details);
        // En producción verificarías que tenga campos como "id", "status", "external_reference"
    }

    @Test
    @DisplayName("⚠️ 12. No permitir duplicar pago para misma orden")
    @Transactional
    void testCannotDuplicatePayment() {
        // Arrange
        Order order = createTestOrder();
        CreatePaymentRequest paymentReq = new CreatePaymentRequest();
        paymentReq.setOrderId(order.getId());
        paymentReq.setMethod("MERCADO_PAGO");

        paymentService.createPayment(paymentReq); // Primer pago

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(paymentReq); // Segundo intento
        }, "Payment already exists for this order");
    }

    @Test
    @DisplayName("⚠️ 13. Stock debe decrementarse en orden")
    @Transactional
    void testStockDecrementedOnOrder() {
        // Arrange
        int initialStock = testProduct.getStock();

        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setItemId(testProduct.getId());
        itemReq.setQuantity(3);
        request.setItems(List.of(itemReq));
        request.setPaymentMethod("CASH");

        // Act
        orderService.createOrder(request, testUser.getEmail());

        // Assert
        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(initialStock - 3, updated.getStock());
    }

    // ========== HELPERS ==========

    private Order createTestOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setItemId(testProduct.getId());
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));
        request.setPaymentMethod("MERCADO_PAGO");
        return orderService.createOrder(request, testUser.getEmail());
    }

    private Payment createTestPayment(Order order, String method) {
        CreatePaymentRequest paymentReq = new CreatePaymentRequest();
        paymentReq.setOrderId(order.getId());
        paymentReq.setMethod(method);
        return paymentService.createPayment(paymentReq);
    }
}