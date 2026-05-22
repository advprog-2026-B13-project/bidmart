package id.ac.ui.cs.advprog.bidmartcore.order.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private AuthContext authContext;

    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, authContext)).build();

        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(orderId);
        testOrder.setBuyerId(buyerId);
        testOrder.setSellerId(sellerId);
        testOrder.setTotalAmount(new BigDecimal("150000.00"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void getBuyerOrders_shouldReturnOrdersList() throws Exception {
        when(orderService.getOrdersByBuyerId(buyerId)).thenReturn(Arrays.asList(testOrder));

        mockMvc.perform(get("/api/orders/buyer/{buyerId}", buyerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].buyerId").value(buyerId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService, times(1)).getOrdersByBuyerId(buyerId);
    }

    @Test
    void getSellerOrders_shouldReturnOrdersList() throws Exception {
        when(orderService.getOrdersBySellerId(sellerId)).thenReturn(Arrays.asList(testOrder));

        mockMvc.perform(get("/api/orders/seller/{sellerId}", sellerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService, times(1)).getOrdersBySellerId(sellerId);
    }

    @Test
    void updateShipmentStatus_shouldReturnUpdatedOrder() throws Exception {
        when(authContext.getUserId()).thenReturn(sellerId);
        testOrder.setStatus(OrderStatus.SHIPPED);
        testOrder.setTrackingNumber("TRACK123");
        when(orderService.updateShipmentStatus(eq(orderId), eq(sellerId), eq(OrderStatus.SHIPPED), eq("TRACK123")))
                .thenReturn(testOrder);

        mockMvc.perform(put("/api/orders/{orderId}/shipment", orderId)
                .param("status", "SHIPPED")
                .param("trackingNumber", "TRACK123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123"));

        verify(orderService, times(1)).updateShipmentStatus(orderId, sellerId, OrderStatus.SHIPPED, "TRACK123");
    }

    @Test
    void confirmDelivery_shouldReturnConfirmedOrder() throws Exception {
        when(authContext.getUserId()).thenReturn(buyerId);
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderService.confirmDelivery(eq(orderId), eq(buyerId))).thenReturn(testOrder);

        mockMvc.perform(put("/api/orders/{orderId}/confirm", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(orderService, times(1)).confirmDelivery(orderId, buyerId);
    }

    @Test
    void disputeOrder_shouldReturnDisputedOrder() throws Exception {
        when(authContext.getUserId()).thenReturn(buyerId);
        testOrder.setStatus(OrderStatus.DISPUTED);
        when(orderService.disputeOrder(eq(orderId), eq(buyerId))).thenReturn(testOrder);

        mockMvc.perform(put("/api/orders/{orderId}/dispute", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"));

        verify(orderService, times(1)).disputeOrder(orderId, buyerId);
    }

    @Test
    void getOrderById_shouldReturnOrder() throws Exception {
        when(authContext.getUserId()).thenReturn(buyerId);
        when(orderService.getOrderById(eq(orderId), eq(buyerId))).thenReturn(testOrder);

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).getOrderById(orderId, buyerId);
    }
}
