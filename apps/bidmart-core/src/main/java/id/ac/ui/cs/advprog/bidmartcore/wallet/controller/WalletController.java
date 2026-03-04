package id.ac.ui.cs.advprog.bidmartcore.wallet.controller;

import id.ac.ui.cs.advprog.bidmartcore.wallet.model.WalletModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// TODO: rename atau modif file template ini
@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;

    @GetMapping("/all")
    public Map<String, Object> getAll() {
        List<WalletModel> walletDatas = walletService.findAll();

        return Map.of(
                "results", walletDatas
        );
    }
}
