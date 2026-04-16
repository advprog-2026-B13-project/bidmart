package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
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

    @Override
    public Order updateShipmentStatus(UUID orderId, UUID sellerId, OrderStatus newStatus, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pesanan dengan ID " + orderId + " tidak ditemukan."));

        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Anda bukan penjual dari pesanan ini.");
        }

        if (newStatus == OrderStatus.SHIPPED && (trackingNumber == null || trackingNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("Nomor resi wajib diinput saat status pesanan menjadi SHIPPED.");
        }

        order.setStatus(newStatus);
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber);
        }

        return orderRepository.save(order);
    }
}