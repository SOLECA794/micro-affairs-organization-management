import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Card, Descriptions, Button, Space, Tag, Typography, App, Result, Spin, Grid } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { getActivityDetail, signupActivity, cancelSignup, scanSignin } from '../../api/activity';
import { activityStatusText, signupStatusText, ACTIVITY_STATUS_COLOR, SIGNUP_STATUS_COLOR } from '../../utils/format';

const { useBreakpoint } = Grid;

/** 活动详情（学生端）：报名 / 取消 / 扫码签到承接 */
export default function ActivityDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { message, modal } = App.useApp();
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getActivityDetail(id);
      setDetail(data);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  // 扫码落地：URL 携带 signinToken 时自动签到
  useEffect(() => {
    const signinToken = searchParams.get('signinToken');
    if (!signinToken) return;
    setSearchParams({}, { replace: true });
    modal.confirm({
      title: '扫码签到',
      content: '检测到签到码，确认在本活动完成签到？',
      onOk: async () => {
        try {
          await scanSignin({ activityId: Number(id), token: signinToken });
          message.success('签到成功');
          fetchDetail();
        } catch {
          // 错误提示由封装统一处理
        }
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleSignup = async () => {
    setSubmitting(true);
    try {
      const result = await signupActivity(id);
      if (result.result === 'WAITING') {
        message.info(`名额已满，已进入候补，当前序号第 ${result.queueOrder} 位`);
      } else {
        message.success('报名成功');
      }
      fetchDetail();
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    modal.confirm({
      title: '取消报名',
      content: '确定取消本活动的报名吗？',
      onOk: async () => {
        setSubmitting(true);
        try {
          await cancelSignup(id);
          message.success('已取消报名');
          fetchDetail();
        } finally {
          setSubmitting(false);
        }
      },
    });
  };

  if (loading && !detail) {
    return <Card className="page-card"><Spin style={{ display: 'block', margin: '80px auto' }} /></Card>;
  }
  if (!detail) {
    return (
      <Card className="page-card">
        <Result status="warning" title="活动不存在" extra={<Button onClick={() => navigate('/activities')}>返回列表</Button>} />
      </Card>
    );
  }

  const remaining = detail.remainingCount;

  return (
    <Card
      className="page-card"
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/activities')} />
          <span>{detail.title}</span>
          <Tag color={ACTIVITY_STATUS_COLOR[detail.status] || 'default'}>{activityStatusText(detail.status)}</Tag>
        </Space>
      }
    >
      <Descriptions
        column={isMobile ? 1 : 2}
        bordered
        size="middle"
        items={[
          { key: 'assoc', label: '主办社团', children: detail.associationName || '-' },
          { key: 'category', label: '活动分类', children: detail.categoryName || '-' },
          { key: 'location', label: '活动地点', children: detail.location },
          { key: 'capacity', label: '名额', children: `${detail.enrolledCount}/${detail.capacity}${remaining != null ? `（剩余 ${remaining}）` : ''}` },
          { key: 'start', label: '开始时间', children: detail.startTime },
          { key: 'end', label: '结束时间', children: detail.endTime },
          { key: 'deadline', label: '报名截止', children: detail.signupDeadline },
          {
            key: 'mysignup',
            label: '我的报名状态',
            children: detail.mySignupStatus
              ? <Tag color={SIGNUP_STATUS_COLOR[detail.mySignupStatus] || 'default'}>{signupStatusText(detail.mySignupStatus)}{detail.myQueueOrder ? `（候补序号 ${detail.myQueueOrder}）` : ''}</Tag>
              : <Typography.Text type="secondary">未报名</Typography.Text>,
          },
        ]}
      />

      <Typography.Paragraph style={{ marginTop: 24, whiteSpace: 'pre-wrap' }}>
        {detail.description || '（暂无活动简介）'}
      </Typography.Paragraph>

      {detail.status === 'REJECTED' && detail.auditComment && (
        <Typography.Paragraph type="danger">驳回意见：{detail.auditComment}</Typography.Paragraph>
      )}

      <Space style={{ marginTop: 16 }}>
        {detail.canSignup && (
          <Button type="primary" loading={submitting} onClick={handleSignup}>
            立即报名
          </Button>
        )}
        {detail.canCancel && (
          <Button danger loading={submitting} onClick={handleCancel}>
            取消报名
          </Button>
        )}
      </Space>
    </Card>
  );
}
