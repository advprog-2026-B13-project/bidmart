package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.EventPublisherPort;
import lombok.RequiredArgsConstructor;

// might change to use RabbitMQ in the future
@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishBidPlaced(BidPlacedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishOutbid(OutbidEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishAuctionClosed(AuctionClosedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
