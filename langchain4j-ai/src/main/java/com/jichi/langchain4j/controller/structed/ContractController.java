package com.jichi.langchain4j.controller.structed;

import com.jichi.langchain4j.model.ContractInfo;
import com.jichi.langchain4j.service.ContractExtractor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/structured/contract")
public class ContractController {

    private final ContractExtractor extractor;

    public ContractController(ContractExtractor extractor) {
        this.extractor = extractor;
    }

    @PostMapping
    public ContractInfo extract(@RequestBody String contractText) {
        return extractor.extract(contractText);
    }
}