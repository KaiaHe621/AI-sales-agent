package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.model.TicketCategory;
import com.jichi.langchain4j.service.TicketClassifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/structured/ticket")
public class TicketController {

    private final TicketClassifier classifier;

    public TicketController(TicketClassifier classifier) {
        this.classifier = classifier;
    }

    @GetMapping
    public TicketCategory classify(@RequestParam String ticket) {
        return classifier.classify(ticket);
    }
}