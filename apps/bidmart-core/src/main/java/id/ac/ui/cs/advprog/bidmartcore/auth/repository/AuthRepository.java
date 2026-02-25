package id.ac.ui.cs.advprog.bidmartcore.auth.repository;

import id.ac.ui.cs.advprog.bidmartcore.auth.model.AuthModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// TODO: rename atau modif file template ini
@Repository
public interface AuthRepository extends JpaRepository<AuthModel, UUID> {}
