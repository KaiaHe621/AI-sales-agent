package com.jichi.langchain4j.tools.scene;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** 售前工具：商品咨询、价格比较 */
@Component
public class PreSaleTools {

    @Tool("查询商品详情和价格")
    public String getProductDetail(@P("商品名称") String productName) {
        if (productName.contains("iPhone")) {
            return "iPhone 15：5999元，6.1英寸，A16芯片，支持USB-C，现货充足";
        }
        if (productName.contains("AirPods")) {
            return "AirPods Pro 2：1299元，主动降噪，续航6小时，现货";
        }
        return productName + "：暂未收录，建议搜索官网获取最新信息";
    }

    @Tool("比较两款商品的价格和功能")
    public String compareProducts(
            @P("第一款商品名称") String productA,
            @P("第二款商品名称") String productB) {
        return String.format("对比结果：%s 与 %s——建议根据预算和使用场景选择，可告知具体需求进一步推荐", productA, productB);
    }
}