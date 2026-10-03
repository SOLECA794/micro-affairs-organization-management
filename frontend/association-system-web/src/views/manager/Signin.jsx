import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Button, Space, App, Typography, Table, Modal, Alert, Descriptions } from 'antd';
import { ArrowLeftOutlined, QrcodeOutlined } from '@ant-design/icons';
import QrcodeDisplay from '../../components/QrcodeDisplay';
import { openSignin, getAttendanceList, getUnsignedList, manualSignin, getManagerActivityDetail } from '../../api/manager';
import { formatDateTime } from '../../utils/format';

/** 签到管理（社团端）：开启签到（限时二维码）+ 人工补签 + 签到名单 */
export default function ManagerSignin() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { message, modal } = App.useApp();
  const [activity, setActivity] = useState(null);
  const [signinInfo, setSigninInfo] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [unsigned, setUnsigned] = useState([]);
  const [opening, setOpening] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const timerRef = useRef(null);

  const loadLists = useCallback(async () => {
    setDataLoading(true);
    try {
      const [att, uns] = await Promise.all([getAttendanceList(id), getUnsignedList(id)]);
      setAttendance(att || []);
      setUnsigned(uns || []);
    } finally {
      setDataLoading(false);
    }
  }, [id]);

  useEffect(() => {
    getManagerActivityDetail(id).then(setActivity);
    loadLists();
    return () => clearInterval(timerRef.current);
  }, [id, loadLists]);

  // 开启签到后倒计时刷新提示（二维码 5 分钟有效）
  useEffect(() => {
    clearInterval(timerRef.current);
    if (signinInfo?.expiresAt) {
      timerRef.current = setInterval(() => {
        const remain = new Date(signinInfo.expiresAt.replace(/-/g, '/')).getTime() - Date.now();
        if (remain <= 0) {
          clearInterval(timerRef.current);
          setSigninInfo((info) => (info ? { ...info, expired: true } : info));
        }
      }, 1000);
    }
  }, [signinInfo]);

  const handleOpen = () => {
    modal.confirm({
      title: '开启签到',
      content: '将生成限时二维码（5 分钟内有效），新开将作废旧码，并通知已报名学生。确认开启？',
      onOk: async () => {
        setOpening(true);
        try {
          const data = await openSignin(id);
          setSigninInfo({ ...data, expired: false });
          message.success('签到已开启，有效期 5 分钟');
        } finally {
          setOpening(false);
        }
      },
    });
  };

  const handleManual = async (row) => {
    await manualSignin(id, { userId: row.userId });
    message.success(`已为 ${row.realName || row.userId} 补签`);
    loadLists();
  };

  const attColumns = [
    { title: '姓名', dataIndex: 'realName' },
    { title: '手机号', dataIndex: 'phone', render: (v) => v || '-' },
    { title: '方式', dataIndex: 'signType', render: (v) => (v === 'MANUAL' ? '人工补签' : '扫码') },
    { title: '签到时间', dataIndex: 'signTime' },
    { title: '操作人', dataIndex: 'operatorName', render: (v) => v || '-' },
  ];

  return (
    <Card
      className="page-card"
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/manager/activities')} />
          <span>签到管理{activity ? ` · ${activity.title}` : ''}</span>
        </Space>
      }
      extra={
        <Button type="primary" icon={<QrcodeOutlined />} loading={opening} onClick={handleOpen}>
          {signinInfo && !signinInfo.expired ? '重新开启签到' : '开启签到'}
        </Button>
      }
    >
      {signinInfo && !signinInfo.expired && (
        <Card type="inner" title="限时签到二维码（5 分钟内有效）" style={{ marginBottom: 16 }}>
          <QrcodeDisplay url={signinInfo.qrcodeUrl} />
          <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginTop: 8 }}>
            有效期至：{signinInfo.expiresAt}；学生扫码后跳转活动页自动签到。
          </Typography.Paragraph>
        </Card>
      )}
      {signinInfo?.expired && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="签到码已过期"
          description={`过期时间：${signinInfo.expiresAt}。请重新开启签到生成新码，旧码已作废。`}
        />
      )}

      <Descriptions
        size="small"
        column={{ xs: 1, sm: 2 }}
        style={{ marginBottom: 16 }}
        items={[
          { key: 'status', label: '活动状态', children: activity?.status || '-' },
          { key: 'signed', label: '已签到人数', children: attendance.length },
        ]}
      />

      <Card type="inner" title="未签到（报名成功成员，可补签）" style={{ marginBottom: 16 }}>
        <Table
          rowKey="signupId"
          size="small"
          loading={dataLoading}
          columns={[
            { title: '姓名', dataIndex: 'realName' },
            { title: '手机号', dataIndex: 'phone', render: (v) => v || '-' },
            { title: '报名时间', dataIndex: 'signupTime' },
            {
              title: '操作',
              render: (_, row) => <a onClick={() => handleManual(row)}>补签</a>,
            },
          ]}
          dataSource={unsigned}
          pagination={false}
          locale={{ emptyText: '报名成功成员均已签到' }}
          scroll={{ x: 'max-content' }}
        />
      </Card>

      <Card type="inner" title="签到名单">
        <Table
          rowKey="attendanceId"
          size="small"
          loading={dataLoading}
          columns={attColumns}
          dataSource={attendance}
          pagination={false}
          locale={{ emptyText: '暂无签到记录' }}
          scroll={{ x: 'max-content' }}
        />
      </Card>
    </Card>
  );
}
