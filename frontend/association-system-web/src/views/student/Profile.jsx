import { useEffect, useState } from 'react';
import { Card, Form, Input, Button, App, Typography, Descriptions, Divider } from 'antd';
import { getMe, updateProfile, changePassword } from '../../api/auth';
import { useUserStore } from '../../store/user';

/** 个人中心：资料维护 + 密码修改（修改后需重新登录，02 文档 §5.1） */
export default function Profile() {
  const [form] = Form.useForm();
  const [pwdForm] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [changing, setChanging] = useState(false);
  const { user, setUser, clear } = useUserStore();
  const { message } = App.useApp();

  useEffect(() => {
    getMe().then((data) => {
      setUser(data);
      form.setFieldsValue({ realName: data.realName, phone: data.phone, avatar: data.avatar });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSaveProfile = async (values) => {
    setSaving(true);
    try {
      const data = await updateProfile(values);
      setUser(data);
      message.success('资料已更新');
    } finally {
      setSaving(false);
    }
  };

  const onChangePassword = async (values) => {
    setChanging(true);
    try {
      await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword });
      message.success('密码已修改，请重新登录');
      clear();
      window.location.href = '/login';
    } finally {
      setChanging(false);
    }
  };

  return (
    <Card className="page-card" title="个人中心">
      <Descriptions
        column={1}
        size="small"
        items={[
          { key: 'username', label: '登录账号', children: user?.username },
          { key: 'role', label: '角色', children: user?.role },
        ]}
      />
      <Divider orientation="left">资料维护</Divider>
      <Form form={form} layout="vertical" onFinish={onSaveProfile} style={{ maxWidth: 420 }}>
        <Form.Item name="realName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="phone" label="手机号">
          <Input />
        </Form.Item>
        <Form.Item name="avatar" label="头像地址">
          <Input placeholder="https://..." />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={saving}>保存资料</Button>
      </Form>

      <Divider orientation="left">修改密码</Divider>
      <Form form={pwdForm} layout="vertical" onFinish={onChangePassword} style={{ maxWidth: 420 }}>
        <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            { min: 6, max: 32, message: '长度需为 6-32 位' },
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="confirm"
          label="确认新密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请再次输入新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('两次输入的密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Button danger htmlType="submit" loading={changing}>修改密码</Button>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
          修改成功后需退出并重新登录。
        </Typography.Paragraph>
      </Form>
    </Card>
  );
}
