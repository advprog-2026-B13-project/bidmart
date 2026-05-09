    package id.ac.ui.cs.advprog.bidmartcore.wallet.service;

    import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
    import id.ac.ui.cs.advprog.bidmartcore.wallet.repository.WalletRepository;
    import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletTransactionModel;
    import id.ac.ui.cs.advprog.bidmartcore.wallet.repository.WalletTransactionRepository;
    import id.ac.ui.cs.advprog.bidmartcore.wallet.model.TransactionType;
    import org.springframework.transaction.annotation.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    import java.math.BigDecimal;
    import java.util.List;
    import java.util.UUID;
    import java.time.LocalDateTime;

    @Service
    @RequiredArgsConstructor
    public class WalletServiceImpl implements WalletService {

        private final WalletRepository walletRepository;
        private final WalletTransactionRepository transactionRepository;

        private WalletModel getOrCreateWallet(UUID userId) {
            try {
                return walletRepository.findByUserId(userId);
            } catch (Exception e) {
                return createWallet(userId);
            }
        }

        @Override
        public WalletModel createWallet(UUID userId) {
            WalletModel wallet = new WalletModel();
            wallet.setUserId(userId);
            wallet.setAvailableBalance(BigDecimal.ZERO);
            wallet.setHeldBalance(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        }

        @Override
        public WalletModel getWalletByUserId(UUID userId) {
            return walletRepository.findByUserId(userId);
        }

        @Override
        public WalletModel topUp(UUID userId, BigDecimal amount) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Top up amount must be greater than zero");
            }
            WalletModel wallet = walletRepository.findByUserId(userId);
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            return walletRepository.save(wallet);
        }

        @Override
        public List<WalletModel> findAll() {
            return walletRepository.findAll();
        }

        @Override
        @Transactional
        public WalletModel withdraw(UUID userId, BigDecimal amount) {
            WalletModel wallet = walletRepository.findByUserId(userId);

            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().subtract(amount)
            );

            walletRepository.save(wallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            wallet.getId(),
                            TransactionType.WITHDRAW,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            return wallet;
        }

        @Override
        public List<WalletTransactionModel> getTransactions(UUID userId) {
            WalletModel wallet = walletRepository.findByUserId(userId);
            return transactionRepository.findByWalletId(wallet.getId());
        }

        @Override
        @Transactional
        public WalletModel convertHoldToPayment(UUID userId, BigDecimal amount) {
            WalletModel wallet = walletRepository.findByUserId(userId);

            if (wallet.getHeldBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient held balance");
            }

            wallet.setHeldBalance(
                    wallet.getHeldBalance().subtract(amount)
            );

            walletRepository.save(wallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            wallet.getId(),
                            TransactionType.PAYMENT,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            return wallet;
        }

        @Override
        @Transactional
        public WalletModel releaseBalance(UUID userId, BigDecimal amount) {
            WalletModel wallet = walletRepository.findByUserId(userId);

            if (wallet.getHeldBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient held balance");
            }

            wallet.setHeldBalance(
                    wallet.getHeldBalance().subtract(amount)
            );

            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(amount)
            );

            walletRepository.save(wallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            wallet.getId(),
                            TransactionType.RELEASE,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            return wallet;
        }

        @Override
        @Transactional
        public WalletModel holdBalance(UUID userId, BigDecimal amount) {
            WalletModel wallet = walletRepository.findByUserId(userId);

            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient available balance");
            }

            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().subtract(amount)
            );

            wallet.setHeldBalance(
                    wallet.getHeldBalance().add(amount)
            );

            walletRepository.save(wallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            wallet.getId(),
                            TransactionType.HOLD,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            return wallet;
        }

        @Override
        @Transactional
        public WalletModel deposit(UUID userId, BigDecimal amount) {

            WalletModel wallet = getOrCreateWallet(userId);

            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(amount)
            );

            walletRepository.save(wallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            wallet.getId(),
                            TransactionType.TOP_UP,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            return wallet;
        }

        @Override
        @Transactional
        public void transferHeldBalance(
                UUID buyerId,
                UUID sellerId,
                BigDecimal amount
        ) {

            WalletModel buyerWallet = walletRepository.findByUserId(buyerId);

            WalletModel sellerWallet = getOrCreateWallet(sellerId);

            if (buyerWallet.getHeldBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient held balance");
            }

            buyerWallet.setHeldBalance(
                    buyerWallet.getHeldBalance().subtract(amount)
            );

            sellerWallet.setAvailableBalance(
                    sellerWallet.getAvailableBalance().add(amount)
            );

            walletRepository.save(buyerWallet);
            walletRepository.save(sellerWallet);

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            buyerWallet.getId(),
                            TransactionType.PAYMENT,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );

            transactionRepository.save(
                    new WalletTransactionModel(
                            null,
                            sellerWallet.getId(),
                            TransactionType.TOP_UP,
                            amount,
                            null,
                            LocalDateTime.now()
                    )
            );
        }

    }