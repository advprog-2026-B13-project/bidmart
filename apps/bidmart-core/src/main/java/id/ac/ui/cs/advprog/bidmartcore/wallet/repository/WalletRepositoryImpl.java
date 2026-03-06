package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: rename atau modif file template ini
@Component
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository{
    private final WalletSpringRepository walletSpringRepository;

    public WalletRepositoryImpl(WalletSpringRepository walletSpringRepository) {
        this.walletSpringRepository = walletSpringRepository;
    }

    @Override
    public WalletModel save(WalletModel wallet) {
        return walletSpringRepository.save(wallet);
    }

    @Override
    public WalletModel findByUserId(UUID userId) {
        return walletSpringRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }
    @Override
    public List<WalletModel> findAll() {
        return springRepository.findAll();
    }
}
