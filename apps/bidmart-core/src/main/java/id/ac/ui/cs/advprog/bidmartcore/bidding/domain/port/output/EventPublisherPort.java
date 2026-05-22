package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionTimeExtendedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;

// may be replaced with RabbitMQ or Redis pub/sub in the future
public interface EventPublisherPort {
    void publishBidPlaced(BidPlacedEvent event);
    void publishOutbid(OutbidEvent event);
    void publishAuctionClosed(AuctionClosedEvent event);
    void publishAuctionTimeExtended(AuctionTimeExtendedEvent event);
}
