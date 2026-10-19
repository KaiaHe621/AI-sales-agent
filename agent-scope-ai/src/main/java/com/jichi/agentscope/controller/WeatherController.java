package com.jichi.agentscope.controller;

import com.jichi.agentscope.runner.AgentFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final AgentFactory agentFactory;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String message) {
        ReActAgent agent = agentFactory.createWeatherAgent();
        Msg response = agent.call(
                Msg.builder().textContent(message).build()
        ).block();
        return ResponseEntity.ok(response.getTextContent());
    }
}