# Java AI 应用开发实践

基于 Java 技术栈的 AI 应用开发项目合集。从大模型调用、Prompt 工程、RAG 知识库，到 Agent 自动化、MCP / A2A 协议、多 Agent 协作，覆盖用 Java 构建 AI 应用的完整路径，并包含三个可直接运行的完整实战项目。

全部项目基于 Spring Boot 生态，既可以作为学习参考，也可以作为企业系统接入 AI 能力的实践样板。

## 项目总览

### 框架基础

| 项目 | 技术栈 | 说明 |
|------|--------|------|
| spring-ai | Spring AI | ChatClient、Prompt、会话记忆、流式输出、多模态 |
| spring-ai-alibaba | Spring AI Alibaba | 阿里云百炼（DashScope）模型接入 |
| langchain4j-ai | LangChain4j | AI Service、对话记忆、工具调用 |
| prompt | Spring AI | Prompt 设计原则、结构化输出、动态构建 |
| agent-ai | Spring AI Alibaba | ReAct 模式与业务 Agent 编排 |
| agent-scope-ai | AgentScope | 阿里开源 Java 多 Agent 框架实践 |

### 协议与工程化

| 项目 | 说明 |
|------|------|
| mcp-tools-server | MCP Server：暴露工具、Prompt 模板、Resources |
| mcp-tools-client | MCP Client：启动时动态发现并加载远程工具 |
| a2a-server / a2a-client | A2A 协议：Agent 之间的通信与协作 |
| eval-framework | LLM / Agent 评测框架：Golden Dataset、关键词评估、裁判模型打分 |

### 完整实战项目

| 项目 | 说明 |
|------|------|
| sales-agent + sales-agent-front | 智能销售数据分析 Agent：自然语言查询、图表生成、异常检测 |
| rag-kb + rag-kb-front | 企业知识库问答系统：文档解析、向量检索、答案溯源 |
| voice-shopping | 语音导购 Agent：实时语音识别、意图理解、商品推荐 |

## 技术栈

`Java 21` `Spring Boot 3` `Spring AI` `Spring AI Alibaba` `LangChain4j` `AgentScope` `MySQL` `Redis` `MinIO` `Vue 3` `React 18` `ECharts` `Sa-Token` `SSE`

## 实战项目

### 智能销售数据分析 Agent（sales-agent）

面向销售团队的自然语言数据分析系统。销售人员无需编写 SQL 或翻阅固定报表，通过日常对话即可完成数据查询、排名分析、趋势分析、异常检测和图表生成。

- 基于 LangChain4j 构建 Agent，封装系统提示词、工具调用、会话记忆与流式响应四大核心能力
- 5 个 Agent Tools 覆盖订单查询、业绩排名、趋势分析、异常检测、图表生成
- 自定义 CHART_JSON 协议，在 SSE 文本流中区分结构化图表数据与自然语言，前端直接渲染 ECharts 图表
- 基于 Sa-Token + ThreadLocal 用户上下文实现三级角色数据权限（总监 / 经理 / 销售员），工具参数白名单校验防止 LLM 幻觉导致的非法查询
- 对话记忆持久化至 MySQL，Redis 缓存高频聚合查询

前端：Vue 3 + Vite + Pinia + Element Plus + ECharts

### 企业知识库问答系统（rag-kb）

上传产品手册、规章制度等文档后，系统自动建立索引，用户以自然语言提问，系统检索相关片段并给出带出处的答案。

- 基于 Spring AI 构建完整 RAG 链路：文档解析 → 切片 → Embedding → 向量检索 → 生成
- Redis 向量检索 + MinIO 对象存储
- 前端：React 18 + Ant Design，支持 Markdown 渲染与代码高亮

### 语音导购 Agent（voice-shopping）

用户以语音说出购物需求，Agent 实时识别语音、理解意图、检索商品并主动推荐，模拟真人导购对话。

- 基于 AgentScope 构建多 Agent 编排
- 接入通义千问（qwen-max / qwen-turbo）与实时语音识别（paraformer）

## 相关文档

- [简历：智能销售数据分析 Agent 系统](./简历-销售AI%20Agent系统.md)
- [面试问答：智能销售 Agent 系统](./面试问答-销售Agent.md)
- [智能销售 Agent 项目学习文档](./sales-agent/智能销售Agent项目学习文档.md)

## 目录结构

```
├── spring-ai              # Spring AI 基础实践
├── spring-ai-alibaba      # Spring AI Alibaba 实践
├── langchain4j-ai         # LangChain4j 核心框架
├── prompt                 # Prompt 工程
├── agent-ai               # Agent 开发
├── agent-scope-ai         # AgentScope 多 Agent 框架
├── mcp-tools-server       # MCP Server
├── mcp-tools-client       # MCP Client
├── a2a-server             # A2A Server
├── a2a-client             # A2A Client
├── eval-framework         # LLM / Agent 评测框架
├── rag-kb                 # 企业知识库问答系统（后端）
├── rag-kb-front           # 企业知识库问答系统（前端）
├── sales-agent            # 智能销售数据分析 Agent（后端）
├── sales-agent-front      # 智能销售数据分析 Agent（前端）
└── voice-shopping         # 语音导购 Agent
```
