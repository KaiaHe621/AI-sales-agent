-- ================================================================
-- 测试用户（实际项目中从用户服务获取，这里直接插）
-- ================================================================

-- ================================================================
-- 知识库初始化
-- ================================================================
INSERT INTO kb_knowledge_base (name, description, department_id, is_public, created_by)
VALUES
    ('HR知识库',        '公司人事制度、员工手册、入职离职流程等文档',   'HR',   FALSE, 1),
    ('技术知识库',      '技术规范、架构设计文档、开发指南等',           'TECH', FALSE, 2),
    ('产品知识库',      '产品手册、功能说明、FAQ等面向内部的产品文档',   'PROD', TRUE,  3),
    ('公司公共知识库',  '公司介绍、组织架构、通用制度等所有人可查的文档', 'ALL',  TRUE,  1);

-- ================================================================
-- 权限配置
-- ================================================================
-- HR 知识库：HR 部门有写权限，其他部门有读权限
INSERT INTO kb_permission (kb_id, subject_type, subject_id, permission, granted_by)
VALUES
    (1, 'DEPARTMENT', 'HR',   'WRITE', 1),
    (1, 'DEPARTMENT', 'TECH', 'READ',  1),
    (1, 'DEPARTMENT', 'PROD', 'READ',  1);

-- 技术知识库：技术部门有写权限
INSERT INTO kb_permission (kb_id, subject_type, subject_id, permission, granted_by)
VALUES
    (2, 'DEPARTMENT', 'TECH', 'WRITE', 2),
    (2, 'DEPARTMENT', 'PROD', 'READ',  2);

-- ================================================================
-- 评估数据集（用于衡量 RAG 检索质量）
-- ================================================================
INSERT INTO kb_eval_dataset (kb_id, question, expected_answer, expected_chunk_ids, created_by)
VALUES
    (1, '新员工入职第一天需要做什么？',
     '领取工牌和电脑，配置 VPN 和开发环境，与直属 Leader 完成对齐会，阅读代码规范。',
     NULL, 1),
    (1, '年假是怎么规定的？',
     '工作满 1 年未满 10 年享有 5 天年假，工作满 10 年以上享有 10 天年假。',
     NULL, 1),
    (2, 'API 限流策略是什么？',
     '单用户每分钟最多 100 次调用，采用滑动窗口算法，超出后返回 429 状态码。',
     NULL, 2),
    (2, '代码提交规范有哪些？',
     'Commit message 格式为 type(scope): message，type 包括 feat/fix/docs/refactor。',
     NULL, 2),
    (3, '如何申请 API 访问权限？',
     '在开发者控制台创建 API Key，免费配额为每日 1000 次调用，付费套餐按量计费。',
     NULL, 3);

-- expected_chunk_ids 初始为 NULL，文档上传切分后再回填。在后面rag效果评估会讲
-- 查询实际 chunk ID：
--   SELECT id, LEFT(content, 50) FROM kb_doc_chunk WHERE kb_id = 1 ORDER BY id;
-- 然后回填：
--   UPDATE kb_eval_dataset SET expected_chunk_ids = ARRAY[1, 2] WHERE id = 1;
--   UPDATE kb_eval_dataset SET expected_chunk_ids = ARRAY[3]    WHERE id = 2;