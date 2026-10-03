import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, Form, Input, Select, InputNumber, DatePicker, Button, Space, App, Spin, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { createActivity, updateActivity, submitAudit, getManagerActivityDetail } from '../../api/manager';
import { pageCategories } from '../../api/admin';

const DATE_FMT = 'YYYY-MM-DD HH:mm:ss';

/** 活动编辑页（社团端）：/manager/activities/new/edit 创建，/:id/edit 修改草稿或被驳回活动 */
export default function ManagerActivityEdit() {
  const { id } = useParams();
  const isNew = id === 'new';
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [categories, setCategories] = useState([]);
  const [activityStatus, setActivityStatus] = useState('DRAFT');
  const [auditComment, setAuditComment] = useState(null);

  useEffect(() => {
    pageCategories({ page: 1, size: 100 }).then((data) => setCategories(data.list || []));
  }, []);

  useEffect(() => {
    if (isNew) return;
    getManagerActivityDetail(id).then((row) => {
      setActivityStatus(row.status);
      setAuditComment(row.auditComment);
      form.setFieldsValue({
        title: row.title,
        categoryId: row.categoryId,
        cover: row.cover,
        description: row.description,
        location: row.location,
        capacity: row.capacity,
        timeRange: [dayjs(row.startTime), dayjs(row.endTime)],
        signupDeadline: dayjs(row.signupDeadline),
      });
      setLoading(false);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleSubmit = async (values, submitAfter) => {
    setSaving(true);
    try {
      const payload = {
        title: values.title,
        categoryId: values.categoryId ?? null,
        cover: values.cover || null,
        description: values.description || null,
        location: values.location,
        startTime: values.timeRange[0].format(DATE_FMT),
        endTime: values.timeRange[1].format(DATE_FMT),
        signupDeadline: values.signupDeadline.format(DATE_FMT),
        capacity: values.capacity,
      };
      let activityId = id;
      if (isNew) {
        activityId = await createActivity(payload);
        message.success('活动已创建（草稿）');
      } else {
        await updateActivity(id, payload);
        message.success('活动已保存');
      }
      if (submitAfter) {
        await submitAudit(activityId);
        message.success('已提交审核');
      }
      navigate('/manager/activities');
    } finally {
      setSaving(false);
    }
  };

  const onFinish = (values) => handleSubmit(values, false);

  const validateDeadline = ({ getFieldValue }) => ({
    validator(_, value) {
      const start = getFieldValue('timeRange')?.[0];
      if (!value || !start) return Promise.resolve();
      if (value.isAfter(start)) {
        return Promise.reject(new Error('报名截止时间不能晚于活动开始时间'));
      }
      return Promise.resolve();
    },
  });

  if (loading) {
    return <Card className="page-card"><Spin style={{ display: 'block', margin: '80px auto' }} /></Card>;
  }

  const editable = isNew || activityStatus === 'DRAFT' || activityStatus === 'REJECTED';

  if (!editable) {
    return (
      <Card className="page-card">
        <Typography.Paragraph>
          当前状态为「{activityStatus}」，仅草稿或被驳回的活动可修改。
        </Typography.Paragraph>
        <Button onClick={() => navigate('/manager/activities')}>返回列表</Button>
      </Card>
    );
  }

  return (
    <Card
      className="page-card"
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/manager/activities')} />
          <span>{isNew ? '创建活动' : '编辑活动'}</span>
        </Space>
      }
    >
      {!isNew && auditComment && <Typography.Paragraph type="danger">驳回意见：{auditComment}</Typography.Paragraph>}
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        style={{ maxWidth: 640 }}
        initialValues={{ capacity: 50 }}
      >
        <Form.Item name="title" label="活动标题" rules={[{ required: true, message: '请输入活动标题' }, { max: 100, message: '最长 100 字' }]}>
          <Input placeholder="如：社团招新见面会" />
        </Form.Item>
        <Form.Item name="categoryId" label="活动分类">
          <Select allowClear placeholder="选择分类" options={categories.map((c) => ({ value: c.id, label: c.name }))} />
        </Form.Item>
        <Form.Item name="location" label="活动地点" rules={[{ required: true, message: '请输入活动地点' }, { max: 200, message: '最长 200 字' }]}>
          <Input />
        </Form.Item>
        <Form.Item
          name="timeRange"
          label="活动起止时间"
          rules={[{ required: true, message: '请选择起止时间' }]}
        >
          <DatePicker.RangePicker
            showTime
            format={DATE_FMT}
            style={{ width: '100%' }}
            disabledDate={() => false}
          />
        </Form.Item>
        <Form.Item
          name="signupDeadline"
          label="报名截止时间"
          rules={[
            { required: true, message: '请选择报名截止时间' },
            validateDeadline,
          ]}
        >
          <DatePicker showTime format={DATE_FMT} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="capacity"
          label="名额上限"
          rules={[{ required: true, message: '请输入名额' }]}
        >
          <InputNumber min={1} precision={0} style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="cover" label="封面地址（可选）">
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="description" label="活动简介">
          <Input.TextArea rows={4} />
        </Form.Item>
        <Space>
          <Button type="primary" htmlType="submit" loading={saving}>
            保存{isNew ? '草稿' : '修改'}
          </Button>
          <Button
            loading={saving}
            onClick={async () => {
              const values = await form.validateFields();
              await handleSubmit(values, true);
            }}
          >
            保存并提交审核
          </Button>
        </Space>
      </Form>
    </Card>
  );
}
