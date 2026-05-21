package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository {
    WalletTransactionModel save(WalletTransactionModel transaction);

    List<WalletTransactionModel> findByWalletId(UUID walletId);
}