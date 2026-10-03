import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, App } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { login } from '../api/auth';
import { useUserStore } from '../store/user';
import { homeOf } from '../router/RequireAuth';

/** 登录页（05 文档 §2） */
export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const setLogin = useUserStore((s) => s.setLogin);
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const data = await login(values);
      setLogin(data.token, data.user);
      message.success(`欢迎，${data.user.realName}`);
      const from = location.state?.from;
      navigate(from && from !== '/' ? from : homeOf(data.user.role), { replace: true });
    } catch {
      // 错误提示由 axios 封装统一处理
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #eef2ff 0%, #e0e7ff 50%, #f5f3ff 100%)',
        padding: 16,
      }}
    >
      <Card style={{ width: '100%', maxWidth: 400, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Typography.Title level={3} style={{ marginBottom: 4 }}>
            社团活动报名与签到系统
          </Typography.Title>
          <Typography.Text type="secondary">统一入口 · 报名 · 签到 · 统计</Typography.Text>
        </div>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: '', password: '' }}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入账号' }]}>
            <Input size="large" prefix={<UserOutlined />} placeholder="账号" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password size="large" prefix={<LockOutlined />} placeholder="密码" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" size="large" htmlType="submit" block loading={loading}>
            登 录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
