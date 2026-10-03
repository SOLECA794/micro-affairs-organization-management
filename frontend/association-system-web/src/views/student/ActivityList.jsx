import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Select, Button, List, Tag, Space, Typography, Grid, Pagination, Empty, DatePicker } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { pageActivities, getAssociationOptions, getCategoryOptions } from '../../api/activity';
import { activityStatusText, ACTIVITY_STATUS_COLOR } from '../../utils/format';

const { RangePicker } = DatePicker;

const { useBreakpoint } = Grid;

const STATUS_OPTIONS = [
  { value: 'PUBLISHED', label: '报名中' },
  { value: 'ENDED', label: '已结束' },
  { value: 'ARCHIVED', label: '已归档' },
  { value: 'CANCELLED', label: '已取消' },
];

/** 活动列表（学生端首页，05 文档 §2；默认展示报名中；按社团/分类/时间范围筛选） */
export default function ActivityList() {
  const navigate = useNavigate();
  const screens = useBreakpoint();
  const isMobile = !screens.md;
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [assocOptions, setAssocOptions] = useState([]);
  const [categoryOptions, setCategoryOptions] = useState([]);

  const fetchList = async (p, s, filters) => {
    setLoading(true);
    try {
      const params = { ...filters };
      const range = params.timeRange;
      delete params.timeRange;
      if (Array.isArray(range) && range.length === 2 && range[0] && range[1]) {
        params.startTimeBegin = range[0].format('YYYY-MM-DD') + ' 00:00:00';
        params.startTimeEnd = range[1].format('YYYY-MM-DD') + ' 23:59:59';
      }
      const data = await pageActivities({ page: p, size: s, ...params });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList(1, 10, { status: 'PUBLISHED' });
    getAssociationOptions().then(setAssocOptions).catch(() => {});
    getCategoryOptions().then(setCategoryOptions).catch(() => {});
  }, []);

  const onSearch = (values) => {
    setPage(1);
    fetchList(1, size, values);
  };

  const onPageChange = (p, s) => {
    setPage(p);
    setSize(s);
    fetchList(p, s, form.getFieldsValue());
  };

  return (
    <Card
      className="page-card"
      title="活动列表"
      extra={<Typography.Text type="secondary">默认展示报名中的活动</Typography.Text>}
    >
      <Form form={form} layout={isMobile ? 'vertical' : 'inline'} onFinish={onSearch} className="filter-bar" initialValues={{ status: 'PUBLISHED' }}>
        <Form.Item name="keyword" style={{ minWidth: isMobile ? '100%' : 180 }}>
          <Input placeholder="活动标题关键词" prefix={<SearchOutlined />} allowClear />
        </Form.Item>
        <Form.Item name="associationId">
          <Select
            placeholder="社团"
            allowClear
            showSearch
            optionFilterProp="label"
            options={assocOptions.map((o) => ({ value: o.id, label: o.name }))}
            style={{ width: isMobile ? '100%' : 160 }}
          />
        </Form.Item>
        <Form.Item name="categoryId">
          <Select
            placeholder="分类"
            allowClear
            options={categoryOptions.map((o) => ({ value: o.id, label: o.name }))}
            style={{ width: isMobile ? '100%' : 130 }}
          />
        </Form.Item>
        <Form.Item name="timeRange">
          <RangePicker placeholder={['开始日期', '结束日期']} style={{ width: isMobile ? '100%' : 250 }} />
        </Form.Item>
        <Form.Item name="status">
          <Select placeholder="状态" allowClear options={STATUS_OPTIONS} style={{ width: isMobile ? '100%' : 120 }} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                form.resetFields();
                setPage(1);
                fetchList(1, size, {});
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <List
        loading={loading}
        itemLayout="vertical"
        dataSource={list}
        renderItem={(item) => (
          <List.Item
            style={{ cursor: 'pointer' }}
            onClick={() => navigate(`/activities/${item.id}`)}
            actions={
              isMobile
                ? undefined
                : [
                    <span key="cap">{item.enrolledCount}/{item.capacity} 人</span>,
                    <span key="time">{item.startTime}</span>,
                  ]
            }
          >
            <List.Item.Meta
              title={
                <Space wrap>
                  <span>{item.title}</span>
                  <Tag color={ACTIVITY_STATUS_COLOR[item.status] || 'default'}>
                    {activityStatusText(item.status)}
                  </Tag>
                </Space>
              }
              description={
                <Space wrap size={[12, 0]}>
                  <Typography.Text type="secondary">{item.associationName}</Typography.Text>
                  {item.categoryName && <Tag>{item.categoryName}</Tag>}
                  <Typography.Text type="secondary">{item.location}</Typography.Text>
                  <Typography.Text type="secondary">报名截止 {item.signupDeadline}</Typography.Text>
                  {isMobile && (
                    <Typography.Text type="secondary">
                      {item.startTime} · {item.enrolledCount}/{item.capacity} 人
                    </Typography.Text>
                  )}
                </Space>
              }
            />
          </List.Item>
        )}
        locale={{ emptyText: <Empty description="暂无活动" /> }}
      />

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
        <Pagination
          current={page}
          pageSize={size}
          total={total}
          showSizeChanger
          showTotal={(t) => `共 ${t} 条`}
          onChange={onPageChange}
        />
      </div>
    </Card>
  );
}
