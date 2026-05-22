package id.ac.ui.cs.advprog.bidmartcore.wallet.controller.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletDtoTest {

    @Test
    void walletHoldResponseShouldStoreValues() {
        WalletHoldResponse response = new WalletHoldResponse(
                BigDecimal.valueOf(7000),
                BigDecimal.valueOf(3000)
        );

        assertEquals(BigDecimal.valueOf(7000), response.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(3000), response.getHeldBalance());
    }
}