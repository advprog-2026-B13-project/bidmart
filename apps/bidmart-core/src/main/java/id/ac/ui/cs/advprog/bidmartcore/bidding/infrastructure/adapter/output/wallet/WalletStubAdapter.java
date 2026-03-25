package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.wallet;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.WalletPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// TODO: Replace with actual WalletService adapter once wallet module implements hold/release
@Component
public class WalletStubAdapter implements WalletPort {

    @Override
    public BigDecimal getAvailableBalance(UUID userId) {
        return BigDecimal.valueOf(Long.MAX_VALUE); // Stub: unlimited balance
    }

    @Override
    public void holdFunds(UUID userId, BigDecimal amount) {
        // Stub: no-op
    }

    @Override
    public void releaseFunds(UUID userId, BigDecimal amount) {
        // Stub: no-op
    }
}
