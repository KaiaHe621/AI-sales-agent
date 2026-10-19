package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlightSearchTool {

    @Tool(name = "search_flights", description = "查询指定日期的可用航班列表")
    public FlightResult searchFlights(
            @ToolParam(name = "departure", description = "出发城市，如：北京、上海") String departure,
            @ToolParam(name = "arrival",   description = "到达城市，如：广州、成都") String arrival,
            @ToolParam(name = "date",      description = "出发日期，格式 yyyy-MM-dd") String date,
            @ToolParam(name = "cabin",     description = "舱位等级：economy（经济舱）/ business（商务舱），默认经济舱", required = false) String cabin
    ) {
        String cabinLabel = "business".equalsIgnoreCase(cabin) ? "商务舱" : "经济舱";
        return new FlightResult(departure, arrival, date, cabinLabel,
                List.of(
                        new Flight("CA1234", "08:00", "10:30", 680),
                        new Flight("MU5678", "12:15", "14:45", 520),
                        new Flight("CZ9012", "18:30", "21:00", 450)
                ));
    }

    public record FlightResult(
            String departure,
            String arrival,
            String date,
            String cabin,
            List<Flight> flights
    ) {}

    public record Flight(String flightNo, String departTime, String arriveTime, int price) {}
}