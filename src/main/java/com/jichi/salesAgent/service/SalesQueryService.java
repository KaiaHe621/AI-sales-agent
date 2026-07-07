package com.jichi.salesAgent.service;

import com.jichi.salesAgent.dto.*;
import com.jichi.salesAgent.entity.SalesOrder;
import com.jichi.salesAgent.entity.SalesRep;
import com.jichi.salesAgent.repository.*;
import com.jichi.salesAgent.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesQueryService {

    private final SalesOrderRepository orderRepository;
    private final SalesRepRepository repRepository;
    private final ProductRepository productRepository;
    private final SalesRegionRepository regionRepository;

    // ============================================================
    // 基础查询
    // ============================================================

    public List<SalesOrder> queryOrders(Long repId, Long regionId,
                                        LocalDate start, LocalDate end) {
        UserContext.UserInfo currentUser = UserContext.get();
        if (currentUser != null) {
            if ("SALES_REP".equals(currentUser.role())) {
                // 普通销售员只能查自己的订单
                repId = currentUser.repId();
            } else if ("SALES_MANAGER".equals(currentUser.role())) {
                // 主管只能查本大区（若传入的 regionId 不是自己管辖的大区，强制覆盖）
                if (regionId == null || !regionId.equals(currentUser.regionId())) {
                    regionId = currentUser.regionId();
                }
            }
            // SALES_DIRECTOR：不限制，查询范围由传入参数决定
        }
        return doQueryOrders(repId, regionId, start, end);
    }

    public List<SalesOrder> doQueryOrders(Long repId, Long regionId,
                                          LocalDate start, LocalDate end) {
        if (repId != null) {
            return orderRepository.findByRepIdAndOrderDateBetween(repId, start, end);
        }
        if (regionId != null) {
            return orderRepository.findByRegionIdAndOrderDateBetween(regionId, start, end);
        }
        return orderRepository.findAll().stream()
                .filter(o -> !o.getOrderDate().isBefore(start) && !o.getOrderDate().isAfter(end))
                .collect(Collectors.toList());
    }

    public BigDecimal queryTotalAmount(Long regionId, LocalDate start, LocalDate end) {
        if (regionId != null) {
            return orderRepository.sumAmountByRegion(regionId, start, end);
        }
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus().equals("COMPLETED"))
                .filter(o -> !o.getOrderDate().isBefore(start) && !o.getOrderDate().isAfter(end))
                .map(SalesOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ============================================================
    // 排名查询
    // ============================================================

    // 排名数据缓存 5 分钟
    @Cacheable(value = "rep-ranking",
            key = "#start.toString() + '_' + #end.toString() + '_' + #topN")
    public List<RepSalesDTO> queryRepRanking(LocalDate start, LocalDate end, int topN) {
        log.debug("查询销售员排名（未命中缓存）: start={}, end={}", start, end);
        List<Object[]> raw = orderRepository.findRepRanking(start, end);

        Map<Long, SalesRep> repMap = repRepository.findAll().stream()
                .collect(Collectors.toMap(SalesRep::getId, r -> r));
        Map<Long, String> regionNameMap = regionRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId(), r -> r.getName()));

        List<RepSalesDTO> result = new ArrayList<>();
        for (Object[] row : raw) {
            Long repId = ((Number) row[0]).longValue();
            BigDecimal total = new BigDecimal(row[1].toString());
            SalesRep rep = repMap.get(repId);
            if (rep == null) continue;

            String regionName = regionNameMap.getOrDefault(rep.getRegionId(), "未知");
            result.add(new RepSalesDTO(repId, rep.getName(), rep.getRegionId(),
                    regionName, total, 0));

            if (result.size() >= topN) break;
        }
        return result;
    }

    @Cacheable(value = "region-ranking",
            key = "#start.toString() + '_' + #end.toString()")
    public List<RegionSalesDTO> queryRegionRanking(LocalDate start, LocalDate end) {
        log.debug("查询大区排名（未命中缓存）");
        List<Object[]> raw = orderRepository.findRegionRanking(start, end);
        Map<Long, String> regionNameMap = regionRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId(), r -> r.getName()));

        return raw.stream().map(row -> {
            Long regionId = ((Number) row[0]).longValue();
            BigDecimal total = new BigDecimal(row[1].toString());
            String regionName = regionNameMap.getOrDefault(regionId, "未知");
            return new RegionSalesDTO(regionId, regionName, total, 0, BigDecimal.ZERO);
        }).collect(Collectors.toList());
    }

    public List<ProductSalesDTO> queryProductRanking(LocalDate start, LocalDate end, int topN) {
        List<Object[]> raw = orderRepository.findProductRanking(start, end);
        Map<Long, com.jichi.salesAgent.entity.Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p));

        List<ProductSalesDTO> result = new ArrayList<>();
        for (Object[] row : raw) {
            Long productId = ((Number) row[0]).longValue();
            BigDecimal total = new BigDecimal(row[1].toString());
            Integer qty = ((Number) row[2]).intValue();
            com.jichi.salesAgent.entity.Product p = productMap.get(productId);
            if (p == null) continue;
            result.add(new ProductSalesDTO(productId, p.getSkuCode(), p.getName(),
                    p.getCategory(), total, qty));
            if (result.size() >= topN) break;
        }
        return result;
    }

    // ============================================================
    // 趋势分析
    // ============================================================

    @Cacheable(value = "monthly-trend",
            key = "(#regionId == null ? 'all' : #regionId.toString()) + '_' + #months")
    public List<MonthlyTrendDTO> queryMonthlyTrend(Long regionId, int months) {
        log.debug("查询月度趋势（未命中缓存）: regionId={}, months={}", regionId, months);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months).withDayOfMonth(1);

        List<Object[]> raw = orderRepository.findMonthlyTrend(regionId, start, end);
        return raw.stream().map(row -> new MonthlyTrendDTO(
                row[0].toString(),
                new BigDecimal(row[1].toString()),
                ((Number) row[2]).intValue()
        )).collect(Collectors.toList());
    }

    public BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 异常检测辅助
    // ============================================================

    public LocalDate queryLastOrderDate(Long productId) {
        return orderRepository.findLastOrderDateByProduct(productId);
    }

    public Long queryOrderCount(Long regionId, LocalDate start, LocalDate end) {
        return orderRepository.countCompletedByRegion(regionId, start, end);
    }

    public List<Object[]> queryRefundRates(LocalDate start, LocalDate end) {
        return orderRepository.findRefundRateByRep(start, end);
    }

    // ============================================================
    // 辅助查询（名称解析）
    // ============================================================

    public String getRepName(Long repId) {
        return repRepository.findById(repId)
                .map(SalesRep::getName)
                .orElse("未知销售员");
    }

    public String getRegionName(Long regionId) {
        return regionRepository.findById(regionId)
                .map(r -> r.getName())
                .orElse("未知大区");
    }

    @Cacheable(value = "region-meta", key = "#regionName")
    public Long getRegionIdByName(String regionName) {
        return regionRepository.findByName(regionName)
                .map(r -> r.getId())
                .orElse(null);
    }

    public Long getRepIdByName(String repName) {
        return repRepository.findByName(repName)
                .map(SalesRep::getId)
                .orElse(null);
    }


    /**
     * 按大区过滤的退单率统计
     */
    public List<Object[]> queryRefundRatesByRegion(LocalDate start, LocalDate end, Long regionId) {
        return orderRepository.findRefundRateByRepAndRegion(start, end, regionId);
    }

    /**
     * 按大区过滤的产品最后出单日期
     */
    public LocalDate queryLastOrderDateByRegion(Long productId, Long regionId) {
        return orderRepository.findLastOrderDateByProductAndRegion(productId, regionId);
    }

    /**
     * 单个销售员的退单数
     */
    public long queryRefundCountByRep(Long repId, LocalDate start, LocalDate end) {
        return orderRepository.countRefundedByRep(repId, start, end);
    }

    /**
     * 单个销售员的订单总数
     */
    public long queryOrderCountByRep(Long repId, LocalDate start, LocalDate end) {
        return orderRepository.countByRepId(repId, start, end);
    }

    /**
     * 单个销售员的销售额（复用已有的 sumAmountByRep）
     */
    public BigDecimal queryTotalAmountByRep(Long repId, LocalDate start, LocalDate end) {
        return orderRepository.sumAmountByRep(repId, start, end);
    }

}
