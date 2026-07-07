package com.jichi.salesAgent.security;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ToolInputValidator {

    private static final Set<String> VALID_REGIONS =
            Set.of("华东区", "华南区", "华北区", "西南区");

    private static final Set<String> VALID_CHART_TYPES =
            Set.of("line", "bar", "pie");

    private static final Set<String> VALID_DIMENSIONS =
            Set.of("region", "rep", "category");

    // 日期格式校验
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public String validateDate(String dateStr) {
        if (dateStr == null || !DATE_PATTERN.matcher(dateStr).matches()) {
            throw new IllegalArgumentException("无效的日期格式，请使用 yyyy-MM-dd");
        }
        try {
            LocalDate.parse(dateStr);
            return dateStr;
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的日期：" + dateStr);
        }
    }

    public String validateRegionName(String regionName) {
        if (regionName == null || regionName.isBlank()) return null;
        if (!VALID_REGIONS.contains(regionName)) {
            throw new IllegalArgumentException("无效的大区名称：" + regionName +
                    "，有效值为：" + VALID_REGIONS);
        }
        return regionName;
    }

    public String validateChartType(String chartType) {
        if (!VALID_CHART_TYPES.contains(chartType)) {
            throw new IllegalArgumentException("无效的图表类型：" + chartType +
                    "，有效值为：line/bar/pie");
        }
        return chartType;
    }

    public String validateDimension(String dimension) {
        if (!VALID_DIMENSIONS.contains(dimension)) {
            throw new IllegalArgumentException("无效的维度：" + dimension +
                    "，有效值为：region/rep/category");
        }
        return dimension;
    }

    public int validateTopN(int topN) {
        return Math.min(Math.max(Math.abs(topN), 1), 20);
    }
}