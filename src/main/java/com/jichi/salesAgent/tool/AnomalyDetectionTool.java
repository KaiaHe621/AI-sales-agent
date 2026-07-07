package com.jichi.salesAgent.tool;

import com.jichi.salesAgent.dto.AnomalyDTO;
import com.jichi.salesAgent.entity.Product;
import com.jichi.salesAgent.entity.SalesRegion;
import com.jichi.salesAgent.entity.SalesRep;
import com.jichi.salesAgent.repository.ProductRepository;
import com.jichi.salesAgent.repository.SalesRegionRepository;
import com.jichi.salesAgent.repository.SalesRepRepository;
import com.jichi.salesAgent.service.SalesQueryService;
import com.jichi.salesAgent.security.UserContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionTool {

    private final SalesQueryService queryService;
    private final SalesRegionRepository regionRepository;
    private final SalesRepRepository repRepository;
    private final ProductRepository productRepository;

    @Value("${sales-agent.tool.anomaly-threshold-days:5}")
    private int zeroSaleThresholdDays;

    @Value("${sales-agent.tool.trend-drop-threshold:0.3}")
    private double trendDropThreshold;

    @Tool("自动检测销售数据中的所有异常，包括：大区订单量骤降、产品连续零销售、" +
         "销售员退单率异常、销售员业绩骤降。适用于：有没有异常、风险排查、预警检测等场景。" +
         "无需传入参数，系统根据当前用户权限自动扫描可见范围内的数据。")
    public String detectAllAnomalies() {

        UserContext.UserInfo user = UserContext.get();
        String role = user != null ? user.role() : "SALES_DIRECTOR";
        Long regionId = user != null ? user.regionId() : null;
        Long repId = user != null ? user.repId() : null;

        log.info("工具调用-detectAllAnomalies: role={}, regionId={}, repId={}", role, regionId, repId);

        List<AnomalyDTO> anomalies = new ArrayList<>();

        try {
            switch (role) {
                case "SALES_DIRECTOR" -> {
                    anomalies.addAll(detectRegionDropAnomalies(null));
                    anomalies.addAll(detectZeroSaleProducts(null));
                    anomalies.addAll(detectHighRefundReps(null));
                    anomalies.addAll(detectRepPerformanceDrop(null));
                }
                case "SALES_MANAGER" -> {
                    anomalies.addAll(detectRegionDropAnomalies(regionId));
                    anomalies.addAll(detectZeroSaleProducts(regionId));
                    anomalies.addAll(detectHighRefundReps(regionId));
                    anomalies.addAll(detectRepPerformanceDrop(regionId));
                }
                case "SALES_REP" -> {
                    anomalies.addAll(detectHighRefundForSelf(repId));
                    anomalies.addAll(detectPerformanceDropForSelf(repId));
                }
                default -> {
                    return "无法识别当前用户角色，请重新登录";
                }
            }
        } catch (Exception e) {
            log.error("异常检测出错", e);
            return "异常检测过程中出现问题，请稍后重试";
        }

        if (anomalies.isEmpty()) {
            return "当前数据未检测到明显异常，您可见范围内的销售数据运行正常。";
        }

        anomalies.sort((a, b) -> severityOrder(a.severity()) - severityOrder(b.severity()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("异常检测结果：共发现 %d 个异常\n\n", anomalies.size()));

        for (AnomalyDTO anomaly : anomalies) {
            String icon = switch (anomaly.severity()) {
                case "HIGH"   -> "🔴 高优先级";
                case "MEDIUM" -> "🟡 中优先级";
                default       -> "🔵 低优先级";
            };
            sb.append(String.format("%s｜%s\n", icon, anomaly.type()));
            sb.append(String.format("  对象：%s\n", anomaly.subject()));
            sb.append(String.format("  描述：%s\n", anomaly.description()));
            sb.append(String.format("  建议：%s\n\n", anomaly.suggestion()));
        }

        return sb.toString();
    }

    // 检测一：大区订单量骤降（filterRegionId 为 null 表示扫描全部大区）
    private List<AnomalyDTO> detectRegionDropAnomalies(Long filterRegionId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate recentStart = today.minusWeeks(2);
        LocalDate baseStart = today.minusWeeks(6);
        LocalDate baseEnd = today.minusWeeks(2).minusDays(1);

        List<SalesRegion> regions = filterRegionId != null
                ? regionRepository.findAllById(List.of(filterRegionId))
                : regionRepository.findAll();

        for (SalesRegion region : regions) {
            Long recentCount = queryService.queryOrderCount(region.getId(), recentStart, today);
            Long baseCount = queryService.queryOrderCount(region.getId(), baseStart, baseEnd);
            double baseAvg = baseCount / 2.0;
            if (baseAvg < 2) continue;

            double dropRate = (baseAvg - recentCount) / baseAvg;
            if (dropRate > trendDropThreshold) {
                String severity = dropRate > 0.6 ? "HIGH" : "MEDIUM";
                result.add(new AnomalyDTO("大区订单量骤降", severity, region.getName(),
                        String.format("近 2 周订单量 %d 笔，过去 4 周均值 %.1f 笔/两周，下降 %.0f%%",
                                recentCount, baseAvg, dropRate * 100),
                        "建议联系大区负责人确认原因"));
            }
        }
        return result;
    }

    // 检测二：产品连续零销售（按大区过滤）
    private List<AnomalyDTO> detectZeroSaleProducts(Long filterRegionId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Product product : productRepository.findByStatus("ACTIVE")) {
            LocalDate lastSaleDate = filterRegionId != null
                    ? queryService.queryLastOrderDateByRegion(product.getId(), filterRegionId)
                    : queryService.queryLastOrderDate(product.getId());
            if (lastSaleDate == null) continue;

            long days = ChronoUnit.DAYS.between(lastSaleDate, today);
            if (days >= zeroSaleThresholdDays) {
                String severity = days >= 14 ? "HIGH" : days >= 7 ? "MEDIUM" : "LOW";
                result.add(new AnomalyDTO("产品连续零销售", severity,
                        product.getName() + "（" + product.getSkuCode() + "）",
                        String.format("已连续 %d 天无销售订单", days),
                        "检查产品是否下架、库存是否充足"));
            }
        }
        return result;
    }

    // 检测三：销售员退单率异常（按大区过滤）
    private List<AnomalyDTO> detectHighRefundReps(Long filterRegionId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        List<Object[]> refundData = filterRegionId != null
                ? queryService.queryRefundRatesByRegion(start, end, filterRegionId)
                : queryService.queryRefundRates(start, end);

        for (Object[] row : refundData) {
            Long rid = ((Number) row[0]).longValue();
            long refunded = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            if (total < 3) continue;

            double refundRate = (double) refunded / total;
            if (refundRate > 0.15) {
                String repName = queryService.getRepName(rid);
                result.add(new AnomalyDTO("销售员退单率异常",
                        refundRate > 0.3 ? "HIGH" : "MEDIUM", repName,
                        String.format("近 30 天退单率 %.0f%%（%d/%d 单）", refundRate * 100, refunded, total),
                        "建议沟通了解原因"));
            }
        }
        return result;
    }

    // 检测四：销售员业绩骤降（按大区过滤）
    private List<AnomalyDTO> detectRepPerformanceDrop(Long filterRegionId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate curStart = today.minusDays(30);
        LocalDate prevStart = today.minusDays(60);
        LocalDate prevEnd = today.minusDays(31);

        List<SalesRep> reps = filterRegionId != null
                ? repRepository.findByRoleAndRegionId("SALES_REP", filterRegionId)
                : repRepository.findByRole("SALES_REP");

        for (SalesRep rep : reps) {
            BigDecimal current = queryService.queryTotalAmountByRep(rep.getId(), curStart, today);
            BigDecimal previous = queryService.queryTotalAmountByRep(rep.getId(), prevStart, prevEnd);
            if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) continue;
            if (current == null) current = BigDecimal.ZERO;

            double dropRate = previous.subtract(current)
                    .divide(previous, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (dropRate > 0.4) {
                result.add(new AnomalyDTO("销售员业绩骤降",
                        dropRate > 0.7 ? "HIGH" : "MEDIUM", rep.getName(),
                        String.format("近 30 天 ¥%.0f，上期 ¥%.0f，下降 %.0f%%", current, previous, dropRate * 100),
                        "建议跟进确认原因"));
            }
        }
        return result;
    }

    // 普通销售员专用：只检测自己的退单率
    private List<AnomalyDTO> detectHighRefundForSelf(Long repId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        long refunded = queryService.queryRefundCountByRep(repId, start, end);
        long total = queryService.queryOrderCountByRep(repId, start, end);
        if (total < 3) return result;

        double refundRate = (double) refunded / total;
        if (refundRate > 0.15) {
            result.add(new AnomalyDTO("您的退单率偏高",
                    refundRate > 0.3 ? "HIGH" : "MEDIUM", "本人",
                    String.format("近 30 天退单率 %.0f%%（%d/%d 单）", refundRate * 100, refunded, total),
                    "建议检查退单原因，是否有客户投诉需要跟进"));
        }
        return result;
    }

    // 普通销售员专用：只检测自己的业绩变化
    private List<AnomalyDTO> detectPerformanceDropForSelf(Long repId) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        BigDecimal current = queryService.queryTotalAmountByRep(repId, today.minusDays(30), today);
        BigDecimal previous = queryService.queryTotalAmountByRep(repId, today.minusDays(60), today.minusDays(31));

        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return result;
        if (current == null) current = BigDecimal.ZERO;

        double dropRate = previous.subtract(current)
                .divide(previous, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
        if (dropRate > 0.4) {
            result.add(new AnomalyDTO("您的业绩明显下滑",
                    dropRate > 0.7 ? "HIGH" : "MEDIUM", "本人",
                    String.format("近 30 天 ¥%.0f，上期 ¥%.0f，下降 %.0f%%", current, previous, dropRate * 100),
                    "建议主动梳理客户跟进情况，与主管沟通是否需要支持"));
        }
        return result;
    }

    private int severityOrder(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}