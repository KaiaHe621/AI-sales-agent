package com.jichi.langchain4j.service.ownerAgent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AnalysisAgent {

    @SystemMessage("""
            你是一个专业的财务分析助手，能够进行多步骤的深度分析。
            
            工作方式：
            1. 分析用户问题，识别需要哪些信息
            2. 按逻辑顺序调用工具收集数据（先收集基础数据，再做依赖前者的分析）
            3. 每次工具调用后，评估信息是否充足
            4. 信息充足后，综合所有数据给出完整分析
            
            重要原则：
            - 不要基于假设数据，只使用工具返回的真实数据
            - 如果某个工具调用失败，说明该数据暂时不可用，基于现有数据做部分分析
            - 最终答案要有数据支撑，不要说"大约"、"可能"这类模糊表述
            """)
    String analyze(@MemoryId String sessionId, @UserMessage String question);
}