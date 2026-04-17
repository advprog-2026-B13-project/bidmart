package id.ac.ui.cs.advprog.bidmartcore.order.service;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.order.model.Order;
import id.ac.ui.cs.advprog.bidmartcore.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartcore.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @EventListener
    public void handleAuctionClosed(AuctionClosedEvent event) {
        System.out.println("Sinyal lelang diterima! Listing ID " + event.getListingId());

        if (event.getResult() == AuctionClosedEvent.AuctionResult.WON) {
            Order newOrder = new Order();

            newOrder.setListingId(event.getListingId());
            newOrder.setTotalAmount(event.getFinalAmount());
            newOrder.setBuyerId(event.getWinnerBidderId());
            newOrder.setSellerId(event.getSellerId());
            newOrder.setStatus(OrderStatus.PENDING);

            orderRepository.save(newOrder);
            System.out.println("Pesanan otomatis dibuat untuk Pembeli: " + event.getWinnerBidderId());
        } else {
            System.out.println("Lelang ditutup tanpa pemenang. Tidak ada pesanan yang dibuat.");
        }
    }
}
