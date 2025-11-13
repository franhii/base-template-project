package com.example.core.service;

import com.example.core.dto.CreateOrderRequest;
import com.example.core.dto.ShippingCalculationRequest;
import com.example.core.dto.ShippingOptionDTO;
import com.example.core.exception.ResourceNotFoundException;
import com.example.core.model.*;
import com.example.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MercadoEnviosService mercadoEnviosService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final AddressRepository addressRepository;

    public OrderService(OrderRepository orderRepository,
                        MercadoEnviosService mercadoEnviosService,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        ServiceRepository serviceRepository,
                        BookingRepository bookingRepository,
                        AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.mercadoEnviosService = mercadoEnviosService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
        this.addressRepository = addressRepository;
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

            // 🗓️ Si es servicio con booking, guardar fecha/hora
            if (item instanceof ServiceItem) {
                ServiceItem service = (ServiceItem) item;
                if (service.isRequiresBooking()) {
                    if (itemReq.getBookingDate() == null || itemReq.getBookingTime() == null) {
                        throw new RuntimeException("El servicio '" + service.getName() + "' requiere fecha y hora de reserva");
                    }
                    orderItem.setBookingDate(LocalDate.parse(itemReq.getBookingDate()));
                    orderItem.setBookingTime(LocalTime.parse(itemReq.getBookingTime()));
                }
            }

            order.getItems().add(orderItem);

            // Calcular total
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(itemTotal);
        }

        order.setTotal(total);
        Order savedOrder = orderRepository.save(order);

        // 4️⃣ CREAR BOOKINGS PARA SERVICIOS
        for (OrderItem orderItem : savedOrder.getItems()) {
            if ("SERVICE".equals(orderItem.getItemType()) && orderItem.getBookingDate() != null) {
                ServiceItem service = (ServiceItem) orderItem.getItem();

                // Crear un booking por cada cantidad
                for (int i = 0; i < orderItem.getQuantity(); i++) {
                    Booking booking = new Booking();
                    booking.setService(service);
                    booking.setOrder(savedOrder);
                    booking.setOrderItem(orderItem);
                    booking.setUser(user);
                    booking.setTenant(user.getTenant());
                    booking.setBookingDate(orderItem.getBookingDate());
                    booking.setStartTime(orderItem.getBookingTime());
                    booking.setEndTime(orderItem.getBookingTime().plusMinutes(service.getDurationMinutes()));
                    booking.setStatus(Booking.BookingStatus.PENDING);
                    booking.setCustomerName(user.getName());
                    booking.setCustomerEmail(user.getEmail());
                    booking.setNotes(savedOrder.getNotes());

                    bookingRepository.save(booking);
                    logger.info("🗓️ Booking creado para orden: {} - Fecha: {} - Hora: {}",
                            savedOrder.getId(), booking.getBookingDate(), booking.getStartTime());
                }
            }
        }
        // ========== PROCESAR ENVÍO ==========
        if (request.getIsDelivery()) {

            // Validar que se haya enviado dirección
            if (request.getDeliveryAddressId() == null || request.getDeliveryAddressId().isBlank()) {
                throw new IllegalArgumentException("Se requiere una dirección para delivery");
            }

            // Validar que se haya seleccionado método de envío
            if (request.getShippingMethodId() == null) {
                throw new IllegalArgumentException("Se requiere seleccionar un método de envío");
            }

            // Obtener dirección del usuario
            Address deliveryAddress = addressRepository.findByIdAndUserIdAndTenantId(
                            request.getDeliveryAddressId(),
                            user.getId(),
                            user.getTenant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

            // Obtener CP del tenant (origen)
            String zipCodeFrom = getTenantPostalCode(user.getTenant());
            if (zipCodeFrom == null) {
                throw new IllegalArgumentException("El negocio no tiene código postal configurado");
            }

            // Cotizar envío para validar el método seleccionado
            ShippingCalculationRequest shippingRequest = ShippingCalculationRequest.builder()
                    .zipCodeFrom(zipCodeFrom)
                    .zipCodeTo(deliveryAddress.getPostalCode())
                    .dimensions("20x20x10,1000") // TODO: Hacer configurable por tenant
                    .listCost(total)
                    .freeShipping(false)
                    .build();

            ShippingOptionDTO selectedOption = mercadoEnviosService.findShippingOption(
                    shippingRequest,
                    request.getShippingMethodId());

            // Configurar datos de envío en la orden
            order.setDeliveryAddress(deliveryAddress);
            order.setDeliveryCost(selectedOption.getCost());
            order.setDelivery(true);
            order.setDeliveryNotes(request.getDeliveryNotes());
            order.setShippingMethodId(selectedOption.getShippingMethodId());
            order.setStatus(Order.OrderStatus.PREPARING); // Pendiente hasta que se confirme el pago

            log.info("Envío configurado: {} (${}) para orden {}",
                    selectedOption.getName(),
                    selectedOption.getCost(),
                    order.getId());

        } else {
            // Es retiro en local (pickup)
            order.setDelivery(false);
            order.setDeliveryCost(BigDecimal.ZERO);
            order.setStatus(Order.OrderStatus.CANCELLED);

            log.info("Orden configurada para retiro en local: {}", order.getId());
        }

        // Recalcular total incluyendo envío
        BigDecimal totalWithShipping = order.getTotalWithDelivery();
        order.setTotal(totalWithShipping);

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

    /**
     * Cancelar orden completa con restauración de stock
     */
    @Transactional
    public Order cancelOrder(String orderId, String reason) {
        logger.info("🚫 Cancelando orden: {} - Razón: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Solo se puede cancelar si está PENDING o CONFIRMED
        if (order.getStatus() != Order.OrderStatus.PENDING &&
            order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("No se puede cancelar una orden en estado: " + order.getStatus());
        }

        // Restaurar stock
        restoreStock(order);

        // Cambiar estado
        order.setStatus(Order.OrderStatus.CANCELLED);
        if (reason != null && !reason.isEmpty()) {
            order.setNotes(order.getNotes() + " | CANCELACIÓN: " + reason);
        }

        Order savedOrder = orderRepository.save(order);
        logger.info("✅ Orden cancelada exitosamente");

        return savedOrder;
    }



    private String getTenantPostalCode(Tenant tenant) {
        try {
            // Opción 1: Si está directo en Tenant
            // return tenant.getPostalCode();

            // Opción 2: Si está en TenantConfig (JSONB)
            Object config = tenant.getConfig();
            if (config instanceof java.util.Map) {
                return (String) ((java.util.Map<?, ?>) config).get("postalCode");
            }
            return null;
        } catch (Exception e) {
            log.error("Error obteniendo CP del tenant", e);
            return null;
        }
    }

    // ========== ACTUALIZAR ESTADO DE ENVÍO (cuando se confirma el pago) ==========

    // En el método que confirma el pago (ej: después del webhook de MercadoPago)
    public void updateShippingStatus(Order order) {
        if (order.isDelivery() && order.getStatus().equals(Order.OrderStatus.PENDING)) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // TODO: Crear shipment en MercadoEnvíos si se necesita tracking
            // String shipmentId = mercadoEnviosService.createShipment(order.getId(), order.getShippingMethodId());
            // order.setShipmentId(shipmentId);

            log.info("Estado de envío actualizado a ready_to_ship para orden {}", order.getId());
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