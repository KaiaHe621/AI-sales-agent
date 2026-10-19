package com.jichi.prompt.controller;

import com.jichi.prompt.service.TicketClassificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticket")
public class TicketClassificationController {

    private final TicketClassificationService classificationService;

    public TicketClassificationController(TicketClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @GetMapping("/classify")
    public String classify(@RequestParam String ticket) {
        return classificationService.classify(ticket);
    }
}