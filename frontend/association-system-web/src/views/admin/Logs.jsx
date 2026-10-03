import { useCallback, useEffect, useState } from 'react';
import { Card, Space, Input, Select, DatePicker, Button, Tag, Typography } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import { pageLogs } from '../../api/admin';

const MODULE_OPTIONS = ['auth', 'signup', 'signin', 'activity', 'admin'].map((m) => ({ value: m, label: m }));
const MODULE_COLOR = {
  auth: 'blue',
  signup: 'green',
  signin: 'cyan',
  activity: 'orange',
  admin: 'purple',
};

/** 日志查询（管理端）：按模块、关键词、时间段筛选 */
export default function AdminLogs() {
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [module, setModule] = useState(undefined);
  const [keyword, setKeyword] = useState('');
  const [range, setRange] = useState(null);

  const fetchList = useCallback(async (p, s, m, kw, r) => {
    setLoading(true);
    try {
      const params = { page: p, size: s, module: m, keyword: kw };
      if (r?.[0]) params.startTime = r[0].format('YYYY-MM-DD HH:mm:ss');
      if (r?.[1]) params.endTime = r[1].format('YYYY-MM-DD HH:mm:ss');
      const data = await pageLogs(params);
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, undefined, '', null);
  }, [fetchList]);

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '操作人', dataIndex: 'operatorName', render: (v) => v || '-' },
    {
      title: '模块',
      dataIndex: 'module',
      width: 90,
      render: (v) => <Tag color={MODULE_COLOR[v] || 'default'}>{v}</Tag>,
    },
    { title: '动作', dataIndex: 'action', width: 140 },
    { title: '详情', dataIndex: 'detail', render: (v) => v || '-' },
    { title: 'IP', dataIndex: 'ip', render: (v) => v || '-' },
    { title: '时间', dataIndex: 'createdAt', width: 170 },
  ];

  return (
    <Card className="page-card" title="操作日志查询">
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="模块"
          style={{ width: 120 }}
          options={MODULE_OPTIONS}
          value={module}
          onChange={setModule}
        />
        <Input
          allowClear
          placeholder="动作/详情关键词"
          prefix={<SearchOutlined />}
          style={{ width: 200 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm:ss" value={range} onChange={setRange} />
        <Button type="primary" onClick={() => { setPage(1); fetchList(1, size, module, keyword, range); }}>查询</Button>
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
          fetchList(p, s, module, keyword, range);
        }}
      />
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        覆盖节点：登录、报名、取消报名、递补、开启签到、扫码签到、补签、审核、活动发布/取消/归档。
      </Typography.Paragraph>
    </Card>
  );
}
