package com.jichi.langchain4j.controller.structed;

import com.jichi.langchain4j.model.ContactInfo;
import com.jichi.langchain4j.service.ContactExtractor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/structured/contact")
public class ContactController {

    private final ContactExtractor extractor;

    public ContactController(ContactExtractor extractor) {
        this.extractor = extractor;
    }

    @GetMapping
    public ContactInfo extract(@RequestParam String text) {
        return extractor.extract(text).orElse(null);
    }
}