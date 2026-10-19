package com.jichi.agentscope.controller;

import com.jichi.agentscope.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanService travelPlanService;

    @GetMapping("/plan")
    public Map<String, String> plan(
            @RequestParam String departure,
            @RequestParam String destination,
            @RequestParam String date) {

        String result = travelPlanService.plan(departure, destination, date);
        return Map.of("result", result);
    }
}