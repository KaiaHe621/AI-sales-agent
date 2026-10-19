package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContactInfo;
import com.jichi.prompt.service.BulkExtractionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/extract")
public class BulkExtractionController {

    private final BulkExtractionService bulkExtractionService;

    public BulkExtractionController(BulkExtractionService bulkExtractionService) {
        this.bulkExtractionService = bulkExtractionService;
    }

    @PostMapping("/contacts/batch")
    public List<ContactInfo> extractContacts(@RequestBody List<String> texts) {
        return bulkExtractionService.extractBatch(
                texts,
                ContactInfo.class,
                "从以下文本中提取联系人信息，只提取明确出现的字段，缺失的填 null。"
        );
    }
}