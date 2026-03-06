package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Order> getOrdersByBuyerId(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }
}