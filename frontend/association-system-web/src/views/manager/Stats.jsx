import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Row, Col, Statistic, Button, Space, Progress, Table, App, Result } from 'antd';
import { ArrowLeftOutlined, DownloadOutlined, TeamOutlined, CheckCircleOutlined, UserDeleteOutlined } from '@ant-design/icons';
import { getStatistics, exportSignups, exportAttendance, getAttendanceList, getManagerActivityDetail } from '../../api/manager';
import StatusTag from '../../components/StatusTag';

/** 统计与导出（社团端）：报名/签到/缺席/到场率 + Excel 导出 */
export default function ManagerStats() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [stats, setStats] = useState(null);
  const [activity, setActivity] = useState(null);
  const [attendance, setAttendance] = useState([]);

  useEffect(() => {
    getManagerActivityDetail(id).then(setActivity);
    getStatistics(id).then(setStats);
    getAttendanceList(id).then(setAttendance);
  }, [id]);

  const rate = stats?.attendanceRate != null ? Math.round(stats.attendanceRate * 10) / 10 : null;

  return (
    <Card
      className="page-card"
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/manager/activities')} />
          <span>统计与导出{activity ? ` · ${activity.title}` : ''}</span>
          {activity && <StatusTag status={activity.status} />}
        </Space>
      }
      extra={
        <Space>
          <Button
            icon={<DownloadOutlined />}
            onClick={async () => {
              await exportSignups(id, `报名名单-${activity?.title || id}.xlsx`);
              message.success('报名名单导出成功');
            }}
          >
            导出报名名单
          </Button>
          <Button
            icon={<DownloadOutlined />}
            onClick={async () => {
              await exportAttendance(id, `签到名单-${activity?.title || id}.xlsx`);
              message.success('签到名单导出成功');
            }}
          >
            导出签到名单
          </Button>
        </Space>
      }
    >
      {!stats ? (
        <Result status="404" title="统计不可用" subTitle="活动不存在或无统计数据" extra={<Button onClick={() => navigate('/manager/activities')}>返回</Button>} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={12} md={6}>
              <Card><Statistic title="报名成功人数" value={stats.enrolledCount ?? 0} prefix={<TeamOutlined />} /></Card>
            </Col>
            <Col xs={12} md={6}>
              <Card><Statistic title="签到人数" value={stats.signedCount ?? 0} prefix={<CheckCircleOutlined />} /></Card>
            </Col>
            <Col xs={12} md={6}>
              <Card><Statistic title="缺席人数" value={stats.absentCount ?? 0} prefix={<UserDeleteOutlined />} /></Card>
            </Col>
            <Col xs={12} md={6}>
              <Card>
                <Statistic
                  title="到场率（签到/报名成功）"
                  value={rate == null ? '-' : rate}
                  suffix="%"
                />
                {rate != null && <Progress percent={rate} size="small" style={{ marginTop: 8 }} />}
              </Card>
            </Col>
          </Row>

          <Card type="inner" title="签到名单明细" style={{ marginTop: 16 }}>
            <Table
              rowKey="attendanceId"
              size="small"
              columns={[
                { title: '姓名', dataIndex: 'realName' },
                { title: '手机号', dataIndex: 'phone', render: (v) => v || '-' },
                { title: '方式', dataIndex: 'signType', render: (v) => (v === 'MANUAL' ? '人工补签' : '扫码') },
                { title: '签到时间', dataIndex: 'signTime' },
                { title: '操作人', dataIndex: 'operatorName', render: (v) => v || '-' },
              ]}
              dataSource={attendance}
              pagination={false}
              locale={{ emptyText: '暂无签到记录' }}
              scroll={{ x: 'max-content' }}
            />
          </Card>
        </>
      )}
    </Card>
  );
}
