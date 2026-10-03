import { useCallback, useEffect, useState } from 'react';
import { Card, List, Tag, Badge, Button, Typography, Empty, Pagination } from 'antd';
import { pageMyNotifications, readNotification } from '../../api/notification';
import { NOTIFY_TYPE } from '../../utils/format';

/** 我的通知（学生端）：分页 + 未读标记，可标记已读 */
export default function Notifications() {
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [unread, setUnread] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const fetchList = useCallback(async (p, s) => {
    setLoading(true);
    try {
      const data = await pageMyNotifications({ page: p, size: s });
      setList(data.list || []);
      setTotal(data.total || 0);
      setUnread(data.unreadCount || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, 10);
  }, [fetchList]);

  const markRead = async (item) => {
    if (item.readStatus === 1) return;
    await readNotification(item.id);
    fetchList(page, size);
  };

  const markAllVisible = async () => {
    for (const item of list.filter((i) => i.readStatus === 0)) {
      await readNotification(item.id);
    }
    fetchList(page, size);
  };

  return (
    <Card
      className="page-card"
      title={
        <span>
          我的通知 <Badge count={unread} style={{ marginLeft: 8 }} />
        </span>
      }
      extra={
        <Button size="small" onClick={markAllVisible} disabled={unread === 0}>
          全部已读（本页）
        </Button>
      }
    >
      <List
        loading={loading}
        dataSource={list}
        renderItem={(item) => (
          <List.Item
            style={{ cursor: item.readStatus === 1 ? 'default' : 'pointer' }}
            onClick={() => markRead(item)}
            extra={
              item.readStatus === 1
                ? <Typography.Text type="secondary">已读</Typography.Text>
                : <Badge status="processing" text="未读" />
            }
          >
            <List.Item.Meta
              title={
                <span>
                  <Tag>{NOTIFY_TYPE[item.type] || item.type}</Tag>
                  <Typography.Text strong={item.readStatus === 0}>{item.title}</Typography.Text>
                </span>
              }
              description={
                <span>
                  {item.content}
                  <Typography.Text type="secondary" style={{ marginLeft: 12 }}>{item.createdAt}</Typography.Text>
                </span>
              }
            />
          </List.Item>
        )}
        locale={{ emptyText: <Empty description="暂无通知" /> }}
      />
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
        <Pagination
          current={page}
          pageSize={size}
          total={total}
          showSizeChanger
          showTotal={(t) => `共 ${t} 条`}
          onChange={(p, s) => {
            setPage(p);
            setSize(s);
            fetchList(p, s);
          }}
        />
      </div>
    </Card>
  );
}
