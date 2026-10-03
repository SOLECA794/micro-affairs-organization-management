import { useCallback, useEffect, useState } from 'react';
import { Card, Button, Space, Input, Select, App, Modal, Form, Tag, Typography } from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import { pageAssociations, createAssociation, updateAssociation, pageUsers } from '../../api/admin';

/** 社团管理（管理端）：列表、创建（绑定负责人）、维护（含状态） */
export default function AdminAssociations() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState(undefined);
  const [editOpen, setEditOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [managers, setManagers] = useState([]);

  const fetchList = useCallback(async (p, s, kw, st) => {
    setLoading(true);
    try {
      const data = await pageAssociations({ page: p, size: s, keyword: kw, status: st });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, '', undefined);
  }, [fetchList]);

  const loadManagers = async () => {
    // 负责人候选：角色为 MANAGER 的正常账号
    const data = await pageUsers({ page: 1, size: 100, role: 'MANAGER', status: 1 });
    setManagers(data.list || []);
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    loadManagers();
    setEditOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    loadManagers();
    form.setFieldsValue({
      name: row.name,
      code: row.code,
      category: row.category,
      leaderUserId: row.leaderUserId,
      status: row.status,
      description: row.description,
    });
    setEditOpen(true);
  };

  const onSave = async (values) => {
    setSaving(true);
    try {
      if (editing) {
        await updateAssociation(editing.id, {
          name: values.name,
          category: values.category,
          leaderUserId: values.leaderUserId,
          status: values.status,
          description: values.description,
        });
        message.success('社团已更新');
      } else {
        await createAssociation(values);
        message.success('社团已创建');
      }
      setEditOpen(false);
      fetchList(page, size, keyword, status);
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '社团名称', dataIndex: 'name' },
    { title: '编号', dataIndex: 'code' },
    { title: '分类', dataIndex: 'category', render: (v) => v || '-' },
    { title: '负责人', dataIndex: 'leaderName' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (v) => (v === 1 ? <Tag color="green">正常</Tag> : <Tag color="red">停用</Tag>),
    },
    { title: '创建时间', dataIndex: 'createdAt' },
    { title: '操作', render: (_, row) => <a onClick={() => openEdit(row)}>维护</a> },
  ];

  return (
    <Card
      className="page-card"
      title="社团管理"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>创建社团</Button>}
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          placeholder="社团名称/编号关键词"
          prefix={<SearchOutlined />}
          style={{ width: 220 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 120 }}
          value={status}
          onChange={setStatus}
          options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]}
        />
        <Button type="primary" onClick={() => { setPage(1); fetchList(1, size, keyword, status); }}>查询</Button>
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
          fetchList(p, s, keyword, status);
        }}
      />

      <Modal
        title={editing ? `维护社团：${editing.name}` : '创建社团'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSave}>
          <Form.Item name="name" label="社团名称" rules={[{ required: true, message: '请输入社团名称' }, { max: 100, message: '最长 100 字' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="社团编号"
            rules={[{ required: !editing, message: '请输入社团编号' }, { max: 30, message: '最长 30 字' }]}
            extra={editing ? '编号创建后不可修改' : undefined}
          >
            <Input disabled={!!editing} placeholder="如 ASSOC-001" />
          </Form.Item>
          <Form.Item name="category" label="社团分类" extra="文本分类，如：文体 / 学术">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="leaderUserId" label="负责人" rules={[{ required: true, message: '请选择负责人' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={managers.map((m) => ({ value: m.id, label: `${m.realName}（${m.username}）` }))}
              placeholder="先在用户管理中创建负责人账号"
            />
          </Form.Item>
          {editing && (
            <Form.Item name="status" label="社团状态" rules={[{ required: true, message: '请选择状态' }]}>
              <Select options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
            </Form.Item>
          )}
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
          <Typography.Paragraph type="secondary">
            提示：一个负责人只能绑定一个社团；停用社团后，其活动不可新发布。
          </Typography.Paragraph>
        </Form>
      </Modal>
    </Card>
  );
}
