package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.CatalogModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// TODO: rename atau modif file template ini
@Repository
public interface CatalogSpringRepository extends JpaRepository<CatalogModel, UUID> {}
