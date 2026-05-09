package id.ac.ui.cs.advprog.bidmartcore.wallet.service;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    List<WalletModel> findAll();
    WalletModel createWallet(UUID userId);
    WalletModel getWalletByUserId(UUID userId);
    WalletModel topUp(UUID userId, BigDecimal amount);
    WalletModel withdraw(UUID userId, BigDecimal amount);
    WalletModel deposit(UUID userId, BigDecimal amount);
    WalletModel holdBalance(UUID userId, BigDecimal amount);
    WalletModel releaseBalance(UUID userId, BigDecimal amount);
    WalletModel convertHoldToPayment(UUID userId, BigDecimal amount);
    List<WalletTransactionModel> getTransactions(UUID userId);
    void transferHeldBalance(UUID buyerId, UUID sellerId, BigDecimal amount);
}