package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.CatalogModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: rename atau modif file template ini
@Component
@RequiredArgsConstructor
public class CatalogRepositoryImpl implements CatalogRepository {
    private final CatalogSpringRepository springRepository;

    @Override
    public List<CatalogModel> findAll() {
        return springRepository.findAll();
    }
}
