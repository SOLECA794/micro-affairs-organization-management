import { useCallback, useEffect, useState } from 'react';
import { Card, Button, Space, Input, InputNumber, App, Modal, Form, Table } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { pageCategories, createCategory, updateCategory, deleteCategory } from '../../api/admin';

/** 分类管理（管理端）：全量列表（不分页）、增改删与排序 */
export default function AdminCategories() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [editOpen, setEditOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const data = await pageCategories({});
      setList(data || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setEditOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    form.setFieldsValue({ name: row.name, sort: row.sort });
    setEditOpen(true);
  };

  const onSave = async (values) => {
    setSaving(true);
    try {
      if (editing) {
        await updateCategory(editing.id, values);
        message.success('分类已更新');
      } else {
        await createCategory(values);
        message.success('分类已创建');
      }
      setEditOpen(false);
      fetchList();
    } finally {
      setSaving(false);
    }
  };

  const onDelete = (row) => {
    modal.confirm({
      title: '删除分类',
      content: `确认删除分类「${row.name}」吗？被活动引用的分类无法删除。`,
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteCategory(row.id);
        message.success('分类已删除');
        fetchList();
      },
    });
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '分类名', dataIndex: 'name' },
    { title: '排序', dataIndex: 'sort' },
    {
      title: '操作',
      render: (_, row) => (
        <Space>
          <a onClick={() => openEdit(row)}>修改</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => onDelete(row)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card
      className="page-card"
      title="活动分类管理"
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchList}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增分类</Button>
        </Space>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={false}
        locale={{ emptyText: '暂无分类' }}
        size="middle"
      />

      <Modal
        title={editing ? '修改分类' : '新增分类'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSave} initialValues={{ sort: 0 }}>
          <Form.Item name="name" label="分类名" rules={[{ required: true, message: '请输入分类名' }, { max: 50, message: '最长 50 字' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="排序（越小越靠前）">
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
