package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;

    @EventListener
    public void handleAuctionClosed(AuctionClosedEvent event) {
        log.info("Auction closed event received: listingId={} result={}", event.getListingId(), event.getResult());

        if (event.getResult() == AuctionClosedEvent.AuctionResult.WON) {
            Order newOrder = new Order();

            newOrder.setListingId(event.getListingId());
            newOrder.setTotalAmount(event.getFinalAmount());
            newOrder.setBuyerId(event.getWinnerBidderId());
            newOrder.setSellerId(event.getSellerId());
            newOrder.setStatus(OrderStatus.PENDING);

            orderRepository.save(newOrder);
            log.info("Order auto-created for auction winner: listingId={} buyerId={} sellerId={} amount={}",
                    event.getListingId(), event.getWinnerBidderId(), event.getSellerId(), event.getFinalAmount());
        } else {
            log.info("Auction closed without winner, no order created: listingId={}", event.getListingId());
        }
    }
}
