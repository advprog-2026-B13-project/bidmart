package id.ac.ui.cs.advprog.bidmartcore.order.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Test
    void handleAuctionClosed_whenAuctionWon_shouldCreateAndSaveOrder() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250000.00");
        LocalDateTime timestamp = LocalDateTime.now();

        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId,
                sellerId,
                winnerId,
                amount,
                AuctionClosedEvent.AuctionResult.WON,
                timestamp
        );

        orderEventListener.handleAuctionClosed(event);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertNotNull(savedOrder);
        assertEquals(listingId, savedOrder.getListingId());
        assertEquals(sellerId, savedOrder.getSellerId());
        assertEquals(winnerId, savedOrder.getBuyerId());
        assertEquals(amount, savedOrder.getTotalAmount());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
    }

    @Test
    void handleAuctionClosed_whenAuctionUnsold_shouldNotCreateOrder() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId,
                sellerId,
                null,
                null,
                AuctionClosedEvent.AuctionResult.UNSOLD,
                timestamp
        );

        orderEventListener.handleAuctionClosed(event);

        verify(orderRepository, never()).save(any(Order.class));
    }
}
