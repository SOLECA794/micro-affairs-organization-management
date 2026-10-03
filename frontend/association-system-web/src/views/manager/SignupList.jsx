import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Button, Space, App, Descriptions } from 'antd';
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import StatusTag from '../../components/StatusTag';
import { pageSignupList, exportSignups, getManagerActivityDetail } from '../../api/manager';
import { SIGNUP_STATUS } from '../../utils/format';

const STATUS_OPTIONS = Object.entries(SIGNUP_STATUS).map(([value, label]) => ({ value, label }));

/** 报名名单（社团端）：按状态筛选、分页、导出 Excel */
export default function ManagerSignupList() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [activity, setActivity] = useState(null);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [status, setStatus] = useState(undefined);

  useEffect(() => {
    getManagerActivityDetail(id).then(setActivity);
  }, [id]);

  const fetchList = useCallback(async (p, s, st) => {
    setLoading(true);
    try {
      const data = await pageSignupList(id, { page: p, size: s, status: st });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchList(1, 10, undefined);
  }, [fetchList]);

  const columns = [
    { title: '姓名', dataIndex: 'realName' },
    { title: '手机号', dataIndex: 'phone', render: (v) => v || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (v, r) => (
        <Space>
          <StatusTag status={v} type="signup" />
          {v === 'WAITING' && r.queueOrder ? <Space>序号 {r.queueOrder}</Space> : null}
        </Space>
      ),
    },
    { title: '报名时间', dataIndex: 'signupTime' },
    { title: '是否已签到', dataIndex: 'signed', render: (v) => (v ? '是' : '否') },
  ];

  return (
    <Card
      className="page-card"
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/manager/activities')} />
          <span>报名名单{activity ? ` · ${activity.title}` : ''}</span>
        </Space>
      }
      extra={
        <Button
          icon={<DownloadOutlined />}
          onClick={async () => {
            await exportSignups(id, `报名名单-${activity?.title || id}.xlsx`);
            message.success('导出成功');
          }}
        >
          导出 Excel
        </Button>
      }
    >
      <Descriptions
        size="small"
        column={{ xs: 1, sm: 3 }}
        style={{ marginBottom: 12 }}
        items={[
          { key: 'cap', label: '报名/名额', children: activity ? `${activity.enrolledCount}/${activity.capacity}` : '-' },
          { key: 'time', label: '活动时间', children: activity ? `${activity.startTime} ~ ${activity.endTime}` : '-' },
          { key: 'deadline', label: '报名截止', children: activity?.signupDeadline || '-' },
        ]}
      />

      <Space style={{ marginBottom: 12 }} wrap>
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
    </Card>
  );
}
