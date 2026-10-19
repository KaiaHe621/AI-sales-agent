# 智能销售数据分析 Agent（sales-agent）

基于 LangChain4j 的销售数据分析 Agent 后端服务。销售团队通过自然语言对话完成订单查询、业绩排名、趋势分析、图表生成和异常检测，底层数据来自 MySQL，模型调用 DeepSeek。

```
你："近 6 个月的销售趋势怎么样？画个折线图"
Agent：调用 getMonthlyTrend 拿到数据 → 调用 generateLineChart 生成 ECharts option
      → 文字分析 + CHART_JSON 协议输出 → 前端渲染折线图
```

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 21、Spring Boot 3.5.11 |
| Agent 框架 | LangChain4j 1.12.1（`AiServices` 手动装配） |
| 模型 | DeepSeek `deepseek-chat`（OpenAI 兼容接口，`api.deepseek.com/v1`） |
| 数据库 | MySQL 8（JPA + 原生 JPQL 聚合查询） |
| 缓存 | Redis（Spring Cache + 手动 RedisTemplate 封装） |
| 权限 | Sa-Token 1.39.0 + ThreadLocal 用户上下文 |
| 流式输出 | Spring WebFlux SSE（`Flux<ServerSentEvent>`） |
| 可观测性 | Actuator + Micrometer token 计数 + 工具调用日志 |

## 快速开始

依赖：JDK 21、MySQL 8、Redis、`DEEPSEEK_API_KEY`。

```bash
export DEEPSEEK_API_KEY=sk-xxx

cd sales-agent
mvn spring-boot:run
```

启动时 Spring 自动执行 `src/main/resources/db/schema.sql`（建表）和 `db/data.sql`（种子数据），无需手工初始化。服务监听 `8087`。

**注意**：`application.yml` 里 `app.auth.enabled` 默认是 `false`（跳过登录校验，方便本地调试）。设成 `true` 后除 `/auth/login`、`/actuator/**` 外的所有接口都要带 Sa-Token。

种子数据里的测试账号（登录只需要 `repId`，无密码）：

| repId | 姓名 | 角色 | 数据范围 |
|---|---|---|---|
| 13 | 黄总 | `SALES_DIRECTOR` | 全公司 |
| 1 | 李明 | `SALES_MANAGER` | 华东区 |
| 2 | 张伟 | `SALES_REP` | 仅自己 |

```bash
curl -X POST http://localhost:8087/auth/login -H 'Content-Type: application/json' -d '{"repId": 13}'
# → {"token": "...", "username": "黄总", "role": "SALES_DIRECTOR"}

curl -X POST http://localhost:8087/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "s1", "message": "本月华东区业绩怎么样？"}'
```

## 整体架构

```
POST /agent/chat（阻塞） / POST /agent/chat/stream（SSE）
        │
        ▼
SalesAgent 接口（AiServices 动态代理）
        │  系统提示词（{{today}} 注入）+ 工具调用循环 + 会话记忆
        ├── SalesQueryTool.queryOrders
        ├── SalesSummaryTool（4 个 @Tool）
        ├── SalesTrendTool（3 个 @Tool）
        ├── ChartGeneratorTool（3 个 @Tool，返回 CHART_JSON:...）
        └── AnomalyDetectionTool.detectAllAnomalies
                    │
                    ▼
        SalesQueryService（角色数据范围 + @Cacheable）
                    │
        JPA Repository ── MySQL（sa_* 表）
        RedisCacheManager / RedisTemplate ── Redis
```

请求进入 Controller 前经过 Sa-Token 登录校验拦截器，通过后把用户信息写入 `UserContext`（ThreadLocal），工具层和服务层从 `UserContext` 读当前角色做数据权限控制。

## 核心设计

### Agent 装配（`agent/SalesAgentConfig.java`）

不用 `@AiService` 注解，而是手动 `AiServices.builder()` 创建 Bean，理由是可以显式控制装配细节：

- 同时注入阻塞式 `ChatModel` 和 `StreamingChatModel`，一个接口提供 `chat()` / `chatStream()` 两个方法；
- `chatMemoryProvider` 按 `sessionId` 创建 `MessageWindowChatMemory`（窗口 20 条），记忆落 MySQL（见下文）；
- `beforeToolExecution` / `afterToolExecution` 钩子打印每次工具调用的入参和结果长度，排查"模型为什么没调工具 / 参数为什么错"时看日志即可。

系统提示词有两个关键设计（`agent/SalesAgent.java`）：

1. **时间锚定**：通过 `@V("today")` 把当天日期注入提示词，明确"本月 / 上个月 / 近 N 个月"的语义边界。LLM 不知道今天是几号，不锚定时间它会把日期算错。
2. **图表输出规则**：约束模型遇到 `CHART_JSON:` 前缀时必须原样输出、不得改写或包代码块——这是图表协议能稳定工作的前提。

### 工具清单（`tool/`）

每个 `@Tool` 的描述里写明了适用场景和"不适合的场景"，引导模型在 11 个工具里选对的那个：

| 工具方法 | 场景 | 说明 |
|---|---|---|
| `queryOrders` | 订单明细 | 返回格式化文本；limit 最大 50 |
| `getTopReps` | 销售员排名 | Top N，`topN` 最大 20 |
| `getRegionRanking` | 大区排名 | 含占比和全公司合计 |
| `getTopProducts` | 产品排名 | `topN` 传负数查最差 N 名 |
| `getSalesSummary` | 汇总 | 总销售额 |
| `calcMonthOverMonth` | 环比 | `prevStart/prevEnd` 传 null 自动取上一个等长周期 |
| `calcYearOverYear` | 同比 | 自动减一年 |
| `getMonthlyTrend` | 月度趋势 | 近 N 个月（最大 24），逐月环比 |
| `generateLineChart` | 折线图 | 返回 `CHART_JSON:` + ECharts option |
| `generateBarChart` | 柱状图 | 维度：region / rep |
| `generatePieChart` | 饼图 | 维度：region / category |
| `detectAllAnomalies` | 异常检测 | 无参数，按当前角色自动扫描（见下） |

工具的错误处理约定：**不抛异常，返回错误描述字符串**。工具返回值会直接进入模型上下文，抛异常会导致 Agent 调用失败；返回"日期格式错误，请使用 yyyy-MM-dd"这种文本，模型会自己修正参数重试。

### CHART_JSON 协议

SSE 流是纯文本，没法传结构化图表数据，所以约定：图表工具返回 `CHART_JSON:` 前缀 + ECharts `option` JSON，模型在回复中原样输出这行字符串，前端解析出 JSON 直接交给 ECharts 渲染。协议能成立依赖三件事：

1. 工具只拼 ECharts 合法的 `option`（title / tooltip / xAxis / yAxis / series）；
2. 系统提示词硬性约束模型不得改写；
3. 前端按前缀逐块解析（`sales-agent-front` 的 `ChartRenderer.vue`）。

### 会话记忆（`memory/MysqlChatMemoryStore.java`）

实现 LangChain4j 的 `ChatMemoryStore` 接口，把消息列表用 `ChatMessageSerializer.messagesToJson()` 序列化成 JSON 存 `sa_chat_memory` 表（sessionId 唯一），读取时反序列化还原。`MessageWindowChatMemory` 保证每会话最多保留 20 条，防止上下文无限膨胀。清空会话走 `DELETE /agent/session/{sessionId}`。

### 权限模型（`security/` + `config/WebMvcConfig.java`）

三级角色：`SALES_DIRECTOR`（全公司）→ `SALES_MANAGER`（本大区）→ `SALES_REP`（仅自己）。

- **登录**：`/auth/login` 传 `repId`（演示项目无密码），Sa-Token 签发 token，`username/role/regionId/repId` 写进 Sa-Token Session；
- **拦截器**：`WebMvcConfig` 注册两级拦截器——`SaInterceptor` 做 `checkLogin()`，第二个拦截器把 Session 里的用户信息灌进 `UserContext`（ThreadLocal），请求结束 `afterCompletion` 里清理，防止线程池复用导致串号；
- **两道防线**：
  - `ToolInputValidator`（`security/ToolInputValidator.java`）：工具参数白名单校验——大区名、图表类型、维度都必须是枚举值，日期必须匹配 `yyyy-MM-dd`。防的是 LLM 幻觉出非法参数（比如编一个大区名、把维度写成 SQL）；
  - `SalesQueryService.queryOrders`：服务层强制数据范围——销售员查订单时 `repId` 被覆盖为本人，主管的 `regionId` 被覆盖为管辖大区，即使模型传了越权参数也拿不到数据；
- **异常检测按角色扫描**：总监扫全公司，主管扫本大区，销售员只算自己的退单率和业绩下滑。

### 缓存（`config/RedisConfig.java`）

- 排名、月度趋势、大区元数据走 Spring `@Cacheable`，`RedisCacheManager` 按缓存区配置 TTL：`rep-ranking` / `region-ranking` / `monthly-trend` 5 分钟，`region-meta` 30 分钟，`anomaly-detection` 2 分钟；
- Redis 值用 `GenericJackson2JsonRedisSerializer` 序列化，`activateDefaultTyping`（`WRAPPER_ARRAY` 模式）保证反序列化还原成 DTO 而不是 `LinkedHashMap`；
- `SalesQueryCacheService` 是手写的 RedisTemplate 封装（`total_amount:*` 键，TTL 可配 `sales-agent.cache.query-ttl-seconds`），给不走 `@Cacheable` 的总额查询复用。

### 流式输出（`controller/SalesAgentStreamController.java`）

`POST /agent/chat/stream` 返回 `text/event-stream`，把 LangChain4j 的 `TokenStream` 回调桥接成 SSE 事件：

| 事件 | data | 时机 |
|---|---|---|
| `token` | 词片 | 每产生一个 token 推送一次 |
| `done` | `[DONE]` | 完整响应结束 |
| `error` | 错误提示 | 模型调用失败 |

注意 `application.yml` 里 `web-application-type: servlet`：项目同时引入了 WebFlux（为 SSE）和 MVC，强制 servlet 模式避免 actuator 端点映射冲突。

### 可观测性（`config/TokenUsageLogger.java`）

- `ChatModelListener` 实现，把每次模型调用的 token 用量累加到 Micrometer Counter（`llm.tokens.input` / `llm.tokens.output`），并打印费用估算日志；
- `SalesAgentConfig` 里注册了工具调用前后日志钩子；
- Actuator 暴露 `health,info,metrics`。

## API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | body `{"repId": 13}` → `{token, username, role}` |
| POST | `/auth/logout` | 退出登录 |
| POST | `/agent/chat` | body `{sessionId, message}` → `{sessionId, reply, durationMs}`，阻塞式 |
| POST | `/agent/chat/stream` | 同上，SSE 流式返回 |
| DELETE | `/agent/session/{sessionId}` | 清空该会话记忆 |
| POST | `/test/tool/*` | 11 个工具直调端点（`ToolTestController`），绕过 LLM 调试工具逻辑 |

`ChatRequest` 校验：`sessionId` 非空且 ≤100 字符，`message` 非空且 ≤2000 字符。

## 数据库设计（`src/main/resources/db/`）

| 表 | 说明 |
|---|---|
| `sa_sales_region` | 4 个大区（华东/华南/华北/西南） |
| `sa_sales_rep` | 13 个销售员，含 4 名主管 + 1 名全国总监 |
| `sa_product` | 20 个 SKU，4 个品类 |
| `sa_sales_order` | 订单，含 amount / cost / profit / status（COMPLETED/REFUNDED/CANCELLED） |
| `sa_chat_memory` | 对话记忆 JSON |

种子数据刻意做了工程化处理：所有订单日期基于 `CURDATE()` 动态偏移，保证任何时候启动都有完整的约 7 个月历史数据（同比、趋势查询可用）；并**预埋了 4 个异常点**，方便演示异常检测：

1. 华北区近 14 天无订单 → 大区订单量骤降预警；
2. `SKU-8821`（智能手表 Pro）近 30 天零销售 → 断货预警；
3. 张磊（repId=8）近 60 天仅 1 单 → 业绩断崖预警；
4. 王芳（repId=3）退单率异常高 → 退单预警。

## 关键配置项（`application.yml`）

| 配置 | 默认值 | 说明 |
|---|---|---|
| `DEEPSEEK_API_KEY` | - | 必填环境变量 |
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `localhost:3306/jc-ai` / `root` / `123456` | 数据源 |
| `app.auth.enabled` | `false` | 登录校验开关，生产必须 `true` |
| `sales-agent.tool.anomaly-threshold-days` | `5` | 零销售预警阈值（天） |
| `sales-agent.tool.trend-drop-threshold` | `0.3` | 订单量骤降阈值（30%） |
| `sales-agent.cache.query-ttl-seconds` | `300` | 手写缓存 TTL |
| `langchain4j.open-ai.chat-model.temperature` | `0.1` | 数据分析场景低温度，减少随机性 |

## 目录结构

```
src/main/java/com/jichi/salesAgent/
├── SalesAgentApplication.java        # 入口
├── agent/                            # Agent 接口 + AiServices 装配
│   ├── SalesAgent.java               # chat() / chatStream()，系统提示词
│   └── SalesAgentConfig.java         # 工具注册、记忆、调用日志钩子
├── controller/                       # HTTP 层
│   ├── AuthController.java           # 登录/登出
│   ├── SalesAgentController.java     # 阻塞式对话 + 清空会话
│   ├── SalesAgentStreamController.java  # SSE 流式对话
│   ├── ToolTestController.java       # 工具直调（调试用）
│   └── ChatRequest.java / ChatResponse.java
├── tool/                             # 5 个工具类、12 个 @Tool 方法
├── service/
│   ├── SalesQueryService.java        # 聚合查询 + 角色数据范围 + @Cacheable
│   └── SalesQueryCacheService.java   # 手写 Redis 缓存封装
├── security/
│   ├── UserContext.java              # ThreadLocal 用户上下文
│   └── ToolInputValidator.java       # 工具参数白名单校验
├── memory/MysqlChatMemoryStore.java  # 对话记忆 MySQL 持久化
├── config/                           # WebMvcConfig / RedisConfig / 异常处理 / Token 日志
├── entity/  repository/  dto/
└── resources/
    ├── application.yml
    └── db/schema.sql + data.sql      # 建表 + 种子数据（自动执行）
```

## 测试

`SalesAgentSmokeTest` 是真实调用 DeepSeek 的冒烟测试（需要 MySQL、Redis、`DEEPSEEK_API_KEY` 都就绪）：

```bash
mvn test
```

- `smokeTest`：验证 Agent 基本对话；
- `toolCallTest`：问"近 6 个月的月度销售趋势是什么？"验证工具调用链路。

## 扩展指南：新增一个工具

1. 在 `tool/` 下新建组件类，方法上打 `@Tool("清晰描述适用场景和【不适合】场景")`，参数用 `@P` 写清格式和取值约束；
2. 需要防幻觉的参数在 `ToolInputValidator` 里加白名单校验；
3. 在 `SalesAgentConfig.salesAgent()` 的 `.tools(...)` 里注册；
4. 返回结果用格式化文本；涉及图表用 `CHART_JSON:` 前缀；出错返回错误描述字符串而不是抛异常；
5. 在 `ToolTestController` 加一个直调端点，先绕过 LLM 验证工具本身正确，再验证模型的调用行为。

## 已知限制

- 登录只认 `repId` 无密码，仅适合演示环境；
- 排名/汇总类工具未按角色过滤数据范围（只有订单明细查询和异常检测做了强制范围控制），如果对权限敏感需要自行补齐；
- 缓存 TTL 内数据变更不会实时反映（排名 5 分钟）；
- Agent 只读不写，所有工具都是查询类。
