import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, App } from 'antd';
import { UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { authApi } from '@/api';
import { useAuthStore } from '@/store/useAuthStore';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { message } = App.useApp();
  const setAuth = useAuthStore((s) => s.setAuth);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const res = await authApi.login(values);
      const token = res.data.data;
      setAuth(
        {
          userId: 0,
          username: values.username,
          departmentId: '',
          role: '',
          token,
        },
        token,
      );
      message.success('登录成功');
      navigate('/chat');
    } catch (err: any) {
      const msg = err.response?.data?.message || '登录失败';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-hero-pattern relative overflow-hidden">
      {/* Background effects */}
      <div className="absolute inset-0">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary-200/40 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-purple-200/40 rounded-full blur-3xl" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary-100/30 rounded-full blur-3xl" />
      </div>

      <div className="relative z-10 w-full max-w-md px-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-xl shadow-primary-100/50 border border-gray-100 p-10">
          {/* Logo */}
          <div className="text-center mb-10">
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-500 to-purple-600 mb-6 shadow-lg shadow-primary-300/30">
              <ThunderboltOutlined className="text-4xl text-white" />
            </div>
            <h1 className="text-3xl font-bold gradient-text mb-2">鸡翅RAG</h1>
            <p className="text-gray-500 text-sm">企业级智能知识库问答系统</p>
          </div>

          <Form
            name="login"
            onFinish={onFinish}
            size="large"
            autoComplete="off"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined className="text-gray-400" />}
                placeholder="用户名"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined className="text-gray-400" />}
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item className="mb-4">
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                className="h-12 text-base font-medium !bg-gradient-to-r !from-primary-600 !to-primary-500 hover:!from-primary-500 hover:!to-primary-400 !border-none shadow-lg shadow-primary-300/30"
              >
                登 录
              </Button>
            </Form.Item>

            <div className="text-center text-xs text-gray-500 space-y-1">
              <p>演示账号：<span className="text-primary-600 font-medium">admin</span> / <span className="text-primary-600 font-medium">demo123</span></p>
              <p>
                <span className="text-primary-600 font-medium">hr001</span> / <span className="text-primary-600 font-medium">tech001</span> (部门用户)
              </p>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
}
