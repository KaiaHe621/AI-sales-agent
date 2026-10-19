import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button, Modal, Form, Input, Switch, Select, Table, Tag, Space, App, Popconfirm,
} from 'antd';
import {
  PlusOutlined, FolderOpenOutlined, FileTextOutlined,
  TeamOutlined, GlobalOutlined, LockOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { kbApi } from '@/api';
import type { KnowledgeBase, KnowledgeBaseCreateRequest } from '@/types';

const DEPT_OPTIONS = [
  { label: 'HR 部门', value: 'HR' },
  { label: '技术部门', value: 'TECH' },
  { label: '产品部门', value: 'PROD' },
  { label: '全公司', value: 'ALL' },
];

export default function KnowledgeBasePage() {
  const [kbList, setKbList] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<KnowledgeBaseCreateRequest>();
  const navigate = useNavigate();
  const { message } = App.useApp();

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await kbApi.list();
      if (res.data.code === 200) setKbList(res.data.data);
    } catch {
      message.error('获取知识库列表失败');
    }
    setLoading(false);
  }, [message]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const handleCreate = async (values: KnowledgeBaseCreateRequest) => {
    setCreating(true);
    try {
      const res = await kbApi.create(values);
      if (res.data.code === 200) {
        message.success('知识库创建成功');
        setCreateModalOpen(false);
        form.resetFields();
        fetchList();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || '创建失败');
    }
    setCreating(false);
  };

  const columns = [
    {
      title: '知识库',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: KnowledgeBase) => (
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-100 to-purple-100 flex items-center justify-center border border-primary-200">
            <FolderOpenOutlined className="text-primary-400" style={{ color: '#4f46e5' }} />
          </div>
          <div>
            <div className="font-medium text-gray-800">{name}</div>
            <div className="text-xs text-gray-500 mt-0.5 max-w-xs truncate">
              {record.description}
            </div>
          </div>
        </div>
      ),
    },
    {
      title: '部门',
      dataIndex: 'departmentId',
      key: 'departmentId',
      width: 120,
      render: (dept: string) => (
        <Tag icon={<TeamOutlined />} className="!border-gray-200 !bg-gray-50">
          {dept}
        </Tag>
      ),
    },
    {
      title: '可见性',
      dataIndex: 'isPublic',
      key: 'isPublic',
      width: 100,
      render: (isPublic: boolean) =>
        isPublic ? (
          <Tag icon={<GlobalOutlined />} color="green">公开</Tag>
        ) : (
          <Tag icon={<LockOutlined />} color="orange">私有</Tag>
        ),
    },
    {
      title: '我的权限',
      dataIndex: 'permission',
      key: 'permission',
      width: 110,
      render: (perm: string) => {
        const config: Record<string, { color: string; label: string }> = {
          ADMIN: { color: 'purple', label: '管理员' },
          WRITE: { color: 'blue', label: '可读写' },
          READ:  { color: 'default', label: '只读' },
        };
        const c = config[perm] || config.READ;
        return <Tag color={c.color}>{c.label}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (t: string) => (
        <span className="text-gray-500 text-sm">
          {dayjs(t).format('YYYY-MM-DD HH:mm')}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_: unknown, record: KnowledgeBase) => (
        <Button
          type="link"
          icon={<FileTextOutlined />}
          onClick={async () => {
            try {
              await kbApi.getDocuments(record.id);
              navigate(`/kb/${record.id}/documents`);
            } catch {
              // 拦截器已弹窗提示，不跳转
            }
          }}
          className="!text-primary-400 hover:!text-primary-600"
        >
          管理文档
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold gradient-text">知识库管理</h1>
          <p className="text-gray-500 text-sm mt-1">创建和管理您的企业知识库</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
          className="!bg-gradient-to-r !from-primary-600 !to-primary-500 !border-none h-10 px-6"
        >
          新建知识库
        </Button>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { label: '知识库总数', value: kbList.length, color: 'from-primary-500 to-primary-600' },
          { label: '公开知识库', value: kbList.filter((k) => k.isPublic).length, color: 'from-green-500 to-emerald-600' },
          { label: '私有知识库', value: kbList.filter((k) => !k.isPublic).length, color: 'from-orange-500 to-amber-600' },
        ].map((stat) => (
          <div key={stat.label} className="glass-card stat-card p-5">
            <div className="text-gray-500 text-sm">{stat.label}</div>
            <div className={`text-3xl font-bold mt-2 bg-gradient-to-r ${stat.color} bg-clip-text text-transparent`}>
              {stat.value}
            </div>
          </div>
        ))}
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        <Table
          dataSource={kbList}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={false}
          className="[&_.ant-table]:!bg-transparent"
        />
      </div>

      {/* Create Modal */}
      <Modal
        title={<span className="gradient-text font-bold text-lg">新建知识库</span>}
        open={createModalOpen}
        onCancel={() => { setCreateModalOpen(false); form.resetFields(); }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreate}
          initialValues={{ isPublic: false }}
          className="mt-4"
        >
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="如：技术文档库" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
            rules={[{ required: true, message: '请输入描述' }]}
          >
            <Input.TextArea rows={3} placeholder="简要描述知识库用途" />
          </Form.Item>

          <Form.Item
            name="departmentId"
            label="所属部门"
            rules={[{ required: true, message: '请选择部门' }]}
          >
            <Select options={DEPT_OPTIONS} placeholder="选择部门" />
          </Form.Item>

          <Form.Item name="isPublic" label="是否公开" valuePropName="checked">
            <Switch checkedChildren="公开" unCheckedChildren="私有" />
          </Form.Item>

          <Form.Item className="mb-0 text-right">
            <Space>
              <Button onClick={() => { setCreateModalOpen(false); form.resetFields(); }}>
                取消
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={creating}
                className="!bg-gradient-to-r !from-primary-600 !to-primary-500 !border-none"
              >
                创建
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
