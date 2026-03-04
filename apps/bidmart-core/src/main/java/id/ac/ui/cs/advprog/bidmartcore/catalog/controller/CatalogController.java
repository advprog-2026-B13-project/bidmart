package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.CatalogModel;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// TODO: rename atau modif file template ini
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    @Autowired
    private CatalogService catalogService;

    @GetMapping("/all")
    public Map<String, Object> getAll() {
        List<CatalogModel> catalogDatas = catalogService.findAll();

        return Map.of(
                "results", catalogDatas
        );
    }
}
