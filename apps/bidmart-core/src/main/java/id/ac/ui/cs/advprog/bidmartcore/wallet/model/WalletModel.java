package id.ac.ui.cs.advprog.bidmartcore.wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallet")
public class WalletModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal heldBalance = BigDecimal.ZERO;

    public BigDecimal getBalance() {
        return availableBalance.add(heldBalance);
    }

    public void setAvailableBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available balance cannot be negative");
        }
        this.availableBalance = amount;
    }

    public void setHeldBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Held balance cannot be negative");
        }
        this.heldBalance = amount;
    }
}