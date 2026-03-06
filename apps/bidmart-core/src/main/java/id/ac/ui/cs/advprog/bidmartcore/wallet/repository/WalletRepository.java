package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;

import java.util.List;

// TODO: rename atau modif file template ini
public interface WalletRepository {
    List<WalletModel> findAll();

    WalletModel save(WalletModel wallet);

    WalletModel findByUserId(UUID userId);
}
