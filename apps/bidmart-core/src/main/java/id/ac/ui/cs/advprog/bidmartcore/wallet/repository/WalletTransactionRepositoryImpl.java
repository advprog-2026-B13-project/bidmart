package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    private final WalletTransactionSpringRepository repository;

    @Override
    public WalletTransactionModel save(WalletTransactionModel transaction) {
        return repository.save(transaction);
    }

    @Override
    public List<WalletTransactionModel> findByWalletId(UUID walletId) {
        return repository.findByWalletId(walletId);
    }
}