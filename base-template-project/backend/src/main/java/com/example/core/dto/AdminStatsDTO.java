package com.example.core.dto;

import lombok.Data;

import java.math.BigDecimal;

// Estadísticas principales del dashboard
@Data
public class AdminStatsDTO {
    private BigDecimal totalSales;           // Total de ventas confirmadas
    private Integer totalOrders;              // Total de órdenes
    private Integer pendingOrders;            // Órdenes pendientes
    private Integer approvedPayments;         // Pagos aprobados
    private Integer pendingPayments;          // Pagos en revisión
    private Integer activeCustomers;          // Clientes que han comprado
    private BigDecimal monthlyRevenue;        // Ingresos del mes actual
    private BigDecimal averageTicket;         // Promedio de compra
}