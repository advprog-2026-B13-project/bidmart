package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create/{userId}")
    public WalletModel createWallet(@PathVariable UUID userId) {
        return walletService.createWallet(userId);
    }

    @GetMapping("/{userId}")
    public WalletModel getWallet(@PathVariable UUID userId) {
        return walletService.getWalletByUserId(userId);
    }

    @PostMapping("/{userId}/topup")
    public WalletModel topUp(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {

        return walletService.topUp(userId, amount);
    }

    @PostMapping("/{userId}/withdraw")
    public WalletModel withdraw(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {

        return walletService.withdraw(userId, amount);
    }

    @PostMapping("/{userId}/hold")
    public Map<String, Object> holdBalance(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {

        WalletModel wallet =
                walletService.holdBalance(userId, amount);

        return Map.of(
                "message", "balance held",
                "available_balance", wallet.getAvailableBalance(),
                "held_balance", wallet.getHeldBalance()
        );
    }

    @PostMapping("/{userId}/release")
    public WalletModel releaseBalance(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {

        return walletService.releaseBalance(userId, amount);
    }

    @PostMapping("/{userId}/payment")
    public WalletModel convertHoldToPayment(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {

        return walletService.convertHoldToPayment(userId, amount);
    }

    @GetMapping("/transactions")
    public List<WalletTransactionModel> getTransactions(
            @RequestParam UUID userId) {

        return walletService.getTransactions(userId);
    }
}