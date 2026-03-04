package id.ac.ui.cs.advprog.bidmartcore.auth.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.model.AuthModel;
import id.ac.ui.cs.advprog.bidmartcore.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// TODO: rename atau modif file template ini
@RestController
@RequestMapping("/api/auth")
public class AuthController{
    @Autowired
    private AuthService authService;

    @GetMapping("/all")
    public Map<String, Object> getAll() {
        List<AuthModel> authDatas = authService.findAll();

        return Map.of(
                "results", authDatas
        );
    }
}
