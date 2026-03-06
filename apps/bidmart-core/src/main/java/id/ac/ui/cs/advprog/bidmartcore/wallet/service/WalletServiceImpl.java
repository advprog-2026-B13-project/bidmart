package id.ac.ui.cs.advprog.bidmartcore.wallet.service;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public WalletModel createWallet(UUID userId) {
        WalletModel wallet = new WalletModel();
        wallet.setUserId(userId);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    @Override
    public WalletModel getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    @Override
    public WalletModel topUp(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be greater than zero");
        }
        WalletModel wallet = walletRepository.findByUserId(userId);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Override
    public List<WalletModel> findAll() {
        return walletRepository.findAll();
    }
}