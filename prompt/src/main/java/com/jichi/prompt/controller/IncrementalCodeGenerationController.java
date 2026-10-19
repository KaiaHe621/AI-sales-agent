package com.jichi.prompt.controller;

import com.jichi.prompt.service.IncrementalCodeGenerationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/code-gen")
public class IncrementalCodeGenerationController {

    private final IncrementalCodeGenerationService service;

    public IncrementalCodeGenerationController(IncrementalCodeGenerationService service) {
        this.service = service;
    }

    @PostMapping("/incremental")
    public Map<String, String> generate(@RequestBody String projectDescription) {
        return service.generateProjectIncrementally(projectDescription);
    }
}