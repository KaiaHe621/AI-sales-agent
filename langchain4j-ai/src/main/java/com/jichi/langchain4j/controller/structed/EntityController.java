package com.jichi.langchain4j.controller.structed;

import com.jichi.langchain4j.model.NamedEntity;
import com.jichi.langchain4j.service.EntityExtractor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/structured/entity")
public class EntityController {

    private final EntityExtractor extractor;

    public EntityController(EntityExtractor extractor) {
        this.extractor = extractor;
    }

    @GetMapping
    public List<NamedEntity> extract(@RequestParam String text) {
        return extractor.extract(text);
    }
}