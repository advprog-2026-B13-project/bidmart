package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.CatalogModel;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// TODO: rename atau modif file template ini
@Service
public class CatalogServiceImpl implements CatalogService {
    @Autowired
    private CatalogRepository catalogRepository;

    @Override
    public List<CatalogModel> findAll() {
        return catalogRepository.findAll();
    }
}
