package com.jichi.langchain4j.controller.tools;

import com.jichi.langchain4j.service.tools.WeatherAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tool/weather")
public class WeatherController {

    private final WeatherAssistant weatherAssistant;

    public WeatherController(WeatherAssistant weatherAssistant) {
        this.weatherAssistant = weatherAssistant;
    }

    @GetMapping
    public String chat(@RequestParam String message,
                       @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId) {
        return weatherAssistant.chat(sessionId, message);
    }
}