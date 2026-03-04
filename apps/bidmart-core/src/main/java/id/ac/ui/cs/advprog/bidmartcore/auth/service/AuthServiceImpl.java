package id.ac.ui.cs.advprog.bidmartcore.auth.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.model.AuthModel;
import id.ac.ui.cs.advprog.bidmartcore.auth.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// TODO: rename atau modif file template ini
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private AuthRepository authRepository;

    @Override
    public List<AuthModel> findAll() {
        return authRepository.findAll();
    }
}
