package id.ac.ui.cs.advprog.bidmartcore.wallet.service;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {

    List<WalletModel> findAll();

    WalletModel createWallet(UUID userId);

    WalletModel findByUserId(UUID userId);

    WalletModel getWalletByUserId(UUID userId);

    WalletModel deposit(UUID userId, BigDecimal amount);
}