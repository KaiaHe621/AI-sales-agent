package com.jichi.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SimpleReActAgent {

    private final ChatClient chatClient;

    // 工具注册表：工具名 → 工具实现
    // 模型输出 "Action: getWeather"，就直接拿名字去这里找对应函数
    private final Map<String, AgentTool> tools;

    public SimpleReActAgent(@Qualifier("dashScopeChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.tools = Map.of(
                "getWeather", this::getWeather,
                "getDate", this::getDate
        );
    }

    /**
     * 执行 ReAct 循环
     *
     * @param userTask      用户任务描述
     * @param maxIterations 最大循环次数（防止死循环）
     */
    public String run(String userTask, int maxIterations) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt();

        messages.add(new UserMessage(userTask));

        for (int i = 0; i < maxIterations; i++) {
            // 每轮都把完整消息历史传给模型
            // 模型是无状态的，它"记得"上下文靠的就是这个列表
            String modelOutput = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(messages)
                    .call()
                    .content();

            messages.add(new AssistantMessage(modelOutput));

            System.out.println("=== 第 " + (i + 1) + " 轮 ===");
            System.out.println(modelOutput);

            // 检测是否输出了最终答案
            if (modelOutput.contains("Final Answer:")) {
                return extractFinalAnswer(modelOutput);
            }

            // 解析模型想调用哪个工具、参数是什么
            String toolName = extractAction(modelOutput);
            String toolInput = extractActionInput(modelOutput);

            if (toolName == null) {
                return "模型输出格式异常，无法继续执行";
            }

            AgentTool tool = tools.get(toolName);
            String observation;
            if (tool == null) {
                observation = "工具 " + toolName + " 不存在，请换一个";
            } else {
                observation = tool.execute(toolInput);
            }

            System.out.println("Observation: " + observation);

            // 把观察结果加回消息历史，下一轮模型就能看到这个结果
            messages.add(new UserMessage("Observation: " + observation));
        }

        return "超过最大迭代次数（" + maxIterations + "），任务未完成";
    }

    private String buildSystemPrompt() {
        return """
                你是一个智能助手，按照以下格式严格输出，每次只做一个动作：
                
                可用工具：
                - getWeather(input: JSON {"city": "城市名", "date": "today/tomorrow"})：查询天气
                - getDate(input: 无)：获取今天的日期
                
                输出格式（严格遵守）：
                Thought: [你的分析和下一步计划]
                Action: [工具名]
                Action Input: [工具参数，JSON 格式]
                
                收到 Observation 后继续思考，直到可以回答为止：
                Thought: [分析观察结果]
                Final Answer: [给用户的最终回答]
                
                注意：
                - 每次只输出一个 Action 或 Final Answer，不要一次输出多个
                - 工具名必须和上面列表完全一致
                """;
    }

    // ---- 工具实现 ----

    private String getWeather(String input) {
        // 真实项目里这里调用天气 API，这里先 mock
        return """
                {"city": "上海", "weather": "晴", "temp": "8~15°C", "wind": "北风3级"}
                """;
    }

    private String getDate(String input) {
        return java.time.LocalDate.now().toString();
    }

    // ---- 解析工具 ----

    private String extractFinalAnswer(String output) {
        int idx = output.indexOf("Final Answer:");
        if (idx == -1) return output;
        return output.substring(idx + "Final Answer:".length()).strip();
    }

    private String extractAction(String output) {
        for (String line : output.split("\n")) {
            if (line.startsWith("Action:")) {
                return line.substring("Action:".length()).strip();
            }
        }
        return null;
    }

    private String extractActionInput(String output) {
        for (String line : output.split("\n")) {
            if (line.startsWith("Action Input:")) {
                return line.substring("Action Input:".length()).strip();
            }
        }
        return "";
    }

    @FunctionalInterface
    interface AgentTool {
        String execute(String input);
    }
}