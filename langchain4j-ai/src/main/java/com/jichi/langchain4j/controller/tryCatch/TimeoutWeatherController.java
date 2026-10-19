package com.jichi.langchain4j.controller.tryCatch;

import com.jichi.langchain4j.service.tools.TimeoutWeatherAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tool/timeout-weather")
public class TimeoutWeatherController {

    private final TimeoutWeatherAssistant timeoutWeatherAssistant;

    public TimeoutWeatherController(TimeoutWeatherAssistant timeoutWeatherAssistant) {
        this.timeoutWeatherAssistant = timeoutWeatherAssistant;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return timeoutWeatherAssistant.chat(message);
    }
}