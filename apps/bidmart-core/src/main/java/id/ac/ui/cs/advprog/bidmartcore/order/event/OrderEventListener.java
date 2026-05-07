package id.ac.ui.cs.advprog.bidmartcore.order.event;

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
        log.info("Sinyal lelang diterima! Listing ID: {}", event.getListingId());

        if (event.getResult() == AuctionClosedEvent.AuctionResult.WON) {
            Order newOrder = new Order();

            newOrder.setListingId(event.getListingId());
            newOrder.setTotalAmount(event.getFinalAmount());
            newOrder.setBuyerId(event.getWinnerBidderId());
            newOrder.setSellerId(event.getSellerId());
            newOrder.setStatus(OrderStatus.PENDING);

            orderRepository.save(newOrder);
            log.info("Pesanan otomatis dibuat untuk Pembeli: {}", event.getWinnerBidderId());
        } else {
            log.info("Lelang ditutup tanpa pemenang. Tidak ada pesanan yang dibuat.");
        }
    }
}