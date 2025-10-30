package com.example.core.service;

import com.example.core.dto.CreateOrderRequest;
import com.example.core.model.*;
import com.example.core.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        ServiceRepository serviceRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request, String userEmail) {
        logger.info("📝 Creando orden para usuario: {}", userEmail);

        // Obtener usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1️⃣ VALIDAR STOCK PRIMERO (antes de crear la orden)
        Map<String, Integer> stockNeeded = new HashMap<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Item item = findItem(itemReq.getItemId());

            if (item == null) {
                throw new RuntimeException("Item not found: " + itemReq.getItemId());
            }

            // Solo validar stock para productos físicos
            if (item instanceof Product) {
                Product product = (Product) item;
                if (product.getType() == Product.ProductType.PHYSICAL) {
                    if (product.getStock() < itemReq.getQuantity()) {
                        throw new RuntimeException(
                                String.format("Stock insuficiente para '%s'. Disponible: %d, Solicitado: %d",
                                        product.getName(), product.getStock(), itemReq.getQuantity())
                        );
                    }
                    stockNeeded.put(product.getId(), itemReq.getQuantity());
                }
            }
        }

        // 2️⃣ CREAR ORDEN
        Order order = new Order();
        order.setUser(user);
        order.setTenant(user.getTenant());
        order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()));
        order.setNotes(request.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;

        // 3️⃣ AGREGAR ITEMS Y DESCONTAR STOCK
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Item item = findItem(itemReq.getItemId());

            if (!item.getTenant().getId().equals(user.getTenant().getId())) {
                throw new RuntimeException("Item does not belong to your tenant");
            }

            // Descontar stock solo si es producto físico
            if (item instanceof Product) {
                Product product = (Product) item;
                if (product.getType() == Product.ProductType.PHYSICAL) {
                    int newStock = product.getStock() - itemReq.getQuantity();
                    product.setStock(newStock);
                    productRepository.save(product);
                    logger.info("📦 Stock actualizado: {} - Stock restante: {}",
                            product.getName(), newStock);
                }
            }

            // Crear OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPriceAtPurchase(item.getPrice());
            orderItem.setItemName(item.getName());
            orderItem.setItemType(item instanceof Product ? "PRODUCT" : "SERVICE");

            order.getItems().add(orderItem);

            // Calcular total
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(itemTotal);
        }

        order.setTotal(total);
        Order savedOrder = orderRepository.save(order);

        logger.info("✅ Orden creada: {} - Total: ${}", savedOrder.getId(), total);
        return savedOrder;
    }

    /**
     * Restaurar stock cuando un pago es cancelado/rechazado
     */
    @Transactional
    public void restoreStock(Order order) {
        logger.info("🔄 Restaurando stock para orden: {}", order.getId());

        for (OrderItem item : order.getItems()) {
            if ("PRODUCT".equals(item.getItemType())) {
                Product product = productRepository.findById(item.getItem().getId())
                        .orElse(null);

                if (product != null && product.getType() == Product.ProductType.PHYSICAL) {
                    int restoredStock = product.getStock() + item.getQuantity();
                    product.setStock(restoredStock);
                    productRepository.save(product);

                    logger.info("↩️ Stock restaurado: {} - Nuevo stock: {}",
                            product.getName(), restoredStock);
                }
            }
        }
    }

    private Item findItem(String itemId) {
        // Intentar buscar como producto
        return productRepository.findById(itemId)
                .map(p -> (Item) p)
                .orElseGet(() ->
                        // Si no es producto, buscar como servicio
                        serviceRepository.findById(itemId)
                                .map(s -> (Item) s)
                                .orElse(null)
                );
    }
}