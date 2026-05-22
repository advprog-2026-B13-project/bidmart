package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;
    private UUID listingId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        listingId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(orderId);
        testOrder.setListingId(listingId);
        testOrder.setBuyerId(buyerId);
        testOrder.setSellerId(sellerId);
        testOrder.setTotalAmount(new BigDecimal("150000.00"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void getOrdersByBuyerId_shouldReturnList() {
        when(orderRepository.findByBuyerId(buyerId)).thenReturn(Arrays.asList(testOrder));

        List<Order> result = orderService.getOrdersByBuyerId(buyerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderRepository, times(1)).findByBuyerId(buyerId);
    }

    @Test
    void getOrdersBySellerId_shouldReturnList() {
        when(orderRepository.findBySellerId(sellerId)).thenReturn(Arrays.asList(testOrder));

        List<Order> result = orderService.getOrdersBySellerId(sellerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderRepository, times(1)).findBySellerId(sellerId);
    }

    @Test
    void updateShipmentStatus_whenOrderNotFound_shouldThrowRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            orderService.updateShipmentStatus(orderId, sellerId, OrderStatus.SHIPPED, "TRACK123")
        );

        assertTrue(exception.getMessage().contains("tidak ditemukan"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateShipmentStatus_whenNotSeller_shouldThrowRuntimeException() {
        UUID incorrectSellerId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            orderService.updateShipmentStatus(orderId, incorrectSellerId, OrderStatus.SHIPPED, "TRACK123")
        );

        assertTrue(exception.getMessage().contains("Anda bukan penjual"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateShipmentStatus_whenShippedWithoutTrackingNumber_shouldThrowIllegalArgumentException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            orderService.updateShipmentStatus(orderId, sellerId, OrderStatus.SHIPPED, "")
        );

        assertTrue(exception.getMessage().contains("Nomor resi wajib diinput"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateShipmentStatus_whenShippedWithTrackingNumber_shouldUpdateAndSave() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateShipmentStatus(orderId, sellerId, OrderStatus.SHIPPED, "TRACK123");

        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.SHIPPED, updatedOrder.getStatus());
        assertEquals("TRACK123", updatedOrder.getTrackingNumber());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void updateShipmentStatus_whenNotShipped_shouldUpdateAndSaveWithoutTracking() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateShipmentStatus(orderId, sellerId, OrderStatus.PACKED, null);

        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.PACKED, updatedOrder.getStatus());
        assertNull(updatedOrder.getTrackingNumber());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void confirmDelivery_whenOrderNotFound_shouldThrowRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            orderService.confirmDelivery(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("tidak ditemukan"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmDelivery_whenNotBuyer_shouldThrowRuntimeException() {
        UUID incorrectBuyerId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            orderService.confirmDelivery(orderId, incorrectBuyerId)
        );

        assertTrue(exception.getMessage().contains("Anda bukan pembeli"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmDelivery_whenNotShipped_shouldThrowIllegalStateException() {
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            orderService.confirmDelivery(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("Pesanan belum dikirim"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmDelivery_whenShipped_shouldUpdateToCompleted() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order completedOrder = orderService.confirmDelivery(orderId, buyerId);

        assertNotNull(completedOrder);
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void disputeOrder_whenOrderNotFound_shouldThrowIllegalArgumentException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            orderService.disputeOrder(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("Pesanan tidak ditemukan"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void disputeOrder_whenNotBuyer_shouldThrowSecurityException() {
        UUID incorrectBuyerId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        SecurityException exception = assertThrows(SecurityException.class, () -> 
            orderService.disputeOrder(orderId, incorrectBuyerId)
        );

        assertTrue(exception.getMessage().contains("Anda bukan pembeli"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void disputeOrder_whenCompleted_shouldThrowIllegalStateException() {
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            orderService.disputeOrder(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("Pesanan sudah selesai"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void disputeOrder_whenAlreadyDisputed_shouldThrowIllegalStateException() {
        testOrder.setStatus(OrderStatus.DISPUTED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            orderService.disputeOrder(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("sudah dalam status sengketa"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void disputeOrder_whenValid_shouldUpdateToDisputed() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order disputedOrder = orderService.disputeOrder(orderId, buyerId);

        assertNotNull(disputedOrder);
        assertEquals(OrderStatus.DISPUTED, disputedOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldThrowIllegalArgumentException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            orderService.getOrderById(orderId, buyerId)
        );

        assertTrue(exception.getMessage().contains("tidak ditemukan"));
    }

    @Test
    void getOrderById_whenAccessDenied_shouldThrowSecurityException() {
        UUID strangerId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        SecurityException exception = assertThrows(SecurityException.class, () -> 
            orderService.getOrderById(orderId, strangerId)
        );

        assertTrue(exception.getMessage().contains("Anda tidak memiliki akses"));
    }

    @Test
    void getOrderById_whenBuyerAccesses_shouldReturnOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        Order result = orderService.getOrderById(orderId, buyerId);

        assertNotNull(result);
        assertEquals(testOrder, result);
    }

    @Test
    void getOrderById_whenSellerAccesses_shouldReturnOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        Order result = orderService.getOrderById(orderId, sellerId);

        assertNotNull(result);
        assertEquals(testOrder, result);
    }
}
