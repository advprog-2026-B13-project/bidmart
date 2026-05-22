package id.ac.ui.cs.advprog.bidmartcore.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;

@Slf4j
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
    public List<Order> getOrdersBySellerId(UUID sellerId) {
        return orderRepository.findBySellerId(sellerId);
    }

    @Override
    public Order updateShipmentStatus(UUID orderId, UUID sellerId, OrderStatus newStatus, String trackingNumber) {
        log.info("Order shipment update: orderId={} sellerId={} newStatus={}", orderId, sellerId, newStatus);
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

        Order saved = orderRepository.save(order);
        log.info("Order shipment updated: orderId={} status={}", orderId, newStatus);
        return saved;
    }

    @Override
    public Order confirmDelivery(UUID orderId, UUID buyerId) {
        log.info("Order delivery confirmed: orderId={} buyerId={}", orderId, buyerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pesanan dengan ID " + orderId + " tidak ditemukan"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Anda bukan pembeli dari pesanan ini.");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Pesanan belum dikirim (Status saat ini: " + order.getStatus() + ").");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return orderRepository.save(order);
    }

    @Override
    public Order disputeOrder(UUID orderId, UUID buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new SecurityException("Akses ditolak: Anda bukan pembeli dari pesanan ini");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Pesanan sudah selesai, sengketa tidak dapat diajukan");
        }
        if (order.getStatus() == OrderStatus.DISPUTED) {
            throw new IllegalStateException("Pesanan ini sudah dalam status sengketa");
        }

        order.setStatus(OrderStatus.DISPUTED);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(UUID orderId, UUID currentUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pesanan dengan ID " + orderId + " tidak ditemukan."));
        if (!order.getBuyerId().equals(currentUserId) && !order.getSellerId().equals(currentUserId)) {
            throw new SecurityException("Akses ditolak: Anda tidak memiliki akses ke pesanan ini.");
        }
        return order;
    }
}