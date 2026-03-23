package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

// might move to Listing service in the future
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bidding")
public class BiddingProperties {
    private BigDecimal minBidIncrement = new BigDecimal("1.00");
    private int antiSnipeSecondsBeforeClose = 120;
    private int antiSnipeExtensionSeconds = 120;
    private int maxAntiSnipeExtensions = 3;
}
