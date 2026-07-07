-- ============================================================
-- jc-sales-agent 建表 DDL
-- 对应文档：数据库设计.md
-- 注意：所有表使用 IF NOT EXISTS，支持服务重启多次执行
-- ============================================================

CREATE TABLE IF NOT EXISTS sa_sales_region (
                                               id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '大区ID',
                                               name             VARCHAR(50)  NOT NULL COMMENT '大区名称，如：华东区',
                                               parent_region_id BIGINT       DEFAULT NULL COMMENT '上级大区，NULL表示顶级',
                                               created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               PRIMARY KEY (id),
                                               UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售大区';

CREATE TABLE IF NOT EXISTS sa_sales_rep (
                                            id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '销售员ID',
                                            name       VARCHAR(50)  NOT NULL COMMENT '姓名',
                                            region_id  BIGINT       NOT NULL COMMENT '所属大区',
                                            role       VARCHAR(20)  NOT NULL DEFAULT 'SALES_REP'
                                                COMMENT '角色：SALES_REP/SALES_MANAGER/SALES_DIRECTOR',
                                            email      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                                            created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (id),
                                            KEY idx_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售员';

CREATE TABLE IF NOT EXISTS sa_product (
                                          id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '产品ID',
                                          sku_code    VARCHAR(50)    NOT NULL COMMENT 'SKU编码',
                                          name        VARCHAR(200)   NOT NULL COMMENT '产品名称',
                                          category    VARCHAR(50)    NOT NULL COMMENT '品类：数码产品/家用电器/服装配饰/其他',
                                          unit_price  DECIMAL(10,2)  NOT NULL COMMENT '售价',
                                          cost        DECIMAL(10,2)  NOT NULL COMMENT '成本',
                                          status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
                                              COMMENT '状态：ACTIVE/INACTIVE',
                                          created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (id),
                                          UNIQUE KEY uk_sku (sku_code),
                                          KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品';

CREATE TABLE IF NOT EXISTS sa_sales_order (
                                              id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                              order_no      VARCHAR(50)    NOT NULL COMMENT '订单号',
                                              rep_id        BIGINT         NOT NULL COMMENT '销售员ID',
                                              product_id    BIGINT         NOT NULL COMMENT '产品ID',
                                              region_id     BIGINT         NOT NULL COMMENT '销售大区ID',
                                              customer_name VARCHAR(100)   NOT NULL COMMENT '客户名称',
                                              quantity      INT            NOT NULL COMMENT '销售数量',
                                              unit_price    DECIMAL(10,2)  NOT NULL COMMENT '成交单价',
                                              amount        DECIMAL(12,2)  NOT NULL COMMENT '成交金额（quantity * unit_price）',
                                              cost          DECIMAL(12,2)  NOT NULL COMMENT '成本总额',
                                              profit        DECIMAL(12,2)  NOT NULL COMMENT '毛利（amount - cost）',
                                              status        VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED'
                                                  COMMENT '状态：COMPLETED/REFUNDED/CANCELLED',
                                              order_date    DATE           NOT NULL COMMENT '下单日期',
                                              created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              PRIMARY KEY (id),
                                              UNIQUE KEY uk_order_no (order_no),
                                              KEY idx_rep (rep_id),
                                              KEY idx_product (product_id),
                                              KEY idx_region (region_id),
                                              KEY idx_order_date (order_date),
                                              KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

-- ============================================================
-- 对话记忆持久化（对应文档：16-对话记忆接入.md）
-- ============================================================
CREATE TABLE IF NOT EXISTS sa_chat_memory (
                                              id           BIGINT       NOT NULL AUTO_INCREMENT,
                                              session_id   VARCHAR(100) NOT NULL COMMENT '会话 ID',
                                              messages     LONGTEXT     NOT NULL COMMENT '序列化的消息列表（JSON）',
                                              updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                  ON UPDATE CURRENT_TIMESTAMP,
                                              PRIMARY KEY (id),
                                              UNIQUE KEY uk_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记忆持久化';
