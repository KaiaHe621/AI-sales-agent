package com.jichi.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 演示用订单快照；正式项目可改为 JPA 实体。 */
record DemoOrder(
        String id,
        String statusText,
        String productName,
        double amount,
        String createdAt,
        String address
) {}

/** 演示用用户快照；正式项目可改为 JPA 实体。 */
record DemoUser(
        long id,
        String nickname,
        String memberLevel,
        LocalDate createdAt,
        int orderCount
) {}

@Configuration
public class McpResourceTemplatesConfig {

    private final Map<String, DemoOrder> ordersById;
    private final Map<String, DemoUser> usersById;

    public McpResourceTemplatesConfig() {
        this.ordersById = Map.of(
                "ORD20240115001",
                new DemoOrder(
                        "ORD20240115001",
                        "已发货",
                        "华为 Mate 70 Pro",
                        13999.0,
                        "2026-01-15 14:30",
                        "上海市浦东新区世纪大道100号"
                )
        );
        this.usersById = Map.of(
                "1001",
                new DemoUser(1001L, "阿强", "黄金会员", LocalDate.of(2023, 6, 1), 28)
        );
    }

    @Bean
    public List<McpServerFeatures.SyncResourceTemplateSpecification> resourceTemplates() {
        return List.of(
                new McpServerFeatures.SyncResourceTemplateSpecification(
                        new McpSchema.ResourceTemplate(
                                "db://orders/{orderId}",
                                "订单详情",
                                "根据订单号读取完整的订单信息",
                                "text/plain",
                                null
                        ),
                        (exchange, request) -> readTemplatedResource(request)
                ),
                new McpServerFeatures.SyncResourceTemplateSpecification(
                        new McpSchema.ResourceTemplate(
                                "db://users/{userId}",
                                "用户信息",
                                "根据用户 ID 读取用户基本信息",
                                "text/plain",
                                null
                        ),
                        (exchange, request) -> readTemplatedResource(request)
                ),
                new McpServerFeatures.SyncResourceTemplateSpecification(
                        new McpSchema.ResourceTemplate(
                                "file://logs/{date}",
                                "应用日志",
                                "读取指定日期的应用日志，date 格式 yyyy-MM-dd",
                                "text/plain",
                                null
                        ),
                        (exchange, request) -> readTemplatedResource(request)
                )
        );
    }

    private McpSchema.ReadResourceResult readTemplatedResource(McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        String content;

        if (uri.startsWith("db://orders/")) {
            String orderId = uri.substring("db://orders/".length());
            content = readOrderById(orderId);
        } else if (uri.startsWith("db://users/")) {
            String userId = uri.substring("db://users/".length());
            content = readUserById(userId);
        } else if (uri.startsWith("file://logs/")) {
            String date = uri.substring("file://logs/".length());
            content = readLogByDate(date);
        } else {
            content = "未知资源 URI：" + uri;
        }

        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(uri, "text/plain", content))
        );
    }

    private String readOrderById(String orderId) {
        DemoOrder order = ordersById.get(orderId);
        if (order == null) {
            return "未找到订单：" + orderId;
        }

        return String.format("""
                订单号：%s
                状态：%s
                商品：%s
                金额：¥%.2f
                下单时间：%s
                收货地址：%s
                """,
                order.id(), order.statusText(),
                order.productName(), order.amount(),
                order.createdAt(), order.address());
    }

    private String readUserById(String userId) {
        DemoUser user = usersById.get(userId);
        if (user == null) {
            return "未找到用户：" + userId;
        }

        return String.format("""
                用户 ID：%s
                昵称：%s
                会员等级：%s
                注册时间：%s
                累计订单：%d 单
                """,
                user.id(), user.nickname(),
                user.memberLevel(), user.createdAt(),
                user.orderCount());
    }

    private String readLogByDate(String date) {
        java.nio.file.Path logPath = java.nio.file.Paths.get(
                "/var/log/app/app-" + date + ".log");

        try {
            if (!java.nio.file.Files.exists(logPath)) {
                return date + " 的日志文件不存在";
            }
            List<String> lines = java.nio.file.Files.readAllLines(logPath);
            // 只返回最后 100 行，避免内容太长
            int start = Math.max(0, lines.size() - 100);
            return String.join("\n", lines.subList(start, lines.size()));
        } catch (Exception e) {
            return "读取日志失败：" + e.getMessage();
        }
    }
}