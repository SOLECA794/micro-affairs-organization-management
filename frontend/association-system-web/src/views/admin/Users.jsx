import { useCallback, useEffect, useState } from 'react';
import { Card, Space, Input, Select, Button, Switch, App, Typography } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import { pageUsers, updateUserStatus } from '../../api/admin';

const ROLE_OPTIONS = [
  { value: 'STUDENT', label: '学生' },
  { value: 'MANAGER', label: '负责人' },
  { value: 'ADMIN', label: '管理员' },
];

const ROLE_NAME = { STUDENT: '学生', MANAGER: '负责人', ADMIN: '管理员' };

/** 用户管理（管理端）：列表、筛选、启用/停用 */
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
      title: '启用/停用',
      render: (_, row) => <Switch checked={row.status === 1} checkedChildren="启用" unCheckedChildren="停用" onChange={() => toggleStatus(row)} />,
    },
  ];

  return (
    <Card className="page-card" title="用户管理">
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
    </Card>
  );
}
