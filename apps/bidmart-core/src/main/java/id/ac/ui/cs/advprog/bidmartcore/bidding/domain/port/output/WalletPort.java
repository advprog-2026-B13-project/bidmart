package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletPort {
    BigDecimal getAvailableBalance(UUID userId);
    void holdFunds(UUID userId, BigDecimal amount);
    void releaseFunds(UUID userId, BigDecimal amount);
}
