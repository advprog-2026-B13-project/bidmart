package id.ac.ui.cs.advprog.bidmartcore.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthContext authContext;

    @Autowired
    public OrderController(OrderService orderService, AuthContext authContext) {
        this.orderService = orderService;
        this.authContext = authContext;
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getBuyerOrders(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyerId(buyerId);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/shipment")
    @RequireLogin
    public ResponseEntity<Order> updateShipmentStatus(@PathVariable UUID orderId, @RequestParam OrderStatus status, @RequestParam(required = false) String trackingNumber) {
        UUID currentSellerId = authContext.getUserId();

        Order updatedOrder = orderService.updateShipmentStatus(orderId, currentSellerId, status, trackingNumber);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{orderId}/confirm")
    @RequireLogin
    public ResponseEntity<Order> confirmDelivery(@PathVariable UUID orderId) {
        UUID currentBuyerId = authContext.getUserId();

        Order confirmedOrder = orderService.confirmDelivery(orderId, currentBuyerId);
        return ResponseEntity.ok(confirmedOrder);
    }

    @RequireLogin
    @PutMapping("/{orderId}/dispute")
    public ResponseEntity<Order> disputeOrder(@PathVariable UUID orderId, AuthContext authContext) {
        UUID buyerId = this.authContext.getUserId();

        Order updatedOrder = orderService.disputeOrder(orderId, buyerId);
        return ResponseEntity.ok(updatedOrder);
    }

    @RequireLogin
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId) {
        UUID currentUserId = this.authContext.getUserId();
        Order order = orderService.getOrderById(orderId, currentUserId);
        return ResponseEntity.ok(order);
    }
}