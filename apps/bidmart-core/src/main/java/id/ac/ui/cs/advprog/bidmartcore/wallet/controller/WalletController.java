package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest.ApiResponse;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.wallet.controller.dto.WalletHoldResponse;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet balance and transaction endpoints")
public class WalletController {

    private final WalletService walletService;
    private final AuthContext authContext;

    @PostMapping("/create/{userId}")
    @Operation(summary = "Create a wallet for a user")
    public ResponseEntity<ApiResponse<WalletModel>> createWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.createWallet(userId)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get wallet by user ID")
    public ResponseEntity<ApiResponse<WalletModel>> getWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByUserId(userId)));
    }

    @GetMapping("/me")
    @RequireLogin
    @Operation(summary = "Get wallet for current user")
    public ResponseEntity<ApiResponse<WalletModel>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByUserId(authContext.getUserId())));
    }

    @PostMapping("/{userId}/topup")
    @Operation(summary = "Top up wallet balance")
    public ResponseEntity<ApiResponse<WalletModel>> topUp(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(walletService.topUp(userId, amount)));
    }

    @PostMapping("/{userId}/withdraw")
    @Operation(summary = "Withdraw wallet balance")
    public ResponseEntity<ApiResponse<WalletModel>> withdraw(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(walletService.withdraw(userId, amount)));
    }

    @PostMapping("/{userId}/hold")
    @Operation(summary = "Hold wallet balance")
    public ResponseEntity<ApiResponse<WalletHoldResponse>> holdBalance(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        WalletModel wallet = walletService.holdBalance(userId, amount);
        WalletHoldResponse response = new WalletHoldResponse(
                wallet.getAvailableBalance(),
                wallet.getHeldBalance()
        );
        return ResponseEntity.ok(ApiResponse.success("balance held", response));
    }

    @PostMapping("/{userId}/release")
    @Operation(summary = "Release held balance back to available")
    public ResponseEntity<ApiResponse<WalletModel>> releaseBalance(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(walletService.releaseBalance(userId, amount)));
    }

    @PostMapping("/{userId}/payment")
    @Operation(summary = "Convert held balance to payment")
    public ResponseEntity<ApiResponse<WalletModel>> convertHoldToPayment(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(walletService.convertHoldToPayment(userId, amount)));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List wallet transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionModel>>> getTransactions(
            @RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getTransactions(userId)));
    }

    @GetMapping("/me/transactions")
    @RequireLogin
    @Operation(summary = "List wallet transactions for current user")
    public ResponseEntity<ApiResponse<List<WalletTransactionModel>>> getMyTransactions() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getTransactions(authContext.getUserId())));
    }
}