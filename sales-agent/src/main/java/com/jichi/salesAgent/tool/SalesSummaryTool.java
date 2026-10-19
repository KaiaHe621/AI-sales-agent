package com.jichi.salesAgent.tool;

import com.jichi.salesAgent.dto.ProductSalesDTO;
import com.jichi.salesAgent.dto.RegionSalesDTO;
import com.jichi.salesAgent.dto.RepSalesDTO;
import com.jichi.salesAgent.security.ToolInputValidator;
import com.jichi.salesAgent.service.SalesQueryService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesSummaryTool {

    private final SalesQueryService queryService;

    private final ToolInputValidator validator;

    @Tool("计算销售员业绩排名。适用于：谁卖得最多、Top N 销售员、业绩第一名、销售冠军、" +
            "各销售员的销售额对比。可按大区筛选或查全公司。")
    public String getTopReps(
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate,
            @P("大区名称，如：华东区。传 null 或空字符串表示查全公司") String regionName,
            @P("返回前 N 名，默认 5，最大 20") int topN) {

        log.info("工具调用-getTopReps: start={}, end={}, region={}, topN={}",
                startDate, endDate, regionName, topN);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            String validRegion = validator.validateRegionName(regionName);  // 白名单校验
            int validTopN = validator.validateTopN(topN);

            int n = Math.min(Math.max(topN, 1), 20);

            List<RepSalesDTO> reps = queryService.queryRepRanking(start, end, n);
            if (reps.isEmpty()) {
                return "该时段内暂无销售数据";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("销售员业绩排名（%s 至 %s%s）：\n\n",
                    startDate, endDate,
                    regionName != null && !regionName.isBlank() ? "，" + regionName : "，全公司"));

            for (int i = 0; i < reps.size(); i++) {
                RepSalesDTO rep = reps.get(i);
                sb.append(String.format("第 %d 名：%s（%s）  销售额：¥%,.0f\n",
                        i + 1, rep.repName(), rep.regionName(), rep.totalAmount()));
            }
            return sb.toString();

        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        } catch (IllegalArgumentException e) {
            return "参数无效：" + e.getMessage();
        } catch (Exception e) {
            log.error("查询销售员排名失败", e);
            return "查询排名数据时出现问题，请稍后重试";
        }
    }

    @Tool("计算各大区的销售业绩排名。适用于：哪个大区最好、大区业绩对比、各区销售额、" +
            "大区排行榜等场景。")
    public String getRegionRanking(
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate) {

        log.info("工具调用-getRegionRanking: start={}, end={}", startDate, endDate);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            List<RegionSalesDTO> regions = queryService.queryRegionRanking(start, end);
            if (regions.isEmpty()) {
                return "该时段内暂无数据";
            }

            BigDecimal grandTotal = regions.stream()
                    .map(RegionSalesDTO::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("大区业绩排名（%s 至 %s）：\n\n", startDate, endDate));

            for (int i = 0; i < regions.size(); i++) {
                RegionSalesDTO region = regions.get(i);
                double ratio = grandTotal.compareTo(BigDecimal.ZERO) > 0
                        ? region.totalAmount().doubleValue() / grandTotal.doubleValue() * 100 : 0;
                sb.append(String.format("第 %d 名：%s  销售额：¥%,.0f  占比：%.1f%%\n",
                        i + 1, region.regionName(), region.totalAmount(), ratio));
            }
            sb.append(String.format("\n全公司合计：¥%,.0f", grandTotal));
            return sb.toString();

        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        } catch (Exception e) {
            log.error("查询大区排名失败", e);
            return "查询大区数据时出现问题，请稍后重试";
        }
    }

    @Tool("计算产品销售排名，找出畅销品或滞销品。适用于：最畅销产品、Top N SKU、" +
            "哪个产品卖得最好/最差、各品类销售情况等场景。")
    public String getTopProducts(
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate,
            @P("返回前 N 名，默认 10，最大 20。负数表示查最差的 N 名") int topN) {

        log.info("工具调用-getTopProducts: start={}, end={}, topN={}", startDate, endDate, topN);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            boolean isWorst = topN < 0;
            int n = Math.min(Math.abs(topN), 20);

            List<ProductSalesDTO> products = queryService.queryProductRanking(start, end, isWorst ? 999 : n);
            if (products.isEmpty()) {
                return "该时段内暂无产品销售数据";
            }

            if (isWorst) {
                products = products.subList(Math.max(0, products.size() - n), products.size());
                products = new java.util.ArrayList<>(products);
                java.util.Collections.reverse(products);
            } else {
                products = products.subList(0, Math.min(n, products.size()));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("产品销售排名%s（%s 至 %s）：\n\n",
                    isWorst ? "（最差）" : "（最佳）", startDate, endDate));

            for (int i = 0; i < products.size(); i++) {
                ProductSalesDTO p = products.get(i);
                sb.append(String.format("第 %d 名：%s [%s]  品类：%s  销售额：¥%,.0f  数量：%d 件\n",
                        i + 1, p.productName(), p.skuCode(), p.category(),
                        p.totalAmount(), p.totalQuantity()));
            }
            return sb.toString();

        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        } catch (Exception e) {
            log.error("查询产品排名失败", e);
            return "查询产品数据时出现问题，请稍后重试";
        }
    }

    @Tool("计算指定时段的总销售额、订单数等汇总数据。适用于：总销售额是多少、" +
            "本月/本季/本年收入、某大区的整体业绩等场景。")
    public String getSalesSummary(
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate,
            @P("大区名称，如：华东区。传 null 表示查全公司") String regionName) {

        log.info("工具调用-getSalesSummary: start={}, end={}, region={}", startDate, endDate, regionName);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            Long regionId = null;
            if (regionName != null && !regionName.isBlank()) {
                regionId = queryService.getRegionIdByName(regionName);
                if (regionId == null) {
                    return "未找到大区：" + regionName;
                }
            }

            BigDecimal totalAmount = queryService.queryTotalAmount(regionId, start, end);

            return String.format("销售额汇总（%s 至 %s%s）：\n总销售额：¥%,.0f",
                    startDate, endDate,
                    regionName != null && !regionName.isBlank() ? "，" + regionName : "，全公司",
                    totalAmount);

        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        } catch (Exception e) {
            log.error("查询销售汇总失败", e);
            return "查询汇总数据时出现问题，请稍后重试";
        }
    }
}
