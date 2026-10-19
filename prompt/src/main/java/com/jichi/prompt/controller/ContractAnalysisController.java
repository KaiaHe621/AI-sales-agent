package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContractRisk;
import com.jichi.prompt.service.ContractAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contract")
public class ContractAnalysisController {

    private final ContractAnalysisService contractAnalysisService;

    public ContractAnalysisController(ContractAnalysisService contractAnalysisService) {
        this.contractAnalysisService = contractAnalysisService;
    }

    @PostMapping("/analyze")
    public ContractRisk analyze(@RequestBody String clause) throws Exception {
        return contractAnalysisService.analyzeWithConsistency(clause);
    }

}