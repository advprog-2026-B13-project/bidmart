package id.ac.ui.cs.advprog.bidmartcore.order.service;

import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;

public interface OrderService {
    List<Order> getOrdersByBuyerId(UUID buyerId);
    List<Order> getOrdersBySellerId(UUID sellerId);
    Order updateShipmentStatus(UUID orderId, UUID sellerId, OrderStatus newStatus, String trackingNumber);
    Order confirmDelivery(UUID orderId, UUID buyerId);
    Order disputeOrder(UUID orderId, UUID buyerId);
    Order getOrderById(UUID orderId, UUID currentUserId);
}