package com.jichi.prompt.controller;

import com.jichi.prompt.config.InputSanitizer;
import com.jichi.prompt.entity.SanitizeResult;
import com.jichi.prompt.entity.ScanResult;
import com.jichi.prompt.service.DocumentSecurityScanner;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/security")
public class SecurityController {

    private final InputSanitizer inputSanitizer;
    private final DocumentSecurityScanner documentSecurityScanner;

    public SecurityController(InputSanitizer inputSanitizer,
                               DocumentSecurityScanner documentSecurityScanner) {
        this.inputSanitizer = inputSanitizer;
        this.documentSecurityScanner = documentSecurityScanner;
    }

    /** 用户输入注入检测 */
    @PostMapping("/sanitize")
    public SanitizeResult sanitize(@RequestBody String input) {
        return inputSanitizer.sanitize(input);
    }

    /** 文档间接注入扫描 */
    @PostMapping("/scan-document")
    public ScanResult scanDocument(@RequestBody String content) {
        return documentSecurityScanner.scanDocument(content);
    }
}