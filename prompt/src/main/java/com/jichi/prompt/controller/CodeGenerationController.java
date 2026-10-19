package com.jichi.prompt.controller;

import com.jichi.prompt.entity.GeneratedCode;
import com.jichi.prompt.service.CodeGenerationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/code-gen")
public class CodeGenerationController {

    private final CodeGenerationService codeGenerationService;

    public CodeGenerationController(CodeGenerationService codeGenerationService) {
        this.codeGenerationService = codeGenerationService;
    }

    record CrudRequest(String entityDescription, String entityFields) {}

    @PostMapping("/crud")
    public GeneratedCode generateCrud(@RequestBody CrudRequest req) {
        return codeGenerationService.generateCrud(req.entityDescription(), req.entityFields());
    }
}