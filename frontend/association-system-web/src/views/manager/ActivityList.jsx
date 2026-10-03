import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Space, Input, Select, App, Popconfirm, Typography } from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import StatusTag from '../../components/StatusTag';
import {
  pageManagerActivities,
  submitAudit,
  publishActivity,
  cancelActivity,
  archiveActivity,
} from '../../api/manager';
import { ACTIVITY_STATUS } from '../../utils/format';

const STATUS_OPTIONS = Object.entries(ACTIVITY_STATUS).map(([value, label]) => ({ value, label }));

/** 活动管理列表（社团端）：状态流转操作入口 */
export default function ManagerActivityList() {
  const navigate = useNavigate();
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState(undefined);

  const fetchList = useCallback(async (p, s, kw, st) => {
    setLoading(true);
    try {
      const data = await pageManagerActivities({ page: p, size: s, keyword: kw, status: st });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, '', undefined);
  }, [fetchList]);

  const doAction = async (action, row, tip) => {
    try {
      await action(row.id);
      message.success(tip);
      fetchList(page, size, keyword, status);
    } catch {
      // 统一错误提示
    }
  };

  const confirmCancel = (row) => {
    modal.confirm({
      title: '取消活动',
      content: `确定取消活动《${row.title}》吗？已报名学生将收到通知。`,
      onOk: () => doAction(cancelActivity, row, '活动已取消'),
    });
  };

  const columns = [
    { title: '活动标题', dataIndex: 'title', width: 200 },
    { title: '状态', dataIndex: 'status', width: 100, render: (v) => <StatusTag status={v} /> },
    { title: '地点', dataIndex: 'location', width: 140 },
    { title: '开始时间', dataIndex: 'startTime', width: 170 },
    { title: '报名截止', dataIndex: 'signupDeadline', width: 170 },
    { title: '报名/名额', width: 100, render: (_, r) => `${r.enrolledCount}/${r.capacity}` },
    {
      title: '操作',
      fixed: 'right',
      width: 300,
      render: (_, row) => (
        <Space size={[4, 4]} wrap>
          <a onClick={() => navigate(`/manager/activities/${row.id}/edit`)}>编辑</a>
          {(row.status === 'DRAFT' || row.status === 'REJECTED') && (
            <Popconfirm title="提交审核后不可修改，确认提交？" onConfirm={() => doAction(submitAudit, row, '已提交审核')}>
              <a>提交审核</a>
            </Popconfirm>
          )}
          {row.status === 'APPROVED' && (
            <a onClick={() => doAction(publishActivity, row, '已发布，学生端可见')}>发布</a>
          )}
          {row.status === 'PUBLISHED' && <a onClick={() => confirmCancel(row)}>取消活动</a>}
          {(row.status === 'ENDED' || row.status === 'CANCELLED') && (
            <Popconfirm title="归档后仅可查看统计，确认归档？" onConfirm={() => doAction(archiveActivity, row, '活动已归档')}>
              <a>归档</a>
            </Popconfirm>
          )}
          <a onClick={() => navigate(`/manager/activities/${row.id}/signups`)}>名单</a>
          <a onClick={() => navigate(`/manager/activities/${row.id}/signin`)}>签到</a>
          <a onClick={() => navigate(`/manager/activities/${row.id}/stats`)}>统计</a>
        </Space>
      ),
    },
  ];

  return (
    <Card
      className="page-card"
      title="活动管理"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/manager/activities/new/edit')}>
          创建活动
        </Button>
      }
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          placeholder="标题关键词"
          prefix={<SearchOutlined />}
          style={{ width: 200 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={() => { setPage(1); fetchList(1, size, keyword, status); }}
        />
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 130 }}
          options={STATUS_OPTIONS}
          value={status}
          onChange={(v) => { setStatus(v); setPage(1); fetchList(1, size, keyword, v); }}
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
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        状态流转：草稿 → 待审核 → 审核通过 → 已发布 → 已结束 → 已归档；驳回后可修改重新提交。
      </Typography.Paragraph>
    </Card>
  );
}
