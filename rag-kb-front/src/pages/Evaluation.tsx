import { useState, useEffect, useCallback } from 'react';
import {
  Button, Select, Input, Table, Tag, Space, App, Empty, Tabs,
  Modal, Form, Popconfirm, Tooltip,
} from 'antd';
import {
  PlayCircleOutlined, HistoryOutlined, RocketOutlined,
  AimOutlined, ThunderboltOutlined, SafetyCertificateOutlined,
  PlusOutlined, EditOutlined, DeleteOutlined, DatabaseOutlined,
  TagsOutlined, QuestionCircleOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { kbApi, evalApi } from '@/api';
import type { KnowledgeBase, EvalReport, EvalDataset, ChunkSummary } from '@/types';

export default function EvalPage() {
  const [kbList, setKbList] = useState<KnowledgeBase[]>([]);
  const [selectedKbId, setSelectedKbId] = useState<number | null>(null);
  const [version, setVersion] = useState('');
  const [running, setRunning] = useState(false);
  const [history, setHistory] = useState<EvalReport[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [latestReport, setLatestReport] = useState<EvalReport | null>(null);

  const [dataset, setDataset] = useState<EvalDataset[]>([]);
  const [datasetLoading, setDatasetLoading] = useState(false);
  const [chunks, setChunks] = useState<ChunkSummary[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<EvalDataset | null>(null);
  const [chunkModalOpen, setChunkModalOpen] = useState(false);
  const [chunkTarget, setChunkTarget] = useState<EvalDataset | null>(null);
  const [selectedChunkIds, setSelectedChunkIds] = useState<number[]>([]);

  const [form] = Form.useForm();
  const { message } = App.useApp();

  useEffect(() => {
    kbApi.list().then((res) => {
      if (res.data.code === 200) setKbList(res.data.data);
    }).catch(() => {});
  }, []);

  const fetchHistory = useCallback(async (kbId: number) => {
    setHistoryLoading(true);
    try {
      const res = await evalApi.getHistory(kbId);
      if (res.data.code === 200) {
        const list = res.data.data ?? [];
        setHistory(list);
        setLatestReport(list.length > 0 ? list[0] : null);
      }
    } catch {
      message.error('获取评估历史失败');
    }
    setHistoryLoading(false);
  }, [message]);

  const fetchDataset = useCallback(async (kbId: number) => {
    setDatasetLoading(true);
    try {
      const res = await evalApi.listDataset(kbId);
      if (res.data.code === 200) setDataset(res.data.data ?? []);
    } catch {
      message.error('获取评估数据集失败');
    }
    setDatasetLoading(false);
  }, [message]);

  const fetchChunks = useCallback(async (kbId: number) => {
    try {
      const res = await evalApi.listChunks(kbId);
      if (res.data.code === 200) setChunks(res.data.data ?? []);
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    if (selectedKbId) {
      fetchHistory(selectedKbId);
      fetchDataset(selectedKbId);
      fetchChunks(selectedKbId);
    } else {
      setHistory([]);
      setLatestReport(null);
      setDataset([]);
      setChunks([]);
    }
  }, [selectedKbId, fetchHistory, fetchDataset, fetchChunks]);

  const handleRun = async () => {
    if (!selectedKbId) {
      message.warning('请先选择知识库');
      return;
    }
    setRunning(true);
    try {
      const res = await evalApi.runEvaluation(selectedKbId, version);
      if (res.data.code === 200) {
        message.success('评估完成');
        setLatestReport(res.data.data);
        fetchHistory(selectedKbId);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || '评估失败');
    }
    setRunning(false);
  };

  // =============== Dataset CRUD ===============

  const openAddModal = () => {
    setEditingItem(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEditModal = (item: EvalDataset) => {
    setEditingItem(item);
    form.setFieldsValue({
      question: item.question,
      expectedAnswer: item.expectedAnswer || '',
    });
    setModalOpen(true);
  };

  const handleSaveQuestion = async () => {
    if (!selectedKbId) return;
    try {
      const values = await form.validateFields();
      if (editingItem) {
        await evalApi.updateQuestion(selectedKbId, editingItem.id, {
          question: values.question,
          expectedAnswer: values.expectedAnswer || undefined,
        });
        message.success('已更新');
      } else {
        await evalApi.addQuestion(selectedKbId, {
          question: values.question,
          expectedAnswer: values.expectedAnswer || undefined,
        });
        message.success('已添加');
      }
      setModalOpen(false);
      fetchDataset(selectedKbId);
    } catch {
      /* validation error */
    }
  };

  const handleDeleteQuestion = async (id: number) => {
    if (!selectedKbId) return;
    try {
      await evalApi.deleteQuestion(selectedKbId, id);
      message.success('已删除');
      fetchDataset(selectedKbId);
    } catch (err: any) {
      message.error(err.response?.data?.message || '删除失败');
    }
  };

  // =============== Chunk 标注 ===============

  const openChunkModal = (item: EvalDataset) => {
    setChunkTarget(item);
    setSelectedChunkIds(item.expectedChunkIds ? [...item.expectedChunkIds] : []);
    setChunkModalOpen(true);
  };

  const handleSaveChunks = async () => {
    if (!selectedKbId || !chunkTarget) return;
    try {
      await evalApi.updateQuestion(selectedKbId, chunkTarget.id, {
        expectedChunkIds: selectedChunkIds,
      });
      message.success('标注已保存');
      setChunkModalOpen(false);
      fetchDataset(selectedKbId);
    } catch (err: any) {
      message.error(err.response?.data?.message || '保存失败');
    }
  };

  // =============== Table columns ===============

  const historyColumns = [
    {
      title: '版本',
      dataIndex: 'evalVersion',
      key: 'evalVersion',
      render: (v: string) => (
        <Tag className="!border-primary-200 !bg-primary-50 !text-primary-600">{v}</Tag>
      ),
    },
    { title: '问题数', dataIndex: 'totalQuestions', key: 'totalQuestions', width: 100 },
    { title: '命中数', dataIndex: 'hitCount', key: 'hitCount', width: 100 },
    {
      title: <span>Hit Rate <Tooltip title="命中率：检索结果中包含正确 Chunk 的问题占比"><QuestionCircleOutlined className="text-gray-400" /></Tooltip></span>,
      dataIndex: 'hitRate', key: 'hitRate', width: 130,
      render: (v: number) => (
        <span className={v > 0.7 ? 'text-green-500' : v > 0.4 ? 'text-yellow-500' : 'text-red-500'}>
          {(v * 100).toFixed(1)}%
        </span>
      ),
    },
    {
      title: <span>MRR <Tooltip title="平均倒数排名：正确 Chunk 排名倒数的均值，越接近 1 越好"><QuestionCircleOutlined className="text-gray-400" /></Tooltip></span>,
      dataIndex: 'mrr', key: 'mrr', width: 130,
      render: (v: number) => (
        <span className={v > 0.7 ? 'text-green-500' : v > 0.4 ? 'text-yellow-500' : 'text-red-500'}>
          {v.toFixed(4)}
        </span>
      ),
    },
    {
      title: <span>忠实性 <Tooltip title="生成回答对检索上下文的忠实程度，越接近 1 说明幻觉越少"><QuestionCircleOutlined className="text-gray-400" /></Tooltip></span>,
      dataIndex: 'avgFaithfulness', key: 'avgFaithfulness', width: 130,
      render: (v: number) => (
        <span className={v > 0.8 ? 'text-green-500' : v > 0.5 ? 'text-yellow-500' : 'text-red-500'}>
          {v.toFixed(4)}
        </span>
      ),
    },
    {
      title: '评估时间', dataIndex: 'evalAt', key: 'evalAt', width: 180,
      render: (t: string) => (
        <span className="text-gray-500 text-sm">{dayjs(t).format('YYYY-MM-DD HH:mm')}</span>
      ),
    },
  ];

  const datasetColumns = [
    {
      title: 'ID', dataIndex: 'id', key: 'id', width: 60,
    },
    {
      title: '问题', dataIndex: 'question', key: 'question',
      width: 250, ellipsis: true,
      render: (q: string) => <span className="text-gray-800">{q}</span>,
    },
    {
      title: '期望答案', dataIndex: 'expectedAnswer', key: 'expectedAnswer',
      width: 200, ellipsis: true,
      render: (v: string | null) => v || <span className="text-gray-400">未设置</span>,
    },
    {
      title: '期望 Chunk IDs', dataIndex: 'expectedChunkIds', key: 'expectedChunkIds',
      width: 180,
      render: (ids: number[] | null, record: EvalDataset) => (
        <Space>
          {ids && ids.length > 0 ? (
            ids.map((id) => <Tag key={id} color="blue">#{id}</Tag>)
          ) : (
            <span className="text-red-400 text-xs">未标注</span>
          )}
          <Tooltip title="标注关联 Chunk">
            <Button
              type="text"
              size="small"
              icon={<TagsOutlined />}
              onClick={() => openChunkModal(record)}
              className="!text-primary-500"
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 140,
      render: (t: string) => (
        <span className="text-gray-500 text-xs">{dayjs(t).format('MM-DD HH:mm')}</span>
      ),
    },
    {
      title: '操作', key: 'actions', width: 120,
      render: (_: unknown, record: EvalDataset) => (
        <Space>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />}
              onClick={() => openEditModal(record)}
              className="!text-gray-500 hover:!text-primary-500"
            />
          </Tooltip>
          <Popconfirm title="确定删除？" onConfirm={() => handleDeleteQuestion(record.id)}>
            <Button type="text" size="small" icon={<DeleteOutlined />}
              danger className="hover:!text-red-400"
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const chunkColumns = [
    {
      title: '',
      key: 'select',
      width: 50,
      render: (_: unknown, record: ChunkSummary) => {
        const checked = selectedChunkIds.includes(record.id);
        return (
          <input
            type="checkbox"
            checked={checked}
            onChange={() => {
              setSelectedChunkIds((prev) =>
                checked ? prev.filter((id) => id !== record.id) : [...prev, record.id],
              );
            }}
            className="w-4 h-4 accent-indigo-500"
          />
        );
      },
    },
    { title: 'Chunk ID', dataIndex: 'id', key: 'id', width: 90 },
    { title: 'Doc ID', dataIndex: 'docId', key: 'docId', width: 80 },
    { title: '序号', dataIndex: 'chunkIndex', key: 'chunkIndex', width: 70 },
    {
      title: '内容摘要', dataIndex: 'content', key: 'content',
      ellipsis: true,
      render: (c: string) => <span className="text-gray-600 text-xs">{c}</span>,
    },
    { title: 'Tokens', dataIndex: 'tokenCount', key: 'tokenCount', width: 80 },
  ];

  // =============== KB selector (shared) ===============

  const kbSelector = (
    <div className="flex-1 min-w-[200px]">
      <label className="text-sm text-gray-500 block mb-1.5">选择知识库</label>
      <Select
        value={selectedKbId}
        onChange={setSelectedKbId}
        placeholder="选择知识库"
        className="w-full"
        options={kbList.map((kb) => ({ label: kb.name, value: kb.id }))}
      />
    </div>
  );

  // =============== Render ===============

  const tabItems = [
    {
      key: 'run',
      label: <span><RocketOutlined /> 运行评估</span>,
      children: (
        <div className="space-y-6">
          <div className="glass-card p-6">
            <div className="flex gap-4 flex-wrap items-end">
              {kbSelector}
              <div className="flex-1 min-w-[200px]">
                <label className="text-sm text-gray-500 block mb-1.5">评估版本标识</label>
                <Input
                  value={version}
                  onChange={(e) => setVersion(e.target.value)}
                  placeholder="v1_hybrid_reranker"
                />
              </div>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleRun}
                loading={running}
                className="!bg-gradient-to-r !from-primary-600 !to-primary-500 !border-none h-10 px-8"
              >
                {running ? '评估中...' : '开始评估'}
              </Button>
            </div>
          </div>

          {latestReport && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="glass-card stat-card p-5">
                <div className="flex items-center gap-2 text-gray-500 text-sm mb-2">
                  <AimOutlined /> Hit Rate
                  <Tooltip title="命中率：检索结果中包含正确 Chunk 的问题占比。值越高说明检索召回能力越强。">
                    <QuestionCircleOutlined className="text-gray-400 cursor-help" />
                  </Tooltip>
                </div>
                <div className="text-3xl font-bold text-green-500">
                  {(latestReport.hitRate * 100).toFixed(1)}%
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  {latestReport.hitCount}/{latestReport.totalQuestions} 命中
                </div>
              </div>
              <div className="glass-card stat-card p-5">
                <div className="flex items-center gap-2 text-gray-500 text-sm mb-2">
                  <ThunderboltOutlined /> MRR
                  <Tooltip title="Mean Reciprocal Rank（平均倒数排名）：正确 Chunk 在检索结果中排名的倒数的平均值。排名越靠前，MRR 越接近 1。">
                    <QuestionCircleOutlined className="text-gray-400 cursor-help" />
                  </Tooltip>
                </div>
                <div className="text-3xl font-bold text-primary-400" style={{ color: '#4f46e5' }}>
                  {latestReport.mrr.toFixed(4)}
                </div>
                <div className="text-xs text-gray-500 mt-1">平均倒数排名</div>
              </div>
              <div className="glass-card stat-card p-5">
                <div className="flex items-center gap-2 text-gray-500 text-sm mb-2">
                  <SafetyCertificateOutlined /> 忠实性
                  <Tooltip title="Faithfulness（忠实度）：生成的回答是否忠实于检索到的上下文，不编造信息。值越接近 1 说明幻觉越少。">
                    <QuestionCircleOutlined className="text-gray-400 cursor-help" />
                  </Tooltip>
                </div>
                <div className="text-3xl font-bold text-cyan-500">
                  {latestReport.avgFaithfulness.toFixed(4)}
                </div>
                <div className="text-xs text-gray-500 mt-1">平均忠实性分数</div>
              </div>
              <div className="glass-card stat-card p-5">
                <div className="flex items-center gap-2 text-gray-500 text-sm mb-2">
                  <HistoryOutlined /> 版本
                </div>
                <div className="text-lg font-bold text-gray-800 mt-2">
                  {latestReport.evalVersion}
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  {dayjs(latestReport.evalAt).format('YYYY-MM-DD HH:mm')}
                </div>
              </div>
            </div>
          )}

          <div className="glass-card overflow-hidden">
            <div className="p-4 border-b border-gray-200 flex items-center gap-2">
              <HistoryOutlined className="text-primary-400" style={{ color: '#4f46e5' }} />
              <span className="font-semibold">评估历史</span>
            </div>
            <Table
              dataSource={history}
              columns={historyColumns}
              rowKey={(r) => `${r.evalVersion}-${r.evalAt}`}
              loading={historyLoading}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无评估记录" /> }}
              className="[&_.ant-table]:!bg-transparent"
            />
          </div>
        </div>
      ),
    },
    {
      key: 'dataset',
      label: <span><DatabaseOutlined /> 评估数据集</span>,
      children: (
        <div className="space-y-4">
          <div className="glass-card p-6">
            <div className="flex gap-4 flex-wrap items-end justify-between">
              {kbSelector}
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={openAddModal}
                disabled={!selectedKbId}
                className="!bg-gradient-to-r !from-primary-600 !to-primary-500 !border-none h-10 px-6"
              >
                添加评估问题
              </Button>
            </div>
          </div>

          {selectedKbId && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="glass-card stat-card p-4">
                <div className="text-gray-500 text-xs">评估问题总数</div>
                <div className="text-2xl font-bold mt-1" style={{ color: '#4f46e5' }}>
                  {dataset.length}
                </div>
              </div>
              <div className="glass-card stat-card p-4">
                <div className="text-gray-500 text-xs">已标注 Chunk</div>
                <div className="text-2xl font-bold text-green-500 mt-1">
                  {dataset.filter((d) => d.expectedChunkIds && d.expectedChunkIds.length > 0).length}
                </div>
              </div>
              <div className="glass-card stat-card p-4">
                <div className="text-gray-500 text-xs">知识库 Chunk 总数</div>
                <div className="text-2xl font-bold text-gray-600 mt-1">
                  {chunks.length}
                </div>
              </div>
            </div>
          )}

          <div className="glass-card overflow-hidden">
            <Table
              dataSource={dataset}
              columns={datasetColumns}
              rowKey="id"
              loading={datasetLoading}
              pagination={false}
              locale={{ emptyText: <Empty description={selectedKbId ? '暂无评估数据' : '请先选择知识库'} /> }}
              className="[&_.ant-table]:!bg-transparent"
            />
          </div>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold gradient-text">RAG 效果评估</h1>
        <p className="text-gray-500 text-sm mt-1">
          管理评估数据集、运行评估流水线、对比不同版本的检索和生成质量
        </p>
      </div>

      <Tabs items={tabItems} />

      {/* 新增/编辑问题 Modal */}
      <Modal
        title={editingItem ? '编辑评估问题' : '添加评估问题'}
        open={modalOpen}
        onOk={handleSaveQuestion}
        onCancel={() => setModalOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" className="mt-4">
          <Form.Item
            name="question"
            label="评估问题"
            rules={[{ required: true, message: '请输入评估问题' }]}
          >
            <Input.TextArea rows={3} placeholder="例如：年假怎么申请？" />
          </Form.Item>
          <Form.Item name="expectedAnswer" label="期望答案（可选）">
            <Input.TextArea rows={3} placeholder="标准参考答案，用于忠实性评估" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Chunk 标注 Modal */}
      <Modal
        title={
          <div>
            <span>标注关联 Chunk</span>
            {chunkTarget && (
              <div className="text-sm font-normal text-gray-500 mt-1">
                问题：{chunkTarget.question}
              </div>
            )}
          </div>
        }
        open={chunkModalOpen}
        onOk={handleSaveChunks}
        onCancel={() => setChunkModalOpen(false)}
        okText="保存标注"
        cancelText="取消"
        width={800}
      >
        <div className="mb-3 flex items-center gap-2 text-sm text-gray-500">
          <TagsOutlined />
          已选中 <Tag color="blue">{selectedChunkIds.length}</Tag> 个 Chunk
          {selectedChunkIds.length > 0 && (
            <span>（ID: {selectedChunkIds.join(', ')}）</span>
          )}
        </div>
        <Table
          dataSource={chunks}
          columns={chunkColumns}
          rowKey="id"
          size="small"
          pagination={{ pageSize: 10, showSizeChanger: false }}
          scroll={{ y: 400 }}
          className="[&_.ant-table]:!bg-transparent"
        />
      </Modal>
    </div>
  );
}
