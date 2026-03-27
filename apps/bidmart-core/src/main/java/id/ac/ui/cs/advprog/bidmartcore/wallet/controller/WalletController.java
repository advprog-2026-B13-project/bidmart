package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

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

    @GetMapping("/transactions")
    public List<WalletTransactionModel> getTransactions(@RequestParam UUID userId) {
        return walletService.getTransactions(userId);
    }
}