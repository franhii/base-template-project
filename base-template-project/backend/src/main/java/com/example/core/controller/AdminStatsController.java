package com.example.core.controller;

import com.example.core.dto.AdminStatsDTO;
import com.example.core.dto.DailySalesDTO;
import com.example.core.dto.PendingPaymentDTO;
import com.example.core.model.*;
import com.example.core.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
public class AdminStatsController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;

    public AdminStatsController(OrderRepository orderRepository,
                                PaymentRepository paymentRepository,
                                UserRepository userRepository,
                                ProductRepository productRepository,
                                TenantRepository tenantRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * GET /api/admin/stats/dashboard
     * Retorna las estadísticas principales del dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminStatsDTO> getDashboardStats(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        // Obtener datos del tenant
        List<Order> allOrders = orderRepository.findByTenant(tenant);
        List<Payment> allPayments = paymentRepository.findByTenant(tenant);

        AdminStatsDTO stats = new AdminStatsDTO();

        // 1. Total de ventas (órdenes confirmadas)
        BigDecimal totalSales = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSales(totalSales);

        // 2. Total de órdenes
        stats.setTotalOrders(allOrders.size());

        // 3. Órdenes pendientes (sin confirmar)
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .count();
        stats.setPendingOrders((int) pendingOrders);

        // 4. Pagos confirmados
        long approvedPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED)
                .count();
        stats.setApprovedPayments((int) approvedPayments);

        // 5. Pagos pendientes de revisión
        long pendingPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .count();
        stats.setPendingPayments((int) pendingPayments);

        // 6. Clientes activos (que han comprado)
        long activeCustomers = allOrders.stream()
                .map(Order::getUser)
                .distinct()
                .count();
        stats.setActiveCustomers((int) activeCustomers);

        // 7. Ingresos del mes actual
        LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIDNIGHT);
        BigDecimal monthlyRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(startOfMonth) &&
                        o.getStatus() == Order.OrderStatus.CONFIRMED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMonthlyRevenue(monthlyRevenue);

        // 8. Promedio de ticket
        BigDecimal averageTicket = totalSales.compareTo(BigDecimal.ZERO) > 0
                ? totalSales.divide(BigDecimal.valueOf(allOrders.size()), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.setAverageTicket(averageTicket);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/stats/daily-sales?days=30
     * Retorna ventas diarias (últimos N días)
     */
    @GetMapping("/daily-sales")
    public ResponseEntity<List<DailySalesDTO>> getDailySales(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Order> orders = orderRepository.findByTenant(tenant).stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate) &&
                        o.getStatus() == Order.OrderStatus.CONFIRMED)
                .collect(Collectors.toList());

        // Agrupar por fecha
        Map<LocalDate, List<Order>> groupedByDate = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<DailySalesDTO> dailySales = groupedByDate.entrySet().stream()
                .map(entry -> {
                    DailySalesDTO dto = new DailySalesDTO();
                    dto.setDate(entry.getKey());

                    BigDecimal dailyTotal = entry.getValue().stream()
                            .map(Order::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dto.setTotal(dailyTotal);

                    dto.setOrderCount(entry.getValue().size());
                    return dto;
                })
                .sorted(Comparator.comparing(DailySalesDTO::getDate))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dailySales);
    }

    /**
     * GET /api/admin/stats/pending-payments
     * Retorna pagos pendientes de revisión (para transferencias manuales)
     */
    @GetMapping("/pending-payments")
    public ResponseEntity<List<PendingPaymentDTO>> getPendingPayments(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        List<Payment> pendingPayments = paymentRepository.findByTenantAndStatus(
                tenant,
                Payment.PaymentStatus.PENDING
        );

        List<PendingPaymentDTO> dtos = pendingPayments.stream()
                .map(p -> {
                    PendingPaymentDTO dto = new PendingPaymentDTO();
                    dto.setId(p.getId());
                    dto.setOrderId(p.getOrder().getId());
                    dto.setAmount(p.getAmount());
                    dto.setMethod(p.getMethod().name());
                    dto.setStatus(p.getStatus().name());
                    dto.setReceiptUrl(p.getReceiptUrl());
                    dto.setReceiptNotes(p.getReceiptNotes());
                    dto.setCreatedAt(p.getCreatedAt());
                    dto.setCustomerName(p.getOrder().getUser().getName());
                    dto.setCustomerEmail(p.getOrder().getUser().getEmail());
                    return dto;
                })
                .sorted(Comparator.comparing(PendingPaymentDTO::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/admin/stats/top-products
     * Productos más vendidos
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        List<Order> orders = orderRepository.findByTenant(tenant).stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED) // Solo órdenes confirmadas
                .collect(Collectors.toList());

        // Agrupar items vendidos por producto
        Map<String, Map<String, Object>> productStats = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                // ✅ FIX: Verificar que sea un producto usando itemType en lugar de instanceof
                if ("PRODUCT".equals(item.getItemType())) {
                    // Usar itemName en lugar de item.getId() ya que el producto puede haber sido eliminado
                    String productKey = item.getItemName(); // Usar nombre como key

                    productStats.putIfAbsent(productKey, new HashMap<>());

                    Map<String, Object> stats = productStats.get(productKey);
                    stats.put("name", item.getItemName());

                    // ✅ FIX: Cast seguro
                    int currentQuantity = stats.containsKey("quantity")
                            ? ((Number) stats.get("quantity")).intValue()
                            : 0;
                    stats.put("quantity", currentQuantity + item.getQuantity());

                    // ✅ FIX: BigDecimal seguro
                    BigDecimal revenue = (BigDecimal) stats.getOrDefault("revenue", BigDecimal.ZERO);
                    BigDecimal itemRevenue = item.getPriceAtPurchase()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    stats.put("revenue", revenue.add(itemRevenue));

                    // Agregar el ID solo si el item aún existe
                    if (item.getItem() != null) {
                        stats.put("id", item.getItem().getId());
                    }
                }
            }
        }

        return ResponseEntity.ok(
                productStats.values().stream()
                        .sorted((a, b) -> {
                            Integer qtyA = ((Number) a.get("quantity")).intValue();
                            Integer qtyB = ((Number) b.get("quantity")).intValue();
                            return qtyB.compareTo(qtyA);
                        })
                        .limit(limit)
                        .collect(Collectors.toList())
        );
    }
    /**
     * GET /api/admin/stats/payment-methods
     * Distribución de métodos de pago
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<Map<String, Integer>> getPaymentMethodsDistribution(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        List<Order> orders = orderRepository.findByTenant(tenant);

        Map<String, Integer> distribution = new HashMap<>();

        for (Order order : orders) {
            if (order.getPaymentMethod() != null) {
                String method = order.getPaymentMethod().name();
                distribution.put(method, distribution.getOrDefault(method, 0) + 1);
            }
        }

        return ResponseEntity.ok(distribution);
    }

    /**
     * GET /api/admin/stats/order-status
     * Distribución de estados de órdenes
     */
    @GetMapping("/order-status")
    public ResponseEntity<Map<String, Integer>> getOrderStatusDistribution(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        List<Order> orders = orderRepository.findByTenant(tenant);

        Map<String, Integer> distribution = new HashMap<>();

        for (Order order : orders) {
            String status = order.getStatus().name();
            distribution.put(status, distribution.getOrDefault(status, 0) + 1);
        }

        return ResponseEntity.ok(distribution);
    }
}