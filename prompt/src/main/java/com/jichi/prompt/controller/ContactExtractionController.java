package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ContactInfo;
import com.jichi.prompt.service.ContactExtractionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/extract")
public class ContactExtractionController {

    private final ContactExtractionService contactExtractionService;

    public ContactExtractionController(ContactExtractionService contactExtractionService) {
        this.contactExtractionService = contactExtractionService;
    }

    @PostMapping("/contact")
    public ContactInfo extractContact(@RequestBody String text) {
        return contactExtractionService.extract(text);
    }
}