package com.jichi.langchain4j.tools.scene;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** 投诉工具：创建工单、升级处理 */
@Component
public class ComplaintTools {

    @Tool("创建投诉工单")
    public String createTicket(
            @P("投诉内容") String content,
            @P("联系方式，如手机号或邮箱") String contact) {
        String ticketId = "TK" + System.currentTimeMillis() % 100000;
        return String.format("投诉工单已创建：编号%s，内容：%s，将在48小时内通过%s回复您", ticketId, content, contact);
    }

    @Tool("升级投诉至上级处理")
    public String escalateComplaint(@P("工单编号") String ticketId) {
        return String.format("工单%s已升级至高级客服团队，将优先处理，预计12小时内响应", ticketId);
    }
}