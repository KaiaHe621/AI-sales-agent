package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContactInfo;
import com.jichi.prompt.entity.ValidatedResult;
import com.jichi.prompt.entity.ValidationResult;
import com.jichi.prompt.service.ContactExtractionService;
import com.jichi.prompt.service.ExtractionValidator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract")
public class ValidatedContactController {

    private final ContactExtractionService extractionService;
    private final ExtractionValidator validator;

    public ValidatedContactController(ContactExtractionService extractionService,
                                      ExtractionValidator validator) {
        this.extractionService = extractionService;
        this.validator = validator;
    }

    @PostMapping("/contact/validated")
    public ValidatedResult extractAndValidate(@RequestBody String text) {
        ContactInfo info = extractionService.extract(text);
        ValidationResult result = validator.validate(info);
        return new ValidatedResult(info, result);
    }
}