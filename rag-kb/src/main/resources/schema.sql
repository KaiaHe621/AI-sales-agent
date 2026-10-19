-- ================================================================
-- jc-rag-kb 数据库初始化脚本
-- 执行前确保已创建数据库：CREATE DATABASE ragkb;
-- ================================================================

-- 启用 PGVector 扩展（必须，向量存储依赖）
CREATE
EXTENSION IF NOT EXISTS vector;

-- ================================================================
-- 1. 知识库表
-- ================================================================
CREATE TABLE kb_knowledge_base
(
    id BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    department_id VARCHAR(50)  NOT NULL,               -- 归属部门
    is_public     BOOLEAN      NOT NULL DEFAULT FALSE, -- 是否对所有人开放
    created_by    BIGINT       NOT NULL,               -- 创建者 userId
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE kb_knowledge_base IS '知识库，一个部门可以有多个知识库';
CREATE INDEX idx_kb_department ON kb_knowledge_base (department_id) WHERE is_deleted = FALSE;

-- ================================================================
-- 2. 知识库权限表
-- ================================================================
CREATE TABLE kb_permission
(
    id BIGSERIAL PRIMARY KEY,
    kb_id        BIGINT      NOT NULL,
    subject_type VARCHAR(20) NOT NULL, -- DEPARTMENT / USER
    subject_id   VARCHAR(50) NOT NULL, -- 部门ID 或 userId
    permission   VARCHAR(20) NOT NULL, -- READ / WRITE / ADMIN
    granted_by   BIGINT      NOT NULL,
    granted_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (kb_id, subject_type, subject_id)
);

CREATE INDEX idx_permission_subject ON kb_permission (subject_type, subject_id);

-- ================================================================
-- 3. 文档表
-- ================================================================
CREATE TABLE kb_document
(
    id BIGSERIAL PRIMARY KEY,
    kb_id       BIGINT       NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(20)  NOT NULL,           -- PDF / DOCX / MD / TXT
    file_size   BIGINT       NOT NULL,           -- 字节数
    minio_path  VARCHAR(500) NOT NULL,           -- MinIO 中的对象路径
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING / PROCESSING / DONE / FAILED
    error_msg   TEXT,                            -- 失败原因
    chunk_count INT                   DEFAULT 0, -- 索引后的分块数量
    token_count INT                   DEFAULT 0, -- 向量化消耗的 Token 数
    version     INT          NOT NULL DEFAULT 1, -- 文档版本号，更新时递增
    uploaded_by BIGINT       NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    indexed_at  TIMESTAMP,                       -- 最近一次索引完成时间
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE kb_document IS '上传到知识库的文档，一个文档对应多个分块';
COMMENT ON COLUMN kb_document.version IS '每次重建索引版本号加1，旧版本分块通过版本号识别并删除';

CREATE INDEX idx_doc_kb_id ON kb_document (kb_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_doc_status ON kb_document (status) WHERE is_deleted = FALSE;

-- ================================================================
-- 4. 文档分块表（核心表，含向量字段）
-- ================================================================
CREATE TABLE kb_doc_chunk (
                              id              BIGSERIAL PRIMARY KEY,
                              doc_id          BIGINT          NOT NULL,
                              kb_id           BIGINT          NOT NULL,           -- 冗余存储，检索时避免 JOIN
                              chunk_index     INT             NOT NULL,           -- 在文档中的顺序（0-based）
                              content         TEXT            NOT NULL,           -- 分块原文
                              content_tsv     TSVECTOR,                          -- 全文检索索引（自动维护）
                              embedding       VECTOR(1024)    NOT NULL,           -- 向量（text-embedding-v3 是 1024 维）
                              page_num        INT,                               -- 来自文档第几页（PDF 专用）
                              section_title   VARCHAR(500),                      -- 所在章节标题（如果能识别）
                              token_count     INT             NOT NULL DEFAULT 0, -- 该块的 Token 估算数
                              doc_version     INT             NOT NULL,           -- 对应的文档版本号
                              created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE kb_doc_chunk IS '文档分块表，每条记录是一个可检索的最小单元';
COMMENT ON COLUMN kb_doc_chunk.content_tsv IS '全文检索向量，由触发器自动更新';
COMMENT ON COLUMN kb_doc_chunk.doc_version IS '冗余版本号，重建索引后删除旧版本时使用';

-- 向量检索索引（HNSW，适合高并发检索）
-- m=16: 每个节点的最大连接数，越大越准但更占内存
-- ef_construction=128: 构建索引时的搜索宽度，越大越准但建索引更慢
CREATE INDEX idx_chunk_embedding ON kb_doc_chunk
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 128);

-- 全文检索索引
CREATE INDEX idx_chunk_content_tsv ON kb_doc_chunk USING GIN (content_tsv);

-- 按 kb_id 过滤的索引（多租户场景必须有）
CREATE INDEX idx_chunk_kb_id ON kb_doc_chunk (kb_id);
CREATE INDEX idx_chunk_doc_id ON kb_doc_chunk (doc_id);

-- 触发器：自动维护全文检索向量
-- 简单版：用默认英文分词（中文效果一般，但不需要额外扩展）
-- 注意：中文全文检索效果不佳，主要靠向量检索；全文检索作为补充用于精确词搜索
CREATE
OR
REPLACE FUNCTION update_chunk_tsv()
RETURNS TRIGGER AS $$
BEGIN NEW.content_tsv := to_tsvector('simple', NEW.content);
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_chunk_tsv
    BEFORE INSERT OR UPDATE OF content
    ON kb_doc_chunk
    FOR EACH ROW
EXECUTE FUNCTION update_chunk_tsv();

-- ================================================================
-- 5. 索引任务表（异步索引状态管理）
-- ================================================================
CREATE TABLE kb_index_task
(
    id BIGSERIAL PRIMARY KEY,
    doc_id      BIGINT      NOT NULL,
    task_type   VARCHAR(20) NOT NULL DEFAULT 'INDEX', -- INDEX / REINDEX
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT         NOT NULL DEFAULT 0,
    max_retry   INT         NOT NULL DEFAULT 3,
    error_msg   TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    started_at  TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX idx_task_status ON kb_index_task (status, created_at);
CREATE INDEX idx_task_doc_id ON kb_index_task (doc_id);

-- ================================================================
-- 6. 对话会话表
-- ================================================================
CREATE TABLE kb_chat_session
(
    id             VARCHAR(36) PRIMARY KEY, -- UUID
    user_id        BIGINT    NOT NULL,
    kb_ids         TEXT      NOT NULL,      -- JSON 数组，查询的知识库列表
    title          VARCHAR(200),            -- 会话标题（取第一条消息）
    message_count  INT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted     BOOLEAN   NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_session_user ON kb_chat_session (user_id, last_active_at DESC) WHERE is_deleted = FALSE;

-- ================================================================
-- 7. 对话消息表
-- ================================================================
CREATE TABLE kb_chat_message
(
    id BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(36) NOT NULL,
    role        VARCHAR(20) NOT NULL,           -- USER / ASSISTANT
    content     TEXT        NOT NULL,
    sources JSONB,                              -- 引用来源列表（仅 ASSISTANT 消息有）
    token_count INT                  DEFAULT 0, -- 消耗的 Token 数
    latency_ms  INT                  DEFAULT 0, -- 生成耗时（毫秒）
    feedback    SMALLINT,                       -- 用户反馈：1=好 -1=差 NULL=未反馈
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN kb_chat_message.sources IS
    'JSON格式：[{"docId":1,"docName":"手册.pdf","chunkId":100,"pageNum":5,"excerpt":"...","score":0.92}]';

CREATE INDEX idx_message_session ON kb_chat_message (session_id, created_at);

-- ================================================================
-- 8. 用户反馈表（点赞/点踩，用于效果评估）
-- ================================================================
CREATE TABLE kb_answer_feedback
(
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    feedback   SMALLINT  NOT NULL, -- 1=有用 -1=无用
    comment    TEXT,               -- 可选的文字反馈
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (message_id, user_id)
);

-- ================================================================
-- 9. RAG 评估数据集表
-- ================================================================
CREATE TABLE kb_eval_dataset
(
    id BIGSERIAL PRIMARY KEY,
    kb_id              BIGINT    NOT NULL,
    question           TEXT      NOT NULL,
    expected_answer    TEXT,
    expected_chunk_ids BIGINT[], -- 期望召回的 chunk ID
    created_by         BIGINT    NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE kb_eval_result
(
    id BIGSERIAL PRIMARY KEY,
    dataset_id       BIGINT      NOT NULL,
    eval_version     VARCHAR(50) NOT NULL, -- 评估版本（如：v1_chunk512_hybrid）
    hit              BOOLEAN     NOT NULL, -- 是否命中（期望 chunk 在召回结果中）
    rank             INT,                  -- 命中 chunk 的排名（用于 MRR 计算）
    actual_answer    TEXT,
    faithfulness     FLOAT,                -- RAGAS Faithfulness 分数
    answer_relevancy FLOAT,                -- RAGAS Answer Relevancy 分数
    eval_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);