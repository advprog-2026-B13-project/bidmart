package id.ac.ui.cs.advprog.bidmartcore.wallet.repository;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// TODO: rename atau modif file template ini
@Repository
public interface WalletSpringRepository extends JpaRepository<WalletModel, UUID> {}
