-- ============================================================================
-- jc-voice-shopping 完整数据库 Schema
-- 目标：PostgreSQL 15+
-- 覆盖：商家、商品、画像、会话、订单、工单、合规、安全、隐私全模块
--
-- 使用：
--   createdb jc_voice_shopping
--   psql -d jc_voice_shopping -f schema.sql
--   psql -d jc_voice_shopping -f data.sql   # 导入测试数据
-- ============================================================================

-- 扩展
CREATE EXTENSION IF NOT EXISTS pgcrypto;        -- 列加密用（敏感字段 AES）
CREATE EXTENSION IF NOT EXISTS pg_trgm;         -- LIKE 加速（向量检索故障时的降级链路用）
CREATE EXTENSION IF NOT EXISTS vector;          -- pgvector：向量检索


-- ============================================================================
-- 一、商家与用户
-- ============================================================================

CREATE TABLE merchant (
                          id              BIGSERIAL    PRIMARY KEY,
                          name            VARCHAR(128) NOT NULL,
                          status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / SUSPENDED
                          scale_level     VARCHAR(16)  NOT NULL DEFAULT 'SMB',      -- HEAD / MID / SMB / NEW
                          contact_email   VARCHAR(128),
                          created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
                          id              BIGSERIAL    PRIMARY KEY,
                          username        VARCHAR(64)  NOT NULL UNIQUE,
                          nickname        VARCHAR(64),
                          phone_enc       TEXT,                                      -- AES 加密（敏感字段必须加密落库）
                          email_enc       TEXT,
                          status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',    -- ACTIVE / ERASED
                          erased_at       TIMESTAMP,
                          created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_app_user_status ON app_user(status);


-- ============================================================================
-- 二、商品与评论
-- ============================================================================

CREATE TABLE product (
                         id              BIGSERIAL     PRIMARY KEY,
                         merchant_id     BIGINT        NOT NULL REFERENCES merchant(id),
                         sku_code        VARCHAR(64)   NOT NULL UNIQUE,
                         name            VARCHAR(255)  NOT NULL,
                         category_l1     VARCHAR(32)   NOT NULL,                   -- 一级分类
                         category_l2     VARCHAR(64)   NOT NULL,                   -- 二级分类
                         brand           VARCHAR(64),
                         price           NUMERIC(10,2) NOT NULL,
                         original_price  NUMERIC(10,2),
                         stock           INT           NOT NULL DEFAULT 0,

    -- 灵活属性（跑鞋的缓震 / 手表的机芯 各自扩展）
                         attributes      JSONB         NOT NULL DEFAULT '{}',
    -- 示例：{"cushion":"high","weight":"medium","gender":"male","size_range":[39,46]}

    -- 文本，用于向量化
                         description     TEXT,
                         selling_points  TEXT,

    -- 运营
                         status          VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE',
                         is_new_arrival  BOOLEAN       NOT NULL DEFAULT FALSE,
                         is_long_tail    BOOLEAN       NOT NULL DEFAULT FALSE,     -- 长尾标记，公平性治理用

    -- 向量（pgvector）：基于"名称 + 品牌 + 品类 + 卖点 + 描述"拼接文本生成
    -- 维度 1024 对应 text-embedding-v3 默认输出
                         embedding       VECTOR(1024),

                         created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
                         updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_product_merchant     ON product(merchant_id);
CREATE INDEX idx_product_category     ON product(category_l2, status);
CREATE INDEX idx_product_attr_gin     ON product USING GIN(attributes);
CREATE INDEX idx_product_name_trgm    ON product USING GIN(name gin_trgm_ops);   -- LIKE 降级
-- 向量近邻索引（HNSW 比 IVFFlat 无需训练、召回率更稳）
-- 生产规模 10w+ 再建议调 m/ef_construction
CREATE INDEX idx_product_embedding_hnsw
    ON product USING HNSW (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE TABLE product_review_summary (
                                        product_id      BIGINT        PRIMARY KEY REFERENCES product(id),
                                        review_count    INT           NOT NULL DEFAULT 0,
                                        avg_rating      NUMERIC(3,2)  NOT NULL DEFAULT 0,
                                        pos_highlight   TEXT,                                     -- 好评摘要
                                        neg_highlight   TEXT,                                     -- 差评摘要
                                        updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- FAQ 知识库（语义召回 + 命中后直接念答案）
CREATE TABLE faq_entry (
                           id              BIGSERIAL     PRIMARY KEY,
                           merchant_id     BIGINT        NOT NULL DEFAULT 0,         -- 0 = 平台通用
                           question        VARCHAR(512)  NOT NULL,
                           answer          TEXT          NOT NULL,
                           category        VARCHAR(32)   NOT NULL,                   -- 物流 / 售后 / 支付 / 发票
                           status          VARCHAR(16)   NOT NULL DEFAULT 'PUBLISHED',
                           hit_count       INT           NOT NULL DEFAULT 0,
                           embedding       VECTOR(1024),                             -- 基于 question 生成
                           updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_faq_merchant ON faq_entry(merchant_id, status);
CREATE INDEX idx_faq_embedding_hnsw
    ON faq_entry USING HNSW (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);


-- ============================================================================
-- 三、用户画像 + 同意管理
-- ============================================================================

-- 静态画像：改动不频繁
CREATE TABLE user_profile_static (
                                     user_id         BIGINT        PRIMARY KEY REFERENCES app_user(id),
                                     gender          VARCHAR(8),
                                     age             INT,
                                     city            VARCHAR(32),
                                     height_cm       INT,
                                     weight_kg       INT,
                                     skin_type       VARCHAR(16),                              -- 干皮 / 油皮 / 敏感
                                     tech_savvy      VARCHAR(16),                              -- novice / mid / expert
                                     budget_band     VARCHAR(16),                              -- budget / mid / premium
                                     locale          VARCHAR(16)   DEFAULT 'zh_cn',            -- 多语言场景用
                                     updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 动态画像：行为累加
CREATE TABLE user_profile_dynamic (
                                      user_id             BIGINT       PRIMARY KEY REFERENCES app_user(id),
                                      category_affinity   JSONB        NOT NULL DEFAULT '{}',
    -- {"跑鞋":0.82,"T恤":0.45,"数码":0.23}
                                      brand_affinity      JSONB        NOT NULL DEFAULT '{}',
                                      recent_viewed       BIGINT[]     NOT NULL DEFAULT '{}',
                                      recent_purchased    BIGINT[]     NOT NULL DEFAULT '{}',
                                      price_sensitivity   NUMERIC(3,2),
                                      avg_order_amount    NUMERIC(10,2),
                                      updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 用户同意状态（GDPR / 个保法用）
CREATE TABLE user_consent (
                              user_id                 BIGINT        PRIMARY KEY REFERENCES app_user(id),
                              voice_enabled           BOOLEAN       NOT NULL DEFAULT FALSE,
                              profile_store_enabled   BOOLEAN       NOT NULL DEFAULT TRUE,
                              consent_version         VARCHAR(16),
                              consented_at            TIMESTAMP,
                              withdrawn_at            TIMESTAMP
);


-- ============================================================================
-- 四、会话
-- ============================================================================

CREATE TABLE session (
                         id                  VARCHAR(64)   PRIMARY KEY,             -- UUID
                         user_id             BIGINT        NOT NULL REFERENCES app_user(id),
                         merchant_id         BIGINT,                                 -- 商家绑定（可选，平台会话为 NULL）
                         started_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
                         ended_at            TIMESTAMP,
                         channel             VARCHAR(16)   NOT NULL,                 -- HOME_ENTRY / PRODUCT_PAGE / SEARCH_FALLBACK
                         bound_product_id    BIGINT,                                 -- 从商详进入时的商品
                         locale              VARCHAR(16)   DEFAULT 'zh_cn',
                         outcome             VARCHAR(16),                            -- ORDERED / ABANDONED / FOLLOWUP / HANDOFF
                         total_tokens        INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_session_user      ON session(user_id, started_at DESC);
CREATE INDEX idx_session_merchant  ON session(merchant_id, started_at DESC);

CREATE TABLE session_message (
                                 id                  BIGSERIAL     PRIMARY KEY,
                                 session_id          VARCHAR(64)   NOT NULL REFERENCES session(id),
                                 turn                INT           NOT NULL,
                                 role                VARCHAR(16)   NOT NULL,                 -- USER / ASSISTANT / SYSTEM
                                 agent_name          VARCHAR(32),                            -- 产出 Agent（ASSISTANT 才有）
                                 content_text        TEXT,                                   -- 已 PiiMask 的脱敏版本
                                 content_audio_url   VARCHAR(255),
                                 tokens              INT           NOT NULL DEFAULT 0,
                                 created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_msg_session ON session_message(session_id, turn);

CREATE TABLE session_state (
                               session_id              VARCHAR(64)  PRIMARY KEY REFERENCES session(id),
                               phase                   VARCHAR(32)  NOT NULL,              -- INTENT / CLARIFY / RECOMMEND / ORDER_CONFIRM / HANDOFF_PENDING / ENDED
                               current_intent          VARCHAR(32),
                               slots                   JSONB        NOT NULL DEFAULT '{}', -- 已填槽位
                               pending_ask             VARCHAR(255),                       -- 当前在问什么
                               last_recommendations    BIGINT[],
                               updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);


-- ============================================================================
-- 五、订单
-- ============================================================================

CREATE TABLE order_record (
                              id                  BIGSERIAL     PRIMARY KEY,
                              order_no            VARCHAR(64)   NOT NULL UNIQUE,
                              user_id             BIGINT        NOT NULL REFERENCES app_user(id),
                              merchant_id         BIGINT        NOT NULL REFERENCES merchant(id),
                              session_id          VARCHAR(64)   NOT NULL,                 -- 归因到具体会话
                              product_id          BIGINT        NOT NULL REFERENCES product(id),
                              sku_code            VARCHAR(64)   NOT NULL,
                              quantity            INT           NOT NULL,
                              unit_price          NUMERIC(10,2) NOT NULL,
                              total_amount        NUMERIC(10,2) NOT NULL,
                              status              VARCHAR(16)   NOT NULL,                 -- CREATED / PAID / SHIPPED / COMPLETED / CANCELLED
                              agent_attribution   BOOLEAN       NOT NULL DEFAULT TRUE,    -- AI 导购归因
    -- 收件人信息（脱敏或加密存）
                              receiver_name       VARCHAR(64),
                              receiver_phone_enc  TEXT,                                   -- AES 加密
                              receiver_addr_enc   TEXT,
                              created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_user      ON order_record(user_id, created_at DESC);
CREATE INDEX idx_order_merchant  ON order_record(merchant_id, created_at DESC);
CREATE INDEX idx_order_session   ON order_record(session_id);


-- ============================================================================
-- 六、客服与工单
-- ============================================================================

CREATE TABLE ticket (
                        id                  BIGSERIAL     PRIMARY KEY,
                        user_id             BIGINT        NOT NULL REFERENCES app_user(id),
                        merchant_id         BIGINT        REFERENCES merchant(id),
                        session_id          VARCHAR(64),
                        reason              VARCHAR(255),
                        status              VARCHAR(16)   NOT NULL DEFAULT 'PENDING', -- PENDING / ASSIGNED / IN_PROGRESS / CLOSED
                        priority            VARCHAR(8)    NOT NULL DEFAULT 'P3',    -- P0 / P1 / P2 / P3
                        conversation_dump   TEXT,
                        slots_snapshot      JSONB,
                        assignee            VARCHAR(64),                            -- 客服工号
                        resolution          TEXT,
                        created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
                        closed_at           TIMESTAMP
);
CREATE INDEX idx_ticket_status ON ticket(status, created_at);
CREATE INDEX idx_ticket_user   ON ticket(user_id);


-- ============================================================================
-- 七、合规、安全、审计
-- ============================================================================

-- 合规事件（Agent 输出审查）
-- 字段同时覆盖"事件归类"与"规则命中详情"两种诉求
CREATE TABLE compliance_event (
                                  id              BIGSERIAL     PRIMARY KEY,
                                  session_id      VARCHAR(64),
                                  user_id         BIGINT,
                                  stage           VARCHAR(32),                                -- INTENT / CLARIFY / RECOMMEND / EMOTION
                                  event_type      VARCHAR(32),                                -- SENSITIVE_WORD / ABSOLUTE_CLAIM / PRICE_PROMISE / ...
                                  rule_type       VARCHAR(32),                                -- BLACK / REWRITE / ABSOLUTE / WARN / LLM_*
                                  severity        VARCHAR(16),                                -- LOW / MEDIUM / HIGH / CRITICAL
                                  matched         VARCHAR(255),
                                  original_text   TEXT,
                                  redacted_text   TEXT,
                                  action          VARCHAR(16),                                -- BLOCKED / REWRITTEN / WARNED
                                  created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_compliance_ruletype ON compliance_event(rule_type, created_at);
CREATE INDEX idx_compliance_severity ON compliance_event(severity, created_at);
CREATE INDEX idx_compliance_session  ON compliance_event(session_id, created_at);

-- 安全事件（Prompt 注入 / 越权检测）
CREATE TABLE security_event (
                                id              BIGSERIAL     PRIMARY KEY,
                                user_id         BIGINT,
                                session_id      VARCHAR(64),
                                event_type      VARCHAR(32),                                -- PROMPT_INJECTION / TOOL_DENY / OUTPUT_LEAK / SCOPE_VIOLATION
                                severity        VARCHAR(16),                                -- LOW / MEDIUM / HIGH / CRITICAL
                                detail          JSONB,
                                created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sec_type_time ON security_event(event_type, created_at);
CREATE INDEX idx_sec_user      ON security_event(user_id, created_at);

-- 数据访问审计（谁读了谁的数据）
CREATE TABLE data_access_audit (
                                   id              BIGSERIAL     PRIMARY KEY,
                                   operator        VARCHAR(64)   NOT NULL,                    -- 人员工号 / 服务名
                                   operator_role   VARCHAR(32),                               -- MERCHANT / PLATFORM / SYSTEM
                                   op_type         VARCHAR(32),                               -- READ / UPDATE / ERASE / EXPORT
                                   target_type     VARCHAR(32),                               -- USER_PROFILE / ORDER / MESSAGE
                                   target_id       VARCHAR(64),
                                   ts              TIMESTAMP     NOT NULL DEFAULT NOW(),
                                   ip              VARCHAR(64),
                                   user_agent      VARCHAR(255)
);
CREATE INDEX idx_dapi_target ON data_access_audit(target_type, target_id, ts);

-- 公平性申诉（商家对推荐曝光不公的申诉记录）
CREATE TABLE fairness_appeal (
                                 id              BIGSERIAL     PRIMARY KEY,
                                 merchant_id     BIGINT        NOT NULL REFERENCES merchant(id),
                                 reason          VARCHAR(255),
                                 period_start    DATE,
                                 period_end      DATE,
                                 status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',  -- PENDING / PROCESSING / RESOLVED
                                 handler         VARCHAR(64),
                                 resolution      TEXT,
                                 created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
                                 closed_at       TIMESTAMP
);


-- ============================================================================
-- 八、物化视图：推荐日志聚合（可选，公平性看板用）
-- ============================================================================

-- 生产里 recommend_log 建议走 ClickHouse / 日志系统，这里给一个 PG 的兜底版
CREATE TABLE recommend_log (
                               id              BIGSERIAL     PRIMARY KEY,
                               session_id      VARCHAR(64)   NOT NULL,
                               user_id         BIGINT        NOT NULL,
                               product_id      BIGINT        NOT NULL,
                               merchant_id     BIGINT        NOT NULL,
                               position        INT           NOT NULL,                    -- 展示位次
                               score           NUMERIC(6,4),
                               reason          TEXT,
                               clicked         BOOLEAN       NOT NULL DEFAULT FALSE,
                               ts              TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rec_log_merchant ON recommend_log(merchant_id, ts);
CREATE INDEX idx_rec_log_product  ON recommend_log(product_id, ts);


-- ============================================================================
-- 完
-- ============================================================================
