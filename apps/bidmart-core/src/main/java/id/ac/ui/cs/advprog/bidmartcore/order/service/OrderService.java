package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<Order> getOrdersByBuyerId(UUID buyerId);
}