package id.ac.ui.cs.advprog.bidmartcore.auth.repository;

import id.ac.ui.cs.advprog.bidmartcore.auth.model.AuthModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: rename atau modif file template ini
@Component
@RequiredArgsConstructor
public class AuthRepositoryImpl implements AuthRepository {
    private final AuthSpringRepository springRepository;

    @Override
    public List<AuthModel> findAll() {
        return springRepository.findAll();
    }
}
