# 智能销售数据分析 Agent

面向销售团队的自然语言数据分析系统。销售人员无需编写 SQL 或翻阅固定报表，通过日常对话即可完成数据查询、排名分析、趋势分析、异常检测和图表生成。

## 项目结构

| 目录 | 说明 |
|------|------|
| sales-agent | 后端：Spring Boot 3 + LangChain4j Agent |
| sales-agent-front | 前端：Vue 3 + Vite + Pinia + Element Plus + ECharts |

## 核心功能

- 基于 LangChain4j 构建 Agent，封装系统提示词、工具调用、会话记忆与流式响应四大核心能力
- 5 个 Agent Tools 覆盖订单查询、业绩排名、趋势分析、异常检测、图表生成
- 自定义 CHART_JSON 协议，在 SSE 文本流中区分结构化图表数据与自然语言，前端直接渲染 ECharts 图表
- 基于 Sa-Token + ThreadLocal 用户上下文实现三级角色数据权限（总监 / 经理 / 销售员），工具参数白名单校验防止 LLM 幻觉导致的非法查询
- 对话记忆持久化至 MySQL，Redis 缓存高频聚合查询
- 前端支持登录鉴权、流式对话、Markdown 渲染、图表展示

## 技术栈

`Java 21` `Spring Boot 3` `LangChain4j` `DeepSeek` `MySQL` `Redis` `Sa-Token` `SSE` `Vue 3` `Vite` `Pinia` `Element Plus` `ECharts`

## 快速开始

### 后端（sales-agent）sales-agent/README.md

1. 准备 MySQL 8 与 Redis（默认 `localhost:6379` 无密码）
2. 启动时自动执行 `src/main/resources/db/schema.sql` 建表并导入测试数据
3. 配置环境变量 `DEEPSEEK_API_KEY`（数据库默认 `root / 123456`，可用 `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` 覆盖）
4. 启动：

```bash
cd sales-agent
mvn spring-boot:run
```

服务运行在 `http://localhost:8087`。

### 前端（sales-agent-front）

```bash
cd sales-agent-front
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，`/auth` 与 `/agent` 接口通过 Vite 代理转发到后端 8087。

## License

见 [sales-agent/LICENSE](./sales-agent/LICENSE)。
