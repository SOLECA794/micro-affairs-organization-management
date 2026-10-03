import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Space, Button, Tag, Typography } from 'antd';
import PaginatedTable from '../../components/PaginatedTable';
import StatusTag from '../../components/StatusTag';
import { pageMySignups } from '../../api/activity';
import { signupStatusText, SIGNUP_STATUS } from '../../utils/format';

const STATUS_OPTIONS = Object.entries(SIGNUP_STATUS).map(([value, label]) => ({ value, label }));

/** 我的报名（学生端）：全部报名记录，含候补与取消 */
export default function MySignups() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [status, setStatus] = useState(undefined);

  const fetchList = useCallback(async (p, s, st) => {
    setLoading(true);
    try {
      const data = await pageMySignups({ page: p, size: s, status: st });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, undefined);
  }, [fetchList]);

  const columns = [
    { title: '活动', dataIndex: 'activityTitle', render: (v, r) => <a onClick={() => navigate(`/activities/${r.activityId}`)}>{v}</a> },
    { title: '社团', dataIndex: 'associationName' },
    { title: '地点', dataIndex: 'location' },
    { title: '开始时间', dataIndex: 'startTime' },
    {
      title: '报名状态',
      dataIndex: 'status',
      render: (v, r) => (
        <Space>
          <StatusTag status={v} type="signup" />
          {v === 'WAITING' && r.queueOrder ? <Tag>序号 {r.queueOrder}</Tag> : null}
        </Space>
      ),
    },
    { title: '报名时间', dataIndex: 'signupTime' },
  ];

  return (
    <Card
      className="page-card"
      title="我的报名"
      extra={
        <Space>
          {STATUS_OPTIONS.map((o) => (
            <Button
              key={o.value}
              size="small"
              type={status === o.value ? 'primary' : 'default'}
              onClick={() => {
                const next = status === o.value ? undefined : o.value;
                setStatus(next);
                setPage(1);
                fetchList(1, size, next);
              }}
            >
              {o.label}
            </Button>
          ))}
        </Space>
      }
    >
      <PaginatedTable
        rowKey="signupId"
        columns={columns}
        dataSource={list}
        total={total}
        page={page}
        size={size}
        loading={loading}
        onPageChange={(p, s) => {
          setPage(p);
          setSize(s);
          fetchList(p, s, status);
        }}
      />
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        提示：报名截止时间前可取消报名；候补中用户按顺序自动递补。
      </Typography.Paragraph>
    </Card>
  );
}
