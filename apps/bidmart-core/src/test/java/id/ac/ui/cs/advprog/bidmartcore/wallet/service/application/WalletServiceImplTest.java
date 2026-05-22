package id.ac.ui.cs.advprog.bidmartcore.wallet.application.service;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.repository.WalletRepository;
import id.ac.ui.cs.advprog.bidmartcore.wallet.repository.WalletTransactionRepository;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private WalletModel wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        wallet = new WalletModel();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setAvailableBalance(BigDecimal.valueOf(10000));
        wallet.setHeldBalance(BigDecimal.ZERO);

        lenient().when(walletRepository.save(any(WalletModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void depositShouldIncreaseAvailableBalanceAndSaveTopUpTransaction() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletModel result = walletService.deposit(userId, BigDecimal.valueOf(5000));

        assertEquals(BigDecimal.valueOf(15000), result.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, result.getHeldBalance());

        ArgumentCaptor<WalletTransactionModel> captor =
                ArgumentCaptor.forClass(WalletTransactionModel.class);

        verify(transactionRepository).save(captor.capture());

        WalletTransactionModel transaction = captor.getValue();
        assertEquals(TransactionType.TOP_UP, transaction.getType());
        assertEquals(BigDecimal.valueOf(5000), transaction.getAmount());
        assertEquals(wallet.getId(), transaction.getWalletId());
    }

    @Test
    void withdrawShouldDecreaseAvailableBalanceAndSaveWithdrawTransaction() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletModel result = walletService.withdraw(userId, BigDecimal.valueOf(3000));

        assertEquals(BigDecimal.valueOf(7000), result.getAvailableBalance());

        ArgumentCaptor<WalletTransactionModel> captor =
                ArgumentCaptor.forClass(WalletTransactionModel.class);

        verify(transactionRepository).save(captor.capture());

        assertEquals(TransactionType.WITHDRAW, captor.getValue().getType());
        assertEquals(BigDecimal.valueOf(3000), captor.getValue().getAmount());
    }

    @Test
    void withdrawShouldThrowExceptionWhenBalanceIsInsufficient() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.withdraw(userId, BigDecimal.valueOf(20000))
        );

        assertEquals("Insufficient balance", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void holdBalanceShouldMoveAvailableToHeldBalance() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletModel result = walletService.holdBalance(userId, BigDecimal.valueOf(4000));

        assertEquals(BigDecimal.valueOf(6000), result.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(4000), result.getHeldBalance());

        ArgumentCaptor<WalletTransactionModel> captor =
                ArgumentCaptor.forClass(WalletTransactionModel.class);

        verify(transactionRepository).save(captor.capture());

        assertEquals(TransactionType.HOLD, captor.getValue().getType());
        assertEquals(BigDecimal.valueOf(4000), captor.getValue().getAmount());
    }

    @Test
    void releaseBalanceShouldMoveHeldBackToAvailableBalance() {
        wallet.setAvailableBalance(BigDecimal.valueOf(6000));
        wallet.setHeldBalance(BigDecimal.valueOf(4000));

        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletModel result = walletService.releaseBalance(userId, BigDecimal.valueOf(4000));

        assertEquals(BigDecimal.valueOf(10000), result.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, result.getHeldBalance());

        ArgumentCaptor<WalletTransactionModel> captor =
                ArgumentCaptor.forClass(WalletTransactionModel.class);

        verify(transactionRepository).save(captor.capture());

        assertEquals(TransactionType.RELEASE, captor.getValue().getType());
    }

    @Test
    void convertHoldToPaymentShouldDecreaseHeldBalance() {
        wallet.setAvailableBalance(BigDecimal.valueOf(6000));
        wallet.setHeldBalance(BigDecimal.valueOf(4000));

        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletModel result = walletService.convertHoldToPayment(userId, BigDecimal.valueOf(4000));

        assertEquals(BigDecimal.valueOf(6000), result.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, result.getHeldBalance());

        ArgumentCaptor<WalletTransactionModel> captor =
                ArgumentCaptor.forClass(WalletTransactionModel.class);

        verify(transactionRepository).save(captor.capture());

        assertEquals(TransactionType.PAYMENT, captor.getValue().getType());
    }

    @Test
    void transferHeldBalanceShouldMoveBuyerHeldBalanceToSellerAvailableBalance() {
        UUID sellerId = UUID.randomUUID();

        WalletModel buyerWallet = new WalletModel();
        buyerWallet.setId(UUID.randomUUID());
        buyerWallet.setUserId(userId);
        buyerWallet.setAvailableBalance(BigDecimal.valueOf(2000));
        buyerWallet.setHeldBalance(BigDecimal.valueOf(3000));

        WalletModel sellerWallet = new WalletModel();
        sellerWallet.setId(UUID.randomUUID());
        sellerWallet.setUserId(sellerId);
        sellerWallet.setAvailableBalance(BigDecimal.ZERO);
        sellerWallet.setHeldBalance(BigDecimal.ZERO);

        when(walletRepository.findByUserId(userId)).thenReturn(buyerWallet);
        when(walletRepository.findByUserId(sellerId)).thenReturn(sellerWallet);

        walletService.transferHeldBalance(userId, sellerId, BigDecimal.valueOf(3000));

        assertEquals(BigDecimal.ZERO, buyerWallet.getHeldBalance());
        assertEquals(BigDecimal.valueOf(3000), sellerWallet.getAvailableBalance());

        verify(walletRepository).save(buyerWallet);
        verify(walletRepository).save(sellerWallet);
        verify(transactionRepository, times(2)).save(any(WalletTransactionModel.class));
    }

    @Test
    void depositShouldRejectZeroAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.deposit(userId, BigDecimal.ZERO)
        );

        assertEquals("Deposit amount must be greater than zero", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }
}