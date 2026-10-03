import { useCallback, useEffect, useState } from 'react';
import { Card, Space, Input, Select, Button, Switch, App, Typography, Modal, Form } from 'antd';
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import { pageUsers, updateUserStatus, createUser, resetUserPassword } from '../../api/admin';

const ROLE_OPTIONS = [
  { value: 'STUDENT', label: '学生' },
  { value: 'MANAGER', label: '负责人' },
];

const ROLE_NAME = { STUDENT: '学生', MANAGER: '负责人', ADMIN: '管理员' };

/** 展示一次性明文密码：提示复制保存 */
function showOneTimePassword(modal, title, data) {
  modal.success({
    title,
    width: 480,
    okText: '我已保存',
    content: (
      <div>
        <Typography.Paragraph>
          {data.realName}（{data.username}，{ROLE_NAME[data.role] || data.role}）的新密码：
        </Typography.Paragraph>
        <Typography.Paragraph copyable style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>
          {data.initialPassword}
        </Typography.Paragraph>
        <Typography.Text type="danger">该密码仅此一次展示，请立即复制保存，关闭后无法再次查看。</Typography.Text>
      </div>
    ),
  });
}

/** 用户管理（管理端）：列表、筛选、启用/停用、创建用户、重置密码 */
export default function AdminUsers() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState(undefined);
  const [status, setStatus] = useState(undefined);
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const fetchList = useCallback(async (p, s, kw, r, st) => {
    setLoading(true);
    try {
      const data = await pageUsers({ page: p, size: s, keyword: kw, role: r, status: st });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, '', undefined, undefined);
  }, [fetchList]);

  const toggleStatus = (row) => {
    const next = row.status === 1 ? 0 : 1;
    modal.confirm({
      title: next === 0 ? '停用用户' : '启用用户',
      content: next === 0
        ? `停用后 ${row.realName}（${row.username}）将无法登录。确认停用？`
        : `确认启用 ${row.realName}（${row.username}）？`,
      onOk: async () => {
        await updateUserStatus(row.id, { status: next });
        message.success(next === 0 ? '已停用' : '已启用');
        fetchList(page, size, keyword, role, status);
      },
    });
  };

  const reset = () => {
    setKeyword('');
    setRole(undefined);
    setStatus(undefined);
    setPage(1);
    fetchList(1, size, '', undefined, undefined);
  };

  const onCreate = async (values) => {
    setSaving(true);
    try {
      const data = await createUser(values);
      setCreateOpen(false);
      form.resetFields();
      showOneTimePassword(modal, '用户已创建', data);
      fetchList(1, size, keyword, role, status);
    } finally {
      setSaving(false);
    }
  };

  const onResetPassword = (row) => {
    modal.confirm({
      title: '重置密码',
      content: `确认为 ${row.realName}（${row.username}）生成新密码？重置后原密码立即失效。`,
      onOk: async () => {
        const data = await resetUserPassword(row.id, {});
        showOneTimePassword(modal, '密码已重置', data);
      },
    });
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '账号', dataIndex: 'username' },
    { title: '姓名', dataIndex: 'realName' },
    { title: '手机号', dataIndex: 'phone', render: (v) => v || '-' },
    { title: '角色', dataIndex: 'role', render: (v) => ROLE_NAME[v] || v },
    {
      title: '状态',
      dataIndex: 'status',
      render: (v) => (v === 1 ? <Typography.Text type="success">正常</Typography.Text> : <Typography.Text type="danger">停用</Typography.Text>),
    },
    { title: '创建时间', dataIndex: 'createdAt' },
    {
      title: '操作',
      fixed: 'right',
      width: 180,
      render: (_, row) => (
        <Space>
          <Switch checked={row.status === 1} checkedChildren="启用" unCheckedChildren="停用" onChange={() => toggleStatus(row)} />
          {row.role !== 'ADMIN' && <a onClick={() => onResetPassword(row)}>重置密码</a>}
        </Space>
      ),
    },
  ];

  return (
    <Card
      className="page-card"
      title="用户管理"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setCreateOpen(true); }}>新建用户</Button>}
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          placeholder="账号/姓名关键词"
          prefix={<SearchOutlined />}
          style={{ width: 200 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Select allowClear placeholder="角色" style={{ width: 120 }} options={ROLE_OPTIONS} value={role} onChange={setRole} />
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 120 }}
          value={status}
          onChange={setStatus}
          options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]}
        />
        <Button type="primary" onClick={() => { setPage(1); fetchList(1, size, keyword, role, status); }}>查询</Button>
        <Button icon={<ReloadOutlined />} onClick={reset}>重置</Button>
      </Space>

      <PaginatedTable
        rowKey="id"
        columns={columns}
        dataSource={list}
        total={total}
        page={page}
        size={size}
        loading={loading}
        onPageChange={(p, s) => {
          setPage(p);
          setSize(s);
          fetchList(p, s, keyword, role, status);
        }}
      />

      <Modal
        title="新建用户"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onCreate}>
          <Form.Item
            name="username"
            label="登录账号"
            rules={[
              { required: true, message: '请输入登录账号' },
              { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '仅字母、数字、下划线，长度 3-50' },
            ]}
          >
            <Input placeholder="如 stu006" />
          </Form.Item>
          <Form.Item name="realName" label="姓名" rules={[{ required: true, message: '请输入姓名' }, { max: 50, message: '最长 50 字' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select options={ROLE_OPTIONS} placeholder="学生 / 负责人" />
          </Form.Item>
          <Form.Item name="phone" label="手机号（可空）" rules={[{ max: 20, message: '最长 20 字' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label="初始密码（可空，留空由系统生成随机 8 位）"
            rules={[{ min: 8, max: 64, message: '密码长度 8-64 位' }]}
          >
            <Input.Password placeholder="留空自动生成" autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
