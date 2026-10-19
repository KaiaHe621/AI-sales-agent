package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContractInfo;
import com.jichi.prompt.service.LongDocumentExtractionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract")
public class LongDocumentExtractionController {

    private final LongDocumentExtractionService service;

    public LongDocumentExtractionController(LongDocumentExtractionService service) {
        this.service = service;
    }

    @PostMapping("/contract/long")
    public ContractInfo extractLongContract(@RequestBody String contractText) {
        return service.extractLongDocument(contractText);
    }
}