import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Button, Upload, Table, Tag, Space, Progress, App, Popconfirm, Tooltip,
} from 'antd';
import {
  UploadOutlined, ArrowLeftOutlined, ReloadOutlined,
  DeleteOutlined, DownloadOutlined, FileTextOutlined, FilePdfOutlined,
  FileMarkdownOutlined, FileUnknownOutlined, CheckCircleOutlined,
  ClockCircleOutlined, SyncOutlined, CloseCircleOutlined,
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import dayjs from 'dayjs';
import { kbApi } from '@/api';
import type { KbDocument } from '@/types';

const FILE_ICONS: Record<string, React.ReactNode> = {
  PDF: <FilePdfOutlined className="text-red-400" />,
  TXT: <FileTextOutlined className="text-blue-400" />,
  MARKDOWN: <FileMarkdownOutlined className="text-green-400" />,
  MD: <FileMarkdownOutlined className="text-green-400" />,
  DOCX: <FileTextOutlined className="text-primary-400" />,
};

const STATUS_MAP: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
  PENDING: { color: 'default', icon: <ClockCircleOutlined />, text: '待处理' },
  PROCESSING: { color: 'processing', icon: <SyncOutlined spin />, text: '索引中' },
  DONE: { color: 'success', icon: <CheckCircleOutlined />, text: '已完成' },
  FAILED: { color: 'error', icon: <CloseCircleOutlined />, text: '失败' },
};

export default function KbDocumentsPage() {
  const { kbId } = useParams<{ kbId: string }>();
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<KbDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const { message } = App.useApp();
  const pollingRef = useRef<ReturnType<typeof setInterval>>();

  const numKbId = Number(kbId);

  const fetchDocuments = useCallback(async () => {
    if (!kbId) return;
    setLoading(true);
    try {
      const res = await kbApi.getDocuments(numKbId);
      if (res.data.code === 200) setDocuments(res.data.data);
    } catch (err: any) {
      if (err.response?.status === 403) {
        navigate('/kb');
        return;
      }
    }
    setLoading(false);
  }, [kbId, numKbId, message]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  useEffect(() => {
    const hasPending = documents.some((d) =>
      d.status === 'PENDING' || d.status === 'PROCESSING',
    );
    if (hasPending) {
      pollingRef.current = setInterval(fetchDocuments, 5000);
    }
    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [documents, fetchDocuments]);

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: true,
    showUploadList: false,
    accept: '.pdf,.txt,.md,.docx',
    customRequest: async ({ file, onSuccess, onError }) => {
      setUploading(true);
      try {
        const res = await kbApi.uploadDocument(numKbId, file as File);
        if (res.data.code === 200) {
          message.success(`${(file as File).name} 上传成功，开始索引...`);
          onSuccess?.(res.data);
          fetchDocuments();
        }
      } catch (err: any) {
        message.error(err.response?.data?.message || '上传失败');
        onError?.(err);
      }
      setUploading(false);
    },
  };

  const handleDelete = async (docId: number) => {
    try {
      await kbApi.deleteDocument(numKbId, docId);
      message.success('文档已删除');
      fetchDocuments();
    } catch (err: any) {
      message.error(err.response?.data?.message || '删除失败');
    }
  };

  const handleDownload = async (record: KbDocument) => {
    try {
      const res = await kbApi.downloadDocument(numKbId, record.id);
      const blob = new Blob([res.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = record.fileName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: any) {
      message.error(err.response?.data?.message || '下载失败');
    }
  };

  const handleReindex = async (docId: number) => {
    try {
      await kbApi.reindexDocument(numKbId, docId);
      message.success('已提交重建索引任务');
      fetchDocuments();
    } catch (err: any) {
      message.error(err.response?.data?.message || '重建失败');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const columns = [
    {
      title: '文件',
      dataIndex: 'fileName',
      key: 'fileName',
      render: (name: string, record: KbDocument) => (
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center border border-gray-200 text-lg">
            {FILE_ICONS[record.fileType] || <FileUnknownOutlined />}
          </div>
          <div>
            <div className="font-medium text-gray-800 text-sm">{name}</div>
            <div className="text-xs text-gray-500 mt-0.5">
              {formatFileSize(record.fileSize)} · v{record.version}
            </div>
          </div>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: string, record: KbDocument) => {
        const st = STATUS_MAP[status] || STATUS_MAP.PENDING;
        return (
          <Tooltip title={record.errorMsg || undefined}>
            <Tag icon={st.icon} color={st.color}>{st.text}</Tag>
          </Tooltip>
        );
      },
    },
    {
      title: 'Chunks',
      dataIndex: 'chunkCount',
      key: 'chunkCount',
      width: 100,
      render: (count: number) => (
        <span className="text-gray-500">{count || '-'}</span>
      ),
    },
    {
      title: 'Tokens',
      dataIndex: 'tokenCount',
      key: 'tokenCount',
      width: 120,
      render: (count: number) => (
        <span className="text-gray-500">{count ? count.toLocaleString() : '-'}</span>
      ),
    },
    {
      title: '上传时间',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      width: 160,
      render: (t: string) => (
        <span className="text-gray-500 text-sm">
          {dayjs(t).format('MM-DD HH:mm')}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: unknown, record: KbDocument) => (
        <Space>
          <Tooltip title="下载原文">
            <Button
              type="text"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
              className="!text-gray-500 hover:!text-primary-400"
            />
          </Tooltip>
          <Tooltip title="重建索引">
            <Button
              type="text"
              size="small"
              icon={<ReloadOutlined />}
              onClick={() => handleReindex(record.id)}
              disabled={record.status === 'PROCESSING'}
              className="!text-gray-500 hover:!text-primary-400"
            />
          </Tooltip>
          <Popconfirm
            title="确定删除该文档？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button
              type="text"
              size="small"
              icon={<DeleteOutlined />}
              danger
              className="hover:!text-red-400"
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/kb')}
            className="!border-gray-200"
          >
            返回
          </Button>
          <div>
            <h1 className="text-2xl font-bold gradient-text">文档管理</h1>
            <p className="text-gray-500 text-sm mt-0.5">知识库 #{kbId}</p>
          </div>
        </div>

        <Upload {...uploadProps}>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            loading={uploading}
            className="!bg-gradient-to-r !from-primary-600 !to-primary-500 !border-none h-10 px-6"
          >
            上传文档
          </Button>
        </Upload>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[
          { label: '文档总数', value: documents.length },
          { label: '已完成', value: documents.filter((d) => d.status === 'DONE').length },
          { label: '处理中', value: documents.filter((d) => d.status === 'PROCESSING' || d.status === 'PENDING').length },
          { label: '总 Chunks', value: documents.reduce((s, d) => s + (d.chunkCount || 0), 0) },
        ].map((stat) => (
          <div key={stat.label} className="glass-card stat-card p-4">
            <div className="text-gray-500 text-xs">{stat.label}</div>
            <div className="text-2xl font-bold text-primary-400 mt-1" style={{ color: '#4f46e5' }}>
              {stat.value}
            </div>
          </div>
        ))}
      </div>

      <div className="glass-card overflow-hidden">
        <Table
          dataSource={documents}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={false}
          className="[&_.ant-table]:!bg-transparent"
        />
      </div>
    </div>
  );
}
