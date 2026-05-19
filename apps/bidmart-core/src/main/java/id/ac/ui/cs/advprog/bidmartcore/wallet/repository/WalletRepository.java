package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;

import java.util.List;
import java.util.UUID;

public interface WalletRepository {
    List<WalletModel> findAll();

    WalletModel save(WalletModel wallet);

    WalletModel findByUserId(UUID userId);
}
