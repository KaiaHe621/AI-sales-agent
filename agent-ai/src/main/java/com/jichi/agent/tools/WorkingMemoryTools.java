package com.jichi.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工作记忆——任务执行过程中的临时状态板
 * 每次任务开始时清空，任务结束后可以选择性提炼到长期记忆
 */
@Component
public class WorkingMemoryTools {

    // 结构化的工作记忆：key 是记忆标签，value 是内容
    private final Map<String, String> workingMemory = new LinkedHashMap<>();
    // 已完成的步骤列表
    private final List<String> completedSteps = new ArrayList<>();

    @Tool(description = """
            在工作记忆中保存一条信息，用于后续步骤使用。
            适用于：保存工具调用结果、中间推断结论、需要跨步骤传递的数据。
            示例：key="上海天气"，value="晴，26°C，适合户外活动"
            """)
    public String memorize(
            @ToolParam(description = "记忆标签，用于之后检索") String key,
            @ToolParam(description = "要记忆的内容") String value) {
        workingMemory.put(key, value);
        return "已记住：" + key;
    }

    @Tool(description = """
            从工作记忆中检索之前保存的信息。
            当需要用到之前步骤保存的数据时调用。
            """)
    public String recall(
            @ToolParam(description = "要检索的记忆标签") String key) {
        String value = workingMemory.get(key);
        return value != null ? key + "：" + value : "工作记忆中没有关于 " + key + " 的记录";
    }

    @Tool(description = "列出当前工作记忆中所有已保存的信息，用于整理当前任务状态")
    public String listMemory() {
        if (workingMemory.isEmpty()) return "工作记忆为空";
        StringBuilder sb = new StringBuilder("当前工作记忆：\n");
        workingMemory.forEach((k, v) -> sb.append("- ").append(k).append("：").append(v).append("\n"));
        return sb.toString();
    }

    @Tool(description = "标记一个步骤已完成，并记录执行结果摘要")
    public String markStepDone(
            @ToolParam(description = "步骤描述") String step,
            @ToolParam(description = "执行结果摘要") String result) {
        completedSteps.add(step + " → " + result);
        return "已标记完成：" + step;
    }

    // 任务结束时，由外部调用清空状态，准备下一个任务
    public void reset() {
        workingMemory.clear();
        completedSteps.clear();
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(workingMemory);
    }
}