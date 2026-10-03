import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Row, Col, Statistic, Table, Typography, Button, Descriptions, Modal, Form, Input, App, Tag } from 'antd';
import {
  CalendarOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  FileDoneOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { getManagerOverview, getMyAssociation, updateMyAssociation } from '../../api/manager';
import StatusTag from '../../components/StatusTag';
import { ACTIVITY_STATUS_COLOR, activityStatusText } from '../../utils/format';

/** 工作台（社团端首页）：统计概览 + 本社团资料维护 + 最近活动 */
export default function ManagerDashboard() {
  const navigate = useNavigate();
  const [overview, setOverview] = useState(null);
  const [association, setAssociation] = useState(null);
  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const { message } = App.useApp();

  const fetchData = async () => {
    const [ov, assoc] = await Promise.all([getManagerOverview(), getMyAssociation()]);
    setOverview(ov);
    setAssociation(assoc);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const openEdit = () => {
    form.setFieldsValue({
      name: association?.name,
      category: association?.category,
      description: association?.description,
    });
    setEditOpen(true);
  };

  const onSave = async (values) => {
    setSaving(true);
    try {
      const data = await updateMyAssociation(values);
      setAssociation(data);
      setEditOpen(false);
      message.success('社团资料已更新');
    } finally {
      setSaving(false);
    }
  };

  const recentColumns = [
    { title: '活动', dataIndex: 'title' },
    { title: '状态', dataIndex: 'status', render: (v) => <StatusTag status={v} /> },
    { title: '报名/名额', render: (_, r) => `${r.enrolledCount}/${r.capacity}` },
    { title: '开始时间', dataIndex: 'startTime' },
    {
      title: '操作',
      render: (_, r) => <a onClick={() => navigate(`/manager/activities/${r.id}/signups`)}>名单</a>,
    },
  ];

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col xs={12} sm={12} md={6}>
          <Card><Statistic title="活动总数" value={overview?.activityTotal ?? '-'} prefix={<CalendarOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card><Statistic title="进行中（报名中）" value={overview?.publishedTotal ?? '-'} prefix={<CheckCircleOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card><Statistic title="报名总人次" value={overview?.signupTotal ?? '-'} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card><Statistic title="签到总人次" value={overview?.attendanceTotal ?? '-'} prefix={<FileDoneOutlined />} /></Card>
        </Col>
      </Row>

      <Card
        style={{ marginTop: 16 }}
        title="本社团资料"
        extra={<Button icon={<EditOutlined />} size="small" onClick={openEdit}>维护资料</Button>}
      >
        <Descriptions
          column={{ xs: 1, sm: 2 }}
          items={[
            { key: 'name', label: '社团名称', children: association?.name || '-' },
            { key: 'code', label: '社团编号', children: association?.code || '-' },
            { key: 'category', label: '社团分类', children: association?.category || '-' },
            {
              key: 'status',
              label: '状态',
              children: association?.status === 1 ? <Tag color="green">正常</Tag> : <Tag color="red">停用</Tag>,
            },
            { key: 'desc', label: '简介', children: association?.description || '-', span: 2 },
          ]}
        />
      </Card>

      <Card style={{ marginTop: 16 }} title="最近活动" extra={<Button type="link" onClick={() => navigate('/manager/activities')}>全部活动</Button>}>
        <Table
          rowKey="id"
          size="small"
          columns={recentColumns}
          dataSource={overview?.recentActivities || []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: '暂无活动' }}
        />
      </Card>

      <Modal
        title="维护本社团资料"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSave}>
          <Form.Item name="name" label="社团名称" rules={[{ required: true, message: '请输入社团名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="category" label="社团分类">
            <Input maxLength={50} placeholder="如：文体 / 学术" />
          </Form.Item>
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
