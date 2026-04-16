package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<Order> getOrdersByBuyerId(UUID buyerId);
    Order updateShipmentStatus(UUID orderId, UUID sellerId, OrderStatus newStatus, String trackingNumber);
}