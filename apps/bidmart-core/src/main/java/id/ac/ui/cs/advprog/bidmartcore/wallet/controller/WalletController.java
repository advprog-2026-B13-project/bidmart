package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/create/{userId}")
    public WalletModel createWallet(@PathVariable UUID userId) {
        return walletService.createWallet(userId);
    }

    @GetMapping("/{userId}")
    public WalletModel getWallet(@PathVariable UUID userId) {
        return walletService.getWalletByUserId(userId);
    }
}