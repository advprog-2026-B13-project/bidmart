package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletSpringRepository walletSpringRepository;

    @Override
    public WalletModel save(WalletModel wallet) {
        return walletSpringRepository.save(wallet);
    }

    @Override
    public WalletModel findByUserId(UUID userId) {
        return walletSpringRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for userId: " + userId));
    }

    @Override
    public List<WalletModel> findAll() {
        return walletSpringRepository.findAll();
    }
}