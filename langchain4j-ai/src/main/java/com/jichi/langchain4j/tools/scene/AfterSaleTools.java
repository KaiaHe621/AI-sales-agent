package com.jichi.langchain4j.tools.scene;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** 售后工具：退款、换货、物流查询 */
@Component
public class AfterSaleTools {

    @Tool("查询订单物流状态")
    public String trackOrder(@P("订单号") String orderId) {
        return String.format("订单%s物流：已发货，快递单号SF1234567890，预计明日送达", orderId);
    }

    @Tool("提交退款申请")
    public String submitRefund(
            @P("订单号") String orderId,
            @P("退款原因，如：质量问题、不喜欢、尺码不合适等") String reason) {
        return String.format("退款申请已受理：订单%s，原因：%s，审核通过后1-3工作日退款", orderId, reason);
    }

    @Tool("申请换货")
    public String applyExchange(
            @P("订单号") String orderId,
            @P("换货原因") String reason) {
        return String.format("换货申请已提交：订单%s，%s，客服将在24小时内联系您", orderId, reason);
    }
}