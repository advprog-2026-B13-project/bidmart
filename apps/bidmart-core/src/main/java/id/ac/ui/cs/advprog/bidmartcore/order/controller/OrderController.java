package id.ac.ui.cs.advprog.bidmartcore.order.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
}