package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionSpringRepository
        extends JpaRepository<WalletTransactionModel, UUID> {

    List<WalletTransactionModel> findByWalletId(UUID walletId);
}