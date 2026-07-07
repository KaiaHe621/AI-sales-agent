# 智能销售 Agent 项目学习文档

> 适合对象：完全不了解本项目、但希望按正确顺序读懂代码和知识点的学习者。
>
> 项目位置：`jc-sales-agent` 后端 + `jc-sales-agent-front` 前端。

---

## 1. 这个项目到底是什么

本项目是一个“智能销售数据分析 Agent”。用户可以用自然语言提问，例如：

- “近 6 个月销售趋势，生成折线图”
- “本季度 Top5 销售员是谁？”
- “各大区销售排名”
- “最近销售数据有没有异常？”

系统会完成以下流程：

1. 前端把用户问题发送给后端。
2. 后端把问题交给 LangChain4j 创建的 `SalesAgent`。
3. 大模型理解用户意图，并决定是否调用工具。
4. 工具查询 MySQL 中的销售数据，计算排名、趋势、异常或生成图表 JSON。
5. 大模型把工具结果整理成中文回答。
6. 前端展示回答；如果返回了 `CHART_JSON:`，前端解析为 ECharts 图表。

一句话理解：

> 这是一个“LLM + 业务工具 + 数据库 + 可视化前端”的销售分析助手。

---

## 2. 推荐学习顺序

如果你完全不了解项目，建议不要一上来就看所有代码，而是按下面顺序学习：

| 阶段 | 学习目标 | 重点文件 |
|---|---|---|
| 1 | 先知道项目模块和启动方式 | `pom.xml`、`application.yml`、`package.json`、`vite.config.js` |
| 2 | 理解数据库里有什么业务数据 | `db/schema.sql`、`db/data.sql`、`entity`、`repository` |
| 3 | 理解普通后端查询如何工作 | `SalesQueryService`、`SalesOrderRepository` |
| 4 | 理解“工具”是什么 | `tool/*Tool.java` |
| 5 | 理解 Agent 如何调用工具 | `SalesAgent.java`、`SalesAgentConfig.java` |
| 6 | 理解接口如何连接前后端 | `SalesAgentController`、`SalesAgentStreamController`、`AuthController` |
| 7 | 理解前端聊天与图表渲染 | `stores/chat.js`、`ChatView.vue`、`ChartRenderer.vue` |
| 8 | 理解权限、记忆、缓存、异常检测等增强能力 | `WebMvcConfig`、`MysqlChatMemoryStore`、`AnomalyDetectionTool` |

---

## 3. 项目整体架构

### 3.1 后端模块：`jc-sales-agent`

这是 Spring Boot 项目，核心职责：

- 提供登录接口。
- 提供聊天接口。
- 接入 LangChain4j 和通义千问兼容 OpenAI 接口。
- 定义销售分析工具。
- 查询 MySQL 销售数据。
- 保存对话记忆。
- 做权限范围控制。

主要目录：

```text
jc-sales-agent/src/main/java/com/jichi/salesAgent
├── agent       # Agent 接口和 Agent Bean 配置
├── config      # Web、Redis、异常处理、Token 日志等配置
├── controller  # HTTP 接口
├── dto         # 接口/统计结果数据结构
├── entity      # JPA 实体，对应数据库表
├── memory      # LangChain4j 对话记忆存储
├── repository  # JPA 数据访问层
├── security    # 当前用户上下文、工具参数校验
├── service     # 业务查询服务
└── tool        # 给大模型调用的业务工具
```

### 3.2 前端模块：`jc-sales-agent-front`

这是 Vue 3 + Vite 项目，核心职责：

- 登录页面。
- 聊天页面。
- 会话列表和本地会话缓存。
- 调用后端普通接口或流式接口。
- 解析图表 JSON 并用 ECharts 渲染。

主要目录：

```text
jc-sales-agent-front/src
├── api          # axios 封装
├── components   # 消息气泡、图表组件
├── router       # 路由
├── stores       # Pinia 状态管理
├── views        # 登录页、聊天页
├── App.vue
└── main.js
```

---

## 4. 第一阶段：看依赖和配置

### 4.1 后端依赖：`jc-sales-agent/pom.xml`

后端使用的关键技术：

| 技术 | 作用 |
|---|---|
| Spring Boot Web | 提供 REST 接口 |
| Spring WebFlux | 用于 SSE 流式输出 |
| Spring Data JPA | 操作 MySQL 数据库 |
| MySQL Connector | 连接 MySQL |
| LangChain4j | 把 Java 接口变成 AI Agent，并支持工具调用 |
| LangChain4j OpenAI Starter | 接入 OpenAI 兼容模型接口，这里指向 DashScope |
| Sa-Token | 登录和权限框架 |
| Redis | 缓存相关能力 |
| Lombok | 简化 Java 样板代码 |

关键点：

- 项目使用 Java 21。
- LangChain4j 版本是 `1.12.1`。
- Spring Boot 版本是 `3.5.11`。

### 4.2 后端配置：`application.yml`

关键配置包括：

1. 服务端口：`8087`。
2. MySQL 数据源。
3. SQL 初始化脚本：
   - `classpath:db/schema.sql`
   - `classpath:db/data.sql`
4. LangChain4j 模型配置：
   - `base-url` 指向 DashScope OpenAI 兼容接口。
   - `model-name` 使用 `qwen-max`。
5. Sa-Token 登录配置。
6. `app.auth.enabled: false` 表示当前开发环境关闭权限拦截。

注意：配置文件里包含数据库密码和模型 API Key，真实生产项目不应直接提交到代码仓库，应放到环境变量或密钥管理系统。

### 4.3 前端配置：`jc-sales-agent-front/package.json`

前端主要依赖：

| 技术 | 作用 |
|---|---|
| Vue 3 | 前端框架 |
| Vue Router | 页面路由 |
| Pinia | 状态管理 |
| Element Plus | UI 组件库 |
| Axios | 普通 HTTP 请求 |
| ECharts | 图表渲染 |
| markdown-it | Markdown 渲染 |

### 4.4 前端代理：`vite.config.js`

前端开发服务端口是 `5173`。

它把下面请求代理到后端 `http://localhost:8087`：

- `/auth`
- `/agent`

所以前端代码里可以直接请求 `/agent/chat/stream`，不需要写完整后端地址。

---

## 5. 第二阶段：理解业务数据模型

### 5.1 数据库表

数据库建表文件：`jc-sales-agent/src/main/resources/db/schema.sql`。

核心表：

| 表名 | 含义 |
|---|---|
| `sa_sales_region` | 销售大区，例如华东区、华南区 |
| `sa_sales_rep` | 销售人员，包含角色、所属大区、邮箱 |
| `sa_product` | 产品 SKU、品类、价格、成本 |
| `sa_sales_order` | 销售订单，包含销售员、产品、大区、金额、利润、状态、日期 |
| `sa_chat_memory` | 对话记忆，保存 LangChain4j 的消息 JSON |

### 5.2 测试数据

测试数据文件：`jc-sales-agent/src/main/resources/db/data.sql`。

它设计了几类典型销售场景：

- 多个大区：华东、华南、华北、西南。
- 多个角色：销售代表、大区经理、全国总监。
- 多个产品品类：数码产品、家用电器、服装配饰、其他。
- 动态日期：基于 `CURDATE()` 往前推，保证无论什么时候启动都有“近几个月”数据。
- 预埋异常：
  - 华北区近 14 天无订单。
  - 某 SKU 近 30 天零销售。
  - 某销售员近 60 天业绩下滑。
  - 某销售员历史退单率偏高。

这让异常检测工具可以稳定演示效果。

### 5.3 Entity 和 Repository

`entity` 包中的类对应数据库表。

`repository` 包中的类负责数据库查询。例如 `SalesOrderRepository`：

- `findByRepIdAndOrderDateBetween`：按销售员和日期查订单。
- `findByRegionIdAndOrderDateBetween`：按大区和日期查订单。
- `sumAmountByRegion`：统计大区销售额。
- `findRepRanking`：销售员销售额排名。
- `findRegionRanking`：大区销售额排名。
- `findProductRanking`：产品销售额排名。
- `findMonthlyTrend`：月度趋势。
- `findRefundRateByRep`：销售员退单率。

这里的知识点：

- Spring Data JPA。
- JPQL 查询。
- 原生 SQL 查询。
- 聚合函数：`SUM`、`COUNT`、`GROUP BY`、`ORDER BY`。
- DTO 映射。

---

## 6. 第三阶段：理解普通业务查询服务

核心文件：`SalesQueryService.java`。

它是业务查询的中间层，位于：

```text
Controller / Tool -> SalesQueryService -> Repository -> MySQL
```

它解决的问题：

1. 不让工具类直接散落复杂 SQL 逻辑。
2. 集中处理权限过滤。
3. 统一封装业务查询。
4. 提供名称解析，例如大区名转大区 ID。
5. 复用统计计算逻辑。

### 6.1 权限过滤

`queryOrders` 会读取 `UserContext`：

- 普通销售员：只能查自己的订单。
- 大区经理：只能查自己大区。
- 全国总监：不限制。

这体现了一个重要业务原则：

> 即使是 AI Agent，也不能绕过系统权限。权限应该在后端业务层兜底，而不是只靠提示词约束模型。

### 6.2 常见查询能力

`SalesQueryService` 提供：

- 查订单列表。
- 查总销售额。
- 查销售员排名。
- 查大区排名。
- 查产品排名。
- 查月度趋势。
- 计算增长率。
- 查产品最后出单时间。
- 查退单率。

这里的知识点：

- Service 层设计。
- 权限上下文。
- 缓存注解 `@Cacheable`。
- Java Stream 数据处理。
- BigDecimal 金额计算。

---

## 7. 第四阶段：理解“工具”是什么

在 Agent 项目中，“工具”是给大模型调用的后端函数。

大模型本身不会直接查数据库，它只会根据用户问题选择合适的工具。工具执行真实代码，返回结果文本给模型，模型再组织成最终回答。

本项目工具位于：`jc-sales-agent/src/main/java/com/jichi/salesAgent/tool`。

### 7.1 `@Tool` 注解

LangChain4j 使用 `@Tool` 标记一个方法可被大模型调用。

例如 `SalesSummaryTool.getTopReps` 的用途描述是“计算销售员业绩排名”。模型看到用户问“Top5 销售员是谁”，就可能选择这个工具。

### 7.2 `@P` 注解

`@P` 用于描述工具参数。

例如：

- 查询开始日期格式。
- 大区名称应该传什么。
- TopN 最大值是多少。

参数描述越清楚，模型越容易正确调用工具。

### 7.3 工具分类

#### 7.3.1 `SalesQueryTool`

作用：查询原始订单列表。

适合问题：

- “帮我查一下 2024-11 的订单”
- “华东区上个月有哪些订单？”

不适合：排名、趋势、图表、异常检测。

#### 7.3.2 `SalesSummaryTool`

作用：销售汇总和排名。

包含：

- `getTopReps`：销售员排名。
- `getRegionRanking`：大区排名。
- `getTopProducts`：产品排名。
- `getSalesSummary`：总销售额汇总。

适合问题：

- “本季度销售冠军是谁？”
- “各大区销售排名”
- “最畅销产品 Top10”
- “本月总销售额是多少？”

#### 7.3.3 `SalesTrendTool`

作用：趋势和增长率分析。

包含：

- `calcMonthOverMonth`：环比。
- `calcYearOverYear`：同比。
- `getMonthlyTrend`：近 N 个月趋势。

适合问题：

- “本月比上月增长多少？”
- “今年同比去年如何？”
- “近 6 个月销售趋势怎么样？”

#### 7.3.4 `ChartGeneratorTool`

作用：生成 ECharts JSON。

包含：

- `generateLineChart`：折线图。
- `generateBarChart`：柱状图。
- `generatePieChart`：饼图。

返回格式是：

```text
CHART_JSON:{...ECharts option JSON...}
```

前端识别这个前缀后，把后面的 JSON 交给 ECharts 渲染。

#### 7.3.5 `AnomalyDetectionTool`

作用：自动检测销售异常。

检测类型：

- 大区订单量骤降。
- 产品连续零销售。
- 销售员退单率异常。
- 销售员业绩骤降。

它会根据当前用户角色自动限定检测范围。

这里的知识点：

- Tool Calling / Function Calling。
- 工具描述工程。
- 参数校验。
- 日期解析。
- 异常兜底。
- 只读工具设计。

---

## 8. 第五阶段：理解 Agent 如何工作

核心文件：

- `SalesAgent.java`
- `SalesAgentConfig.java`

### 8.1 `SalesAgent.java`

这是一个接口，不是普通 Service。

它定义两个方法：

1. `chat`：普通非流式回答。
2. `chatStream`：流式回答，返回 `TokenStream`。

方法上写了 `@SystemMessage`，这是 Agent 的系统提示词。

系统提示词定义了：

- Agent 角色：专业销售数据分析助手。
- 当前日期解释规则。
- 能力范围：查订单、统计、趋势、图表、异常检测。
- 限制：只能查询，不能修改数据，不能预测未来，不能发送邮件。
- 回答风格：中文、金额格式、给出简短判断。
- 图表规则：如果工具返回 `CHART_JSON:`，必须原样输出。

### 8.2 `@MemoryId`

`@MemoryId String sessionId` 表示同一个会话 ID 下的多轮对话会共享上下文记忆。

例如：

1. 用户：“帮我看华东区近 6 个月销售趋势”
2. 用户：“那华南区呢？”

第二句话省略了很多信息，Agent 可以结合前文理解。

### 8.3 `@UserMessage`

`@UserMessage String message` 表示用户当前输入。

### 8.4 `@V("today")`

`@V("today") String today` 会把 Java 传入的当前日期注入到系统提示词里的 `{{today}}`。

这样模型理解“上个月”“本季度”“今年”时，会基于后端传入日期，而不是模型自己猜。

### 8.5 `SalesAgentConfig.java`

这个类用 `AiServices.builder(SalesAgent.class)` 创建真正的 Agent Bean。

它配置了：

- `chatModel`：普通聊天模型。
- `streamingChatModel`：流式模型。
- `.tools(...)`：注册所有业务工具。
- `.beforeToolExecution(...)`：工具调用前日志。
- `.afterToolExecution(...)`：工具调用后日志。
- `.chatMemoryProvider(...)`：每个 session 使用窗口记忆。

核心思想：

```text
用户问题 -> LLM 判断 -> 调用工具 -> 工具查数据库 -> LLM 组织答案 -> 返回前端
```

这里的知识点：

- LangChain4j AI Service。
- System Prompt。
- Tool Calling。
- Streaming Token。
- Chat Memory。
- Prompt 约束和业务规则。

---

## 9. 第六阶段：理解 HTTP 接口

### 9.1 登录接口：`AuthController`

接口：

- `POST /auth/login`
- `POST /auth/logout`

登录时传 `repId`，后端从 `sa_sales_rep` 查销售员信息，然后使用 Sa-Token 登录。

返回：

- token
- username
- role

并把用户信息写入 Sa-Token Session，供后续权限判断使用。

### 9.2 普通聊天接口：`SalesAgentController`

接口：

- `POST /agent/chat`
- `DELETE /agent/session/{sessionId}`

`/agent/chat` 调用：

```text
salesAgent.chat(sessionId, message, LocalDate.now().toString())
```

也就是非流式地拿到完整回答。

`DELETE /agent/session/{sessionId}` 用于清除 MySQL 中保存的对话记忆。

### 9.3 流式聊天接口：`SalesAgentStreamController`

接口：

- `POST /agent/chat/stream`

它返回 `text/event-stream`，也就是 SSE。

处理流程：

1. 调用 `salesAgent.chatStream(...)`。
2. `onPartialResponse` 每拿到一个 token，就推送一个 SSE 事件。
3. `onCompleteResponse` 推送 `[DONE]`。
4. `onError` 推送错误消息。

前端聊天主要使用这个流式接口，所以用户能看到 AI 一边生成一边显示。

这里的知识点：

- REST Controller。
- 请求体校验。
- SSE：Server-Sent Events。
- Flux 响应式流。
- 流式 LLM 输出。

---

## 10. 第七阶段：理解前端聊天链路

### 10.1 API 封装：`src/api/index.js`

这里创建了 axios 实例，并在请求头中加入 `sa-token`。

普通接口：

- `authApi.login`
- `authApi.logout`
- `agentApi.chat`
- `agentApi.clearSession`

注意：流式接口没有用 axios，而是在 `stores/chat.js` 里直接用 `fetch`，因为浏览器处理流式响应时 `fetch` 更合适。

### 10.2 登录状态：`stores/auth.js`

负责：

- 保存 token 到 `localStorage`。
- 保存用户信息到 `localStorage`。
- 判断是否登录。
- 登录成功后跳转首页。
- 登出时清理登录态。

### 10.3 聊天状态：`stores/chat.js`

这是前端最核心的状态管理文件。

它负责：

- 本地保存会话列表。
- 创建新会话。
- 删除会话。
- 切换会话。
- 发送消息。
- 接收流式响应。
- 解析 `CHART_JSON:`。
- 停止生成。

`sendMessage` 的大致流程：

1. 用户消息加入当前会话。
2. 创建一条空的 assistant 占位消息。
3. 调用 `/agent/chat/stream`。
4. 使用 `reader.read()` 不断读取后端 SSE 数据。
5. 把 token 追加到 `fullContent`。
6. 如果发现 `CHART_JSON:`，尝试解析后面的 JSON。
7. 解析成功后，把文本和 `chartOption` 分开存入消息。
8. 流结束后保存会话到 `localStorage`。

### 10.4 页面：`ChatView.vue`

负责展示：

- 左侧会话列表。
- 快捷提问。
- 顶部标题。
- 消息列表。
- 输入框。
- 发送/停止按钮。

用户点击快捷问题或输入问题后，最终都会调用 `chat.sendMessage(text)`。

### 10.5 图表渲染：`ChartRenderer.vue`

这个组件接收一个 ECharts `option` 对象，然后：

1. 初始化 ECharts 实例。
2. 合并默认样式。
3. 调用 `chart.setOption(...)`。
4. 监听容器大小变化自动 resize。
5. 组件销毁时释放图表实例。

这里的知识点：

- Vue 3 Composition API。
- Pinia 状态管理。
- fetch 流式读取。
- SSE 文本协议解析。
- ECharts option。
- 本地存储 localStorage。

---

## 11. 第八阶段：理解权限、记忆和安全设计

### 11.1 权限设计

相关文件：

- `AuthController`
- `WebMvcConfig`
- `UserContext`
- `SalesQueryService`
- `AnomalyDetectionTool`

项目里有三类角色：

| 角色 | 能看什么数据 |
|---|---|
| `SALES_REP` | 只能看自己的订单和异常 |
| `SALES_MANAGER` | 只能看自己大区的数据 |
| `SALES_DIRECTOR` | 可以看全公司数据 |

关键学习点：

> 提示词中说“不能越权”是不够的，必须在后端查询层强制限制。

### 11.2 对话记忆

相关文件：`MysqlChatMemoryStore.java`。

LangChain4j 的 `ChatMemoryStore` 负责持久化对话。

本项目用 `sa_chat_memory` 表保存：

- `session_id`
- 序列化后的消息 JSON
- 更新时间

`SalesAgentConfig` 使用：

```text
MessageWindowChatMemory maxMessages = 20
```

也就是说，每个会话最多保留 20 条消息上下文。

### 11.3 工具安全

工具安全体现在：

- 工具只做查询，不修改数据库。
- Agent 系统提示词明确限制只读。
- 部分参数使用 `ToolInputValidator` 白名单校验。
- 查询层再次做权限过滤。
- 工具内部捕获异常，避免把内部错误直接暴露给用户。

### 11.4 缓存

`SalesQueryService` 中部分方法使用 `@Cacheable`：

- 销售员排名。
- 大区排名。
- 月度趋势。
- 大区名称解析。

目的：减少重复查询，提高响应速度。

---

## 12. 一次完整请求的执行链路

以用户问：“近 6 个月销售趋势，生成折线图”为例：

```text
1. ChatView.vue 点击快捷问题或输入问题
2. stores/chat.js 调用 fetch('/agent/chat/stream')
3. Vite 代理转发到 localhost:8087
4. SalesAgentStreamController 接收请求
5. 调用 salesAgent.chatStream(sessionId, message, today)
6. LangChain4j 把用户问题 + 系统提示词 + 历史记忆发给大模型
7. 大模型判断需要生成趋势折线图
8. 调用 ChartGeneratorTool.generateLineChart
9. 工具调用 SalesQueryService.queryMonthlyTrend
10. SalesQueryService 调用 SalesOrderRepository.findMonthlyTrend
11. MySQL 返回近 N 个月聚合数据
12. 工具组装 ECharts option，并返回 CHART_JSON:{...}
13. 大模型按系统提示词输出一句话 + 原始 CHART_JSON
14. 后端通过 SSE 一段段推给前端
15. stores/chat.js 解析 token，发现 CHART_JSON 后解析 JSON
16. MessageBubble 展示文字，ChartRenderer 展示 ECharts 图表
```

---

## 13. 初学者应该重点掌握的知识点

### 13.1 后端基础

- Spring Boot 项目结构。
- Controller、Service、Repository 分层。
- JPA 实体和仓库。
- `@RestController`、`@Service`、`@Repository`、`@Component`。
- `@RequiredArgsConstructor` 依赖注入。
- `@Query` 自定义查询。
- DTO 和 Entity 的区别。

### 13.2 数据分析基础

- 销售额、利润、订单量。
- TopN 排名。
- 同比：与去年同期比较。
- 环比：与上一周期比较。
- 趋势：按月聚合。
- 异常检测：零销售、退单率、业绩下滑。

### 13.3 Agent 基础

- LLM 不是直接操作数据库，而是通过 Tool。
- Tool 描述决定模型是否能正确选择工具。
- System Prompt 定义角色、边界、输出格式。
- MemoryId 支持多轮对话。
- Tool Calling 是“模型决策 + 程序执行”的结合。

### 13.4 前端基础

- Vue 组件组织。
- Pinia 管理全局状态。
- fetch 读取流式响应。
- localStorage 保存本地会话。
- ECharts 图表渲染。

### 13.5 安全和工程化

- 不能只依赖提示词做权限控制。
- 工具应该默认只读。
- 外部输入要校验。
- 密钥不应硬编码。
- 对话记忆要按 session 隔离。
- 重要操作要记录日志。

---

## 14. 建议的代码阅读路线

### 第 1 天：跑通和看架构

1. 看 `README.md` 了解项目二定位。
2. 看 `jc-sales-agent/pom.xml`。
3. 看 `jc-sales-agent/src/main/resources/application.yml`。
4. 看 `jc-sales-agent-front/package.json`。
5. 看 `jc-sales-agent-front/vite.config.js`。

目标：知道后端、前端分别用什么技术，以及端口和代理如何连接。

### 第 2 天：看数据库和基础查询

1. 看 `db/schema.sql`。
2. 看 `db/data.sql`。
3. 看 `entity/SalesOrder.java`、`Product.java`、`SalesRep.java`、`SalesRegion.java`。
4. 看 `repository/SalesOrderRepository.java`。
5. 看 `service/SalesQueryService.java`。

目标：知道销售数据长什么样，普通查询是怎么实现的。

### 第 3 天：看工具

1. 看 `SalesQueryTool.java`。
2. 看 `SalesSummaryTool.java`。
3. 看 `SalesTrendTool.java`。
4. 看 `ChartGeneratorTool.java`。
5. 看 `AnomalyDetectionTool.java`。

目标：理解模型能调用哪些后端能力。

### 第 4 天：看 Agent 编排

1. 看 `SalesAgent.java`。
2. 看 `SalesAgentConfig.java`。
3. 重点理解 `@SystemMessage`、`@Tool`、`@MemoryId`、`@UserMessage`、`@V`。

目标：理解自然语言如何变成工具调用。

### 第 5 天：看接口和前端

1. 看 `AuthController.java`。
2. 看 `SalesAgentController.java`。
3. 看 `SalesAgentStreamController.java`。
4. 看 `jc-sales-agent-front/src/stores/chat.js`。
5. 看 `ChatView.vue`。
6. 看 `ChartRenderer.vue`。

目标：理解一次聊天请求从前端到后端再回来的完整链路。

### 第 6 天：看增强能力

1. 看 `MysqlChatMemoryStore.java`。
2. 看 `WebMvcConfig.java`。
3. 看 `UserContext.java`。
4. 看 `ToolInputValidator.java`。
5. 看 `GlobalExceptionHandler.java`。

目标：理解权限、记忆、安全、异常处理。

---

## 15. 常见问题解释

### 15.1 为什么不让大模型直接写 SQL？

因为直接写 SQL 风险很大：

- 可能生成错误 SQL。
- 可能越权查询。
- 可能执行修改操作。
- 难以控制性能。

本项目用固定工具封装查询能力，大模型只能选择工具和参数，更安全、更稳定。

### 15.2 为什么工具返回字符串，而不是复杂对象？

LangChain4j 工具可以返回多种类型，但返回字符串更容易让模型直接理解和总结。

图表场景则用特殊前缀 `CHART_JSON:`，让前端知道这里不是普通文本，而是图表配置。

### 15.3 为什么需要系统提示词？

系统提示词相当于 Agent 的“岗位说明书”和“行为边界”。

它告诉模型：

- 你是谁。
- 你会什么。
- 你不能做什么。
- 你应该怎么回答。
- 图表数据应该怎么输出。

### 15.4 为什么还要做后端权限控制？

因为模型可能误解用户意图，也可能被用户诱导。权限属于安全边界，必须由代码控制。

### 15.5 前端为什么要解析 `CHART_JSON:`？

因为大模型输出的是文本流，前端需要一种简单协议区分“普通回答”和“图表配置”。

`CHART_JSON:` 就是本项目自定义的轻量协议。

---

## 16. 你可以动手练习的任务

### 任务 1：增加一个新快捷问题

修改：`jc-sales-agent-front/src/views/ChatView.vue`

在 `quickQuestions` 或 `welcomeCards` 中增加一个问题，例如：

```text
本月各大区销售占比，生成饼图
```

观察 Agent 是否会调用 `ChartGeneratorTool.generatePieChart`。

### 任务 2：增加一个新工具

例如新增“查询毛利率最高产品”的工具：

1. 在 Repository 加聚合查询。
2. 在 Service 封装方法。
3. 在 Tool 中增加 `@Tool` 方法。
4. 在 `SalesAgentConfig` 注册工具 Bean。
5. 用自然语言测试。

### 任务 3：增强图表协议

现在只支持一个 `CHART_JSON:`。

可以思考如何支持：

- 多个图表。
- 表格数据。
- 下载报告。

### 任务 4：完善权限演示

把 `app.auth.enabled` 改成 `true`，使用不同角色登录，观察普通销售员和经理能看到的数据范围是否不同。

---

## 17. 总结

这个项目的核心不是“简单聊天”，而是把大模型接入真实业务系统：

```text
自然语言输入
  -> Agent 理解意图
  -> 调用后端工具
  -> 查询业务数据库
  -> 计算指标/生成图表/检测异常
  -> 组织中文分析结论
  -> 前端流式展示
```

学习本项目时，要抓住三条主线：

1. **业务数据线**：订单、产品、销售员、大区、排名、趋势、异常。
2. **Agent 工具线**：System Prompt、Tool、Memory、LangChain4j 编排。
3. **前后端交互线**：Vue 输入、SSE 流式输出、图表 JSON 解析、ECharts 渲染。

只要按这三条线看代码，就能从“完全不了解”逐步理解智能销售 Agent 的完整实现。
