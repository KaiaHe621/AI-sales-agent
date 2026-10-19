import { useState, useEffect, useCallback } from 'react';
import { Button, Spin, App } from 'antd';
import {
  ReloadOutlined, ThunderboltOutlined, CloudOutlined,
  CodeOutlined, DollarOutlined, PieChartOutlined,
} from '@ant-design/icons';
import { statsApi } from '@/api';
import type { TokenStats } from '@/types';

export default function DashboardPage() {
  const [stats, setStats] = useState<TokenStats | null>(null);
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();

  const fetchStats = useCallback(async () => {
    setLoading(true);
    try {
      const res = await statsApi.getTokenStats();
      if (res.data.code === 200) setStats(res.data.data);
    } catch (err: any) {
      message.error(err.response?.data?.message || '获取统计数据失败');
    }
    setLoading(false);
  }, [message]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const statCards = stats
    ? [
        {
          title: 'Embedding Tokens',
          value: stats.embeddingTokens,
          icon: <CloudOutlined />,
          color: 'from-cyan-400 to-cyan-600',
          iconColor: '#06b6d4',
          bgColor: 'from-cyan-500/10 to-cyan-600/5',
          description: '向量化消耗',
        },
        {
          title: 'Context Tokens',
          value: stats.contextTokens,
          icon: <CodeOutlined />,
          color: 'from-primary-400 to-primary-600',
          iconColor: '#6366f1',
          bgColor: 'from-primary-500/10 to-primary-600/5',
          description: '上下文消耗',
        },
        {
          title: 'Generation Tokens',
          value: stats.generationTokens,
          icon: <ThunderboltOutlined />,
          color: 'from-purple-400 to-purple-600',
          iconColor: '#a855f7',
          bgColor: 'from-purple-500/10 to-purple-600/5',
          description: '生成消耗',
        },
        {
          title: '总 Tokens',
          value: stats.totalTokens,
          icon: <PieChartOutlined />,
          color: 'from-amber-400 to-orange-600',
          iconColor: '#f59e0b',
          bgColor: 'from-amber-500/10 to-orange-600/5',
          description: '累计消耗',
        },
      ]
    : [];

  const formatNumber = (n: number) => {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(2) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toString();
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold gradient-text">监控面板</h1>
          <p className="text-gray-500 text-sm mt-1">Token 用量与成本监控</p>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={fetchStats}
          loading={loading}
          className="!border-gray-200"
        >
          刷新
        </Button>
      </div>

      {loading && !stats ? (
        <div className="flex items-center justify-center h-64">
          <Spin size="large" />
        </div>
      ) : stats ? (
        <>
          {/* Cost Card - Hero */}
          <div className="glass-card neon-border p-8 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-radial from-primary-500/10 to-transparent rounded-full -translate-y-1/2 translate-x-1/2" />
            <div className="relative">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-purple-600 flex items-center justify-center">
                  <DollarOutlined className="text-2xl text-white" />
                </div>
                <div>
                  <div className="text-gray-500 text-sm">预估总成本</div>
                  <div className="text-xs text-gray-500">基于当前 Token 用量估算</div>
                </div>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-5xl font-bold gradient-text">
                  ¥{stats.estimatedCostCny.toFixed(2)}
                </span>
                <span className="text-gray-500 text-sm">CNY</span>
              </div>
              <div className="mt-4 flex gap-6 text-sm">
                <div>
                  <span className="text-gray-500">Token 总量：</span>
                  <span className="text-primary-600 font-medium">
                    {formatNumber(stats.totalTokens)}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500">平均单价：</span>
                  <span className="text-primary-600 font-medium">
                    ¥{stats.totalTokens > 0
                      ? ((stats.estimatedCostCny / stats.totalTokens) * 1000).toFixed(4)
                      : '0'} / 1K tokens
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Token Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {statCards.map((card) => (
              <div key={card.title} className="glass-card stat-card p-6 group hover:scale-[1.02] transition-transform">
                <div className="flex items-center justify-between mb-4">
                  <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${card.bgColor} flex items-center justify-center`}>
                    <span className="text-lg" style={{ color: card.iconColor }}>
                      {card.icon}
                    </span>
                  </div>
                  <span className="text-xs text-gray-500">{card.description}</span>
                </div>
                <div className={`text-3xl font-bold bg-gradient-to-r ${card.color} bg-clip-text text-transparent`}>
                  {formatNumber(card.value)}
                </div>
                <div className="text-xs text-gray-500 mt-2">
                  {card.value.toLocaleString()} tokens
                </div>
                {/* Percentage bar */}
                {stats.totalTokens > 0 && (
                  <div className="mt-3">
                    <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className={`h-full bg-gradient-to-r ${card.color} rounded-full transition-all duration-1000`}
                        style={{ width: `${(card.value / stats.totalTokens) * 100}%` }}
                      />
                    </div>
                    <div className="text-right text-xs text-gray-500 mt-1">
                      {((card.value / stats.totalTokens) * 100).toFixed(1)}%
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Distribution */}
          <div className="glass-card p-6">
            <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
              <PieChartOutlined className="text-primary-400" style={{ color: '#4f46e5' }} />
              Token 分布
            </h3>
            <div className="space-y-4">
              {[
                { label: 'Embedding', value: stats.embeddingTokens, color: '#06b6d4' },
                { label: 'Context', value: stats.contextTokens, color: '#6366f1' },
                { label: 'Generation', value: stats.generationTokens, color: '#a855f7' },
              ].map((item) => (
                <div key={item.label} className="flex items-center gap-4">
                  <div className="w-24 text-sm text-gray-500">{item.label}</div>
                  <div className="flex-1 h-8 bg-gray-100 rounded-lg overflow-hidden relative">
                    <div
                      className="h-full rounded-lg transition-all duration-1000 flex items-center px-3"
                      style={{
                        width: `${stats.totalTokens > 0 ? (item.value / stats.totalTokens) * 100 : 0}%`,
                        background: `linear-gradient(90deg, ${item.color}33, ${item.color}66)`,
                        minWidth: item.value > 0 ? '60px' : '0',
                      }}
                    >
                      <span className="text-xs font-medium whitespace-nowrap">
                        {formatNumber(item.value)}
                      </span>
                    </div>
                  </div>
                  <div className="w-16 text-right text-sm text-gray-500">
                    {stats.totalTokens > 0
                      ? ((item.value / stats.totalTokens) * 100).toFixed(1) + '%'
                      : '0%'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
