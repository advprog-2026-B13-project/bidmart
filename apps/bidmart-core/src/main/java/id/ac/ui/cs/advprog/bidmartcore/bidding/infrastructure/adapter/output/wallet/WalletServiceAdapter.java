package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.wallet;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.WalletPort;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletServiceAdapter implements WalletPort {

    private final WalletService walletService;

    @Override
    public BigDecimal getAvailableBalance(UUID userId) {
        WalletModel wallet = walletService.getWalletByUserId(userId);
        return wallet.getAvailableBalance();
    }

    @Override
    public void holdFunds(UUID userId, BigDecimal amount) {
        walletService.holdBalance(userId, amount);
    }

    @Override
    public void releaseFunds(UUID userId, BigDecimal amount) {
        walletService.releaseBalance(userId, amount);
    }
}
