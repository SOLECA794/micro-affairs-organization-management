import { useCallback, useEffect, useState } from 'react';
import { Card, Button, Space, Input, Select, App, Modal, Tag, Typography, Descriptions } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import PaginatedTable from '../../components/PaginatedTable';
import StatusTag from '../../components/StatusTag';
import { pageAudits, approveAudit, rejectAudit } from '../../api/admin';
import { formatDateTime } from '../../utils/format';

const STATUS_OPTIONS = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '审核通过' },
  { value: 'REJECTED', label: '已驳回' },
];

/** 活动审核（管理端）：待审核列表、通过、驳回（必填意见）、行内详情弹窗（消除盲审） */
export default function AdminAudits() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [status, setStatus] = useState('PENDING');
  const [keyword, setKeyword] = useState('');
  const [detailRow, setDetailRow] = useState(null);

  const fetchList = useCallback(async (p, s, st, kw) => {
    setLoading(true);
    try {
      const data = await pageAudits({ page: p, size: s, status: st, keyword: kw });
      setList(data.list || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10, 'PENDING', '');
  }, [fetchList]);

  const handleApprove = (row) => {
    modal.confirm({
      title: '审核通过',
      content: `确认通过活动《${row.title}》？通过后由负责人发布。`,
      onOk: async () => {
        await approveAudit(row.id);
        message.success('已通过，负责人将收到通知');
        fetchList(page, size, status, keyword);
      },
    });
  };

  const handleReject = (row) => {
    let comment = '';
    modal.confirm({
      title: '驳回活动',
      content: (
        <div>
          <Typography.Paragraph>驳回活动《{row.title}》，驳回意见必填：</Typography.Paragraph>
          <Input.TextArea
            rows={3}
            maxLength={500}
            placeholder="请填写驳回意见（必填）"
            onChange={(e) => { comment = e.target.value; }}
          />
        </div>
      ),
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!comment.trim()) {
          message.warning('驳回意见不能为空');
          return Promise.reject();
        }
        await rejectAudit(row.id, { comment: comment.trim() });
        message.success('已驳回，负责人将收到通知');
        fetchList(page, size, status, keyword);
      },
    });
  };

  const columns = [
    { title: '活动标题', dataIndex: 'title', width: 180 },
    { title: '社团', dataIndex: 'associationName' },
    { title: '分类', dataIndex: 'categoryName', render: (v) => v || '-' },
    { title: '地点', dataIndex: 'location' },
    { title: '开始时间', dataIndex: 'startTime', width: 160 },
    { title: '名额', dataIndex: 'capacity', width: 70 },
    { title: '状态', dataIndex: 'status', width: 100, render: (v) => <StatusTag status={v} /> },
    {
      title: '操作',
      fixed: 'right',
      width: 160,
      render: (_, row) => (
        <Space>
          {row.status === 'PENDING' && <a onClick={() => handleApprove(row)}>通过</a>}
          {row.status === 'PENDING' && <a style={{ color: '#ff4d4f' }} onClick={() => handleReject(row)}>驳回</a>}
          <a onClick={() => setDetailRow(row)}>详情</a>
        </Space>
      ),
    },
  ];

  return (
    <Card className="page-card" title="活动审核">
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          placeholder="标题关键词"
          prefix={<SearchOutlined />}
          style={{ width: 200 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Select allowClear placeholder="状态" style={{ width: 130 }} options={STATUS_OPTIONS} value={status} onChange={setStatus} />
        <Button type="primary" onClick={() => { setPage(1); fetchList(1, size, status, keyword); }}>查询</Button>
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
          fetchList(p, s, status, keyword);
        }}
      />
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        审核结果将站内通知负责人；驳回必须填写意见。
      </Typography.Paragraph>

      <Modal
        title={`活动详情：${detailRow?.title || ''}`}
        open={!!detailRow}
        footer={null}
        onCancel={() => setDetailRow(null)}
        width={640}
      >
        {detailRow && (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="状态"><StatusTag status={detailRow.status} /></Descriptions.Item>
            <Descriptions.Item label="社团">{detailRow.associationName || '-'}</Descriptions.Item>
            <Descriptions.Item label="分类">{detailRow.categoryName || '-'}</Descriptions.Item>
            <Descriptions.Item label="地点">{detailRow.location || '-'}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{formatDateTime(detailRow.startTime)}</Descriptions.Item>
            <Descriptions.Item label="结束时间">{formatDateTime(detailRow.endTime)}</Descriptions.Item>
            <Descriptions.Item label="报名截止">{formatDateTime(detailRow.signupDeadline)}</Descriptions.Item>
            <Descriptions.Item label="名额">{detailRow.capacity ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="活动简介">
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                {detailRow.description || '（无）'}
              </Typography.Paragraph>
            </Descriptions.Item>
            {detailRow.auditComment && (
              <Descriptions.Item label="审核意见">{detailRow.auditComment}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>
    </Card>
  );
}
