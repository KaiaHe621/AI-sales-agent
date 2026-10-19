package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.entity.ProductEntity;
import com.jichi.voiceshopping.repository.ProductRepository;
import com.jichi.voiceshopping.service.ProductVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductVectorService vector;
    private final ProductRepository repo;

    @GetMapping
    public List<ProductEntity> search(@RequestParam String q,
                                      @RequestParam(required = false) Integer budget) {
        String filter = null;
        List<Object> params = new java.util.ArrayList<>();
        if (budget != null) {
            filter = "price <= ?";
            params.add(budget);
        }
        List<Long> ids = vector.search(q, filter, params, 5);
        return repo.findByIdIn(ids);
    }
}