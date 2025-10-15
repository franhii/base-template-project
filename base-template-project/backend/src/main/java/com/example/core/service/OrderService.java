package com.example.core.service;

import com.example.core.dto.CreateOrderRequest;
import com.example.core.model.*;
import com.example.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

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
        // Obtener usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Crear orden
        Order order = new Order();
        order.setUser(user);
        order.setTenant(user.getTenant());
        order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()));
        order.setNotes(request.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;

        // Agregar items
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Item item = findItem(itemReq.getItemId());

            if (item == null) {
                throw new RuntimeException("Item not found: " + itemReq.getItemId());
            }

            if (!item.getTenant().getId().equals(user.getTenant().getId())) {
                throw new RuntimeException("Item does not belong to your tenant");
            }

            // Verificar stock si es producto
            if (item instanceof Product) {
                Product product = (Product) item;
                if (product.getStock() < itemReq.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for: " + product.getName());
                }
                // Descontar stock
                product.setStock(product.getStock() - itemReq.getQuantity());
                productRepository.save(product);
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
        return orderRepository.save(order);
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