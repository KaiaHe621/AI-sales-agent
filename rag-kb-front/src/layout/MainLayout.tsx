import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Button } from 'antd';
import {
  MessageOutlined,
  DatabaseOutlined,
  BarChartOutlined,
  DashboardOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/useAuthStore';
import { authApi } from '@/api';

const { Sider, Content, Header } = Layout;

const menuItems = [
  {
    key: '/chat',
    icon: <MessageOutlined />,
    label: '智能问答',
  },
  {
    key: '/kb',
    icon: <DatabaseOutlined />,
    label: '知识库管理',
  },
  {
    key: '/eval',
    icon: <BarChartOutlined />,
    label: '效果评估',
  },
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '监控面板',
  },
];

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    }
    logout();
    navigate('/login');
  };

  const selectedKey = '/' + location.pathname.split('/')[1];

  return (
    <Layout className="h-screen">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={240}
        style={{ background: '#ffffff', borderRight: '1px solid #e5e7eb' }}
      >
        <div className="h-16 flex items-center justify-center border-b border-gray-200 px-4">
          <ThunderboltOutlined
            className="text-2xl"
            style={{ color: '#4f46e5' }}
          />
          {!collapsed && (
            <span className="ml-3 text-lg font-bold gradient-text whitespace-nowrap">
              鸡翅RAG
            </span>
          )}
        </div>

        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          className="border-none mt-2"
          style={{ background: 'transparent' }}
        />

        <div className="absolute bottom-4 left-0 right-0 px-4">
          {!collapsed && (
            <div className="glass-card p-3 text-center">
              <div className="text-xs text-gray-500">鸡翅RAG 知识库系统</div>
              <div className="text-xs text-primary-600 mt-1">v1.0.0</div>
            </div>
          )}
        </div>
      </Sider>

      <Layout>
        <Header
          className="flex items-center justify-between px-6 border-b border-gray-200"
          style={{ background: '#ffffff', height: 64, lineHeight: '64px' }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="!text-gray-500 hover:!text-gray-800"
          />

          <Dropdown
            menu={{
              items: [
                {
                  key: 'info',
                  label: (
                    <div className="px-2 py-1">
                      <div className="font-medium text-gray-800">{user?.username}</div>
                      <div className="text-xs text-gray-500">
                        {user?.departmentId} · {user?.role}
                      </div>
                    </div>
                  ),
                  disabled: true,
                },
                { type: 'divider' },
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: handleLogout,
                  danger: true,
                },
              ],
            }}
            placement="bottomRight"
          >
            <div className="flex items-center cursor-pointer gap-2 hover:opacity-80 transition-opacity">
              <Avatar
                size={36}
                icon={<UserOutlined />}
                style={{ backgroundColor: '#4f46e5' }}
              />
              {user && (
                <span className="text-sm text-gray-700 hidden sm:inline">
                  {user.username}
                </span>
              )}
            </div>
          </Dropdown>
        </Header>

        <Content className="overflow-auto p-6" style={{ background: '#f8fafc' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
