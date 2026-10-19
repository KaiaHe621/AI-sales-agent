package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContractInfo;
import com.jichi.prompt.service.ContractExtractionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract")
public class ContractExtractionController {

    private final ContractExtractionService contractExtractionService;

    public ContractExtractionController(ContractExtractionService contractExtractionService) {
        this.contractExtractionService = contractExtractionService;
    }

    @PostMapping("/contract")
    public ContractInfo extractContract(@RequestBody String contractText) {
        return contractExtractionService.extract(contractText);
    }
}