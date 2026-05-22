package id.ac.ui.cs.advprog.bidmartcore.order.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void testOrderGettersSettersAndConstructors() {
        UUID id = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal totalAmount = new BigDecimal("500000.00");
        OrderStatus status = OrderStatus.PENDING;
        String trackingNumber = "RES12345";
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setId(id);
        order.setListingId(listingId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setTrackingNumber(trackingNumber);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        assertEquals(id, order.getId());
        assertEquals(listingId, order.getListingId());
        assertEquals(buyerId, order.getBuyerId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(totalAmount, order.getTotalAmount());
        assertEquals(status, order.getStatus());
        assertEquals(trackingNumber, order.getTrackingNumber());
        assertEquals(now, order.getCreatedAt());
        assertEquals(now, order.getUpdatedAt());

        // Test All-args Constructor
        Order orderAllArgs = new Order(id, listingId, buyerId, sellerId, totalAmount, status, trackingNumber, now, now);
        assertEquals(id, orderAllArgs.getId());
        assertEquals(listingId, orderAllArgs.getListingId());
        assertEquals(buyerId, orderAllArgs.getBuyerId());
        assertEquals(sellerId, orderAllArgs.getSellerId());
        assertEquals(totalAmount, orderAllArgs.getTotalAmount());
        assertEquals(status, orderAllArgs.getStatus());
        assertEquals(trackingNumber, orderAllArgs.getTrackingNumber());
        assertEquals(now, orderAllArgs.getCreatedAt());
        assertEquals(now, orderAllArgs.getUpdatedAt());
    }

    @Test
    void testOrderStatusEnumValues() {
        assertEquals("PENDING", OrderStatus.PENDING.name());
        assertEquals("PACKED", OrderStatus.PACKED.name());
        assertEquals("SHIPPED", OrderStatus.SHIPPED.name());
        assertEquals("COMPLETED", OrderStatus.COMPLETED.name());
        assertEquals("DISPUTED", OrderStatus.DISPUTED.name());

        OrderStatus[] statuses = OrderStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
    }
}
