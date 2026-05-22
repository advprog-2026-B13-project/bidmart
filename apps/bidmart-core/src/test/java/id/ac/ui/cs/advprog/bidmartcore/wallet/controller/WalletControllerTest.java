package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WalletService walletService;

    @Mock
    private AuthContext authContext;

    private UUID userId;
    private WalletModel wallet;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletController(walletService, authContext))
                .build();

        userId = UUID.randomUUID();
        wallet = createWallet(userId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
    }

    @Test
    void createWalletShouldReturnCreatedWallet() throws Exception {
        when(walletService.createWallet(userId)).thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/create/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userId.toString())))
                .andExpect(content().string(containsString("10000")));

        verify(walletService).createWallet(userId);
    }

    @Test
    void getWalletShouldReturnWalletByUserId() throws Exception {
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        mockMvc.perform(get("/api/wallet/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userId.toString())))
                .andExpect(content().string(containsString("10000")));

        verify(walletService).getWalletByUserId(userId);
    }

    @Test
    void getMyWalletShouldReturnAuthenticatedUserWallet() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        mockMvc.perform(get("/api/wallet/me"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userId.toString())))
                .andExpect(content().string(containsString("10000")));

        verify(authContext).getUserId();
        verify(walletService).getWalletByUserId(userId);
    }

    @Test
    void topUpShouldReturnUpdatedWallet() throws Exception {
        WalletModel updatedWallet = createWallet(userId, BigDecimal.valueOf(15000), BigDecimal.ZERO);

        when(walletService.topUp(eq(userId), eq(BigDecimal.valueOf(5000))))
                .thenReturn(updatedWallet);

        mockMvc.perform(post("/api/wallet/{userId}/topup", userId)
                        .param("amount", "5000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15000")));

        verify(walletService).topUp(userId, BigDecimal.valueOf(5000));
    }

    @Test
    void withdrawShouldReturnUpdatedWallet() throws Exception {
        WalletModel updatedWallet = createWallet(userId, BigDecimal.valueOf(7000), BigDecimal.ZERO);

        when(walletService.withdraw(eq(userId), eq(BigDecimal.valueOf(3000))))
                .thenReturn(updatedWallet);

        mockMvc.perform(post("/api/wallet/{userId}/withdraw", userId)
                        .param("amount", "3000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("7000")));

        verify(walletService).withdraw(userId, BigDecimal.valueOf(3000));
    }

    @Test
    void holdBalanceShouldReturnWalletHoldResponse() throws Exception {
        WalletModel updatedWallet = createWallet(userId, BigDecimal.valueOf(7000), BigDecimal.valueOf(3000));

        when(walletService.holdBalance(eq(userId), eq(BigDecimal.valueOf(3000))))
                .thenReturn(updatedWallet);

        mockMvc.perform(post("/api/wallet/{userId}/hold", userId)
                        .param("amount", "3000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("balance held")))
                .andExpect(content().string(containsString("7000")))
                .andExpect(content().string(containsString("3000")));

        verify(walletService).holdBalance(userId, BigDecimal.valueOf(3000));
    }

    @Test
    void releaseBalanceShouldReturnUpdatedWallet() throws Exception {
        WalletModel updatedWallet = createWallet(userId, BigDecimal.valueOf(10000), BigDecimal.ZERO);

        when(walletService.releaseBalance(eq(userId), eq(BigDecimal.valueOf(3000))))
                .thenReturn(updatedWallet);

        mockMvc.perform(post("/api/wallet/{userId}/release", userId)
                        .param("amount", "3000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10000")))
                .andExpect(content().string(containsString("0")));

        verify(walletService).releaseBalance(userId, BigDecimal.valueOf(3000));
    }

    @Test
    void convertHoldToPaymentShouldReturnUpdatedWallet() throws Exception {
        WalletModel updatedWallet = createWallet(userId, BigDecimal.valueOf(7000), BigDecimal.ZERO);

        when(walletService.convertHoldToPayment(eq(userId), eq(BigDecimal.valueOf(3000))))
                .thenReturn(updatedWallet);

        mockMvc.perform(post("/api/wallet/{userId}/payment", userId)
                        .param("amount", "3000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("7000")))
                .andExpect(content().string(containsString("0")));

        verify(walletService).convertHoldToPayment(userId, BigDecimal.valueOf(3000));
    }

    @Test
    void getTransactionsShouldReturnTransactionList() throws Exception {
        WalletTransactionModel transaction = new WalletTransactionModel(
                UUID.randomUUID(),
                wallet.getId(),
                TransactionType.TOP_UP,
                BigDecimal.valueOf(10000),
                null,
                null
        );

        when(walletService.getTransactions(userId)).thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOP_UP")))
                .andExpect(content().string(containsString("10000")));

        verify(walletService).getTransactions(userId);
    }

    @Test
    void getMyTransactionsShouldReturnAuthenticatedUserTransactions() throws Exception {
        WalletTransactionModel transaction = new WalletTransactionModel(
                UUID.randomUUID(),
                wallet.getId(),
                TransactionType.WITHDRAW,
                BigDecimal.valueOf(3000),
                null,
                null
        );

        when(authContext.getUserId()).thenReturn(userId);
        when(walletService.getTransactions(userId)).thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/wallet/me/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WITHDRAW")))
                .andExpect(content().string(containsString("3000")));

        verify(authContext).getUserId();
        verify(walletService).getTransactions(userId);
    }

    private WalletModel createWallet(UUID userId, BigDecimal availableBalance, BigDecimal heldBalance) {
        WalletModel wallet = new WalletModel();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setAvailableBalance(availableBalance);
        wallet.setHeldBalance(heldBalance);
        return wallet;
    }
}