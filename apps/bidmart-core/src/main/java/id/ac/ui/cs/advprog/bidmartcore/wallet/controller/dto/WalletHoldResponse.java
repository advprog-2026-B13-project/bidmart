package id.ac.ui.cs.advprog.bidmartcore.wallet.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Wallet hold balance response")
public class WalletHoldResponse {

    @Schema(description = "Available balance after hold")
    private BigDecimal availableBalance;

    @Schema(description = "Held balance after hold")
    private BigDecimal heldBalance;
}

