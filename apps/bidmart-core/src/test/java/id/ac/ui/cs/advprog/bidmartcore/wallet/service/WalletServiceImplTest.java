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
import java.util.List;

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

    @Test
    void getWalletByUserIdShouldCreateWalletWhenWalletDoesNotExist() {
        UUID newUserId = UUID.randomUUID();

        when(walletRepository.findByUserId(newUserId))
                .thenThrow(new RuntimeException("Wallet not found"));

        WalletModel result = walletService.getWalletByUserId(newUserId);

        assertEquals(newUserId, result.getUserId());
        assertEquals(BigDecimal.ZERO, result.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, result.getHeldBalance());

        verify(walletRepository).save(any(WalletModel.class));
    }

    @Test
    void getTransactionsShouldReturnWalletTransactions() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        WalletTransactionModel transaction = new WalletTransactionModel(
                UUID.randomUUID(),
                wallet.getId(),
                TransactionType.TOP_UP,
                BigDecimal.valueOf(5000),
                null,
                null
        );

        when(transactionRepository.findByWalletId(wallet.getId()))
                .thenReturn(List.of(transaction));

        List<WalletTransactionModel> result = walletService.getTransactions(userId);

        assertEquals(1, result.size());
        assertEquals(TransactionType.TOP_UP, result.get(0).getType());
        assertEquals(BigDecimal.valueOf(5000), result.get(0).getAmount());
    }

    @Test
    void holdBalanceShouldRejectZeroAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.holdBalance(userId, BigDecimal.ZERO)
        );

        assertEquals("Hold amount must be greater than zero", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void releaseBalanceShouldRejectZeroAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.releaseBalance(userId, BigDecimal.ZERO)
        );

        assertEquals("Release amount must be greater than zero", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void convertHoldToPaymentShouldRejectZeroAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.convertHoldToPayment(userId, BigDecimal.ZERO)
        );

        assertEquals("Payment amount must be greater than zero", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void holdBalanceShouldThrowExceptionWhenAvailableBalanceIsInsufficient() {
        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.holdBalance(userId, BigDecimal.valueOf(20000))
        );

        assertEquals("Insufficient available balance", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void releaseBalanceShouldThrowExceptionWhenHeldBalanceIsInsufficient() {
        wallet.setAvailableBalance(BigDecimal.valueOf(5000));
        wallet.setHeldBalance(BigDecimal.valueOf(1000));

        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.releaseBalance(userId, BigDecimal.valueOf(3000))
        );

        assertEquals("Insufficient held balance", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void convertHoldToPaymentShouldThrowExceptionWhenHeldBalanceIsInsufficient() {
        wallet.setAvailableBalance(BigDecimal.valueOf(5000));
        wallet.setHeldBalance(BigDecimal.valueOf(1000));

        when(walletRepository.findByUserId(userId)).thenReturn(wallet);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.convertHoldToPayment(userId, BigDecimal.valueOf(3000))
        );

        assertEquals("Insufficient held balance", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferHeldBalanceShouldThrowExceptionWhenBuyerHeldBalanceIsInsufficient() {
        UUID sellerId = UUID.randomUUID();

        WalletModel buyerWallet = new WalletModel();
        buyerWallet.setId(UUID.randomUUID());
        buyerWallet.setUserId(userId);
        buyerWallet.setAvailableBalance(BigDecimal.valueOf(2000));
        buyerWallet.setHeldBalance(BigDecimal.valueOf(1000));

        WalletModel sellerWallet = new WalletModel();
        sellerWallet.setId(UUID.randomUUID());
        sellerWallet.setUserId(sellerId);
        sellerWallet.setAvailableBalance(BigDecimal.ZERO);
        sellerWallet.setHeldBalance(BigDecimal.ZERO);

        when(walletRepository.findByUserId(userId)).thenReturn(buyerWallet);
        when(walletRepository.findByUserId(sellerId)).thenReturn(sellerWallet);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.transferHeldBalance(userId, sellerId, BigDecimal.valueOf(3000))
        );

        assertEquals("Insufficient held balance", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }
}