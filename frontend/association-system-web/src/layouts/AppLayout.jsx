import { useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Layout, Menu, Dropdown, Avatar, Grid, Typography } from 'antd';
import {
  HomeOutlined,
  CalendarOutlined,
  BellOutlined,
  UserOutlined,
  DashboardOutlined,
  TeamOutlined,
  ApartmentOutlined,
  TagsOutlined,
  AuditOutlined,
  FileTextOutlined,
  MenuOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { App as AntApp } from 'antd';
import { useUserStore } from '../store/user';
import { logout } from '../api/auth';

const { Header, Sider, Content } = Layout;

const MENUS = {
  STUDENT: [
    { key: '/activities', icon: <CalendarOutlined />, label: '活动列表' },
    { key: '/me/signups', icon: <HomeOutlined />, label: '我的报名' },
    { key: '/me/notifications', icon: <BellOutlined />, label: '我的通知' },
    { key: '/me/profile', icon: <UserOutlined />, label: '个人中心' },
  ],
  MANAGER: [
    { key: '/manager', icon: <DashboardOutlined />, label: '工作台' },
    { key: '/manager/activities', icon: <CalendarOutlined />, label: '活动管理' },
    { key: '/me/profile', icon: <UserOutlined />, label: '个人中心' },
  ],
  ADMIN: [
    { key: '/admin/users', icon: <TeamOutlined />, label: '用户管理' },
    { key: '/admin/associations', icon: <ApartmentOutlined />, label: '社团管理' },
    { key: '/admin/categories', icon: <TagsOutlined />, label: '分类管理' },
    { key: '/admin/audits', icon: <AuditOutlined />, label: '活动审核' },
    { key: '/admin/logs', icon: <FileTextOutlined />, label: '日志查询' },
  ],
};

const ROLE_NAME = { STUDENT: '学生端', MANAGER: '社团管理端', ADMIN: '系统管理端' };

export default function AppLayout() {
  const { user, role, clear } = useUserStore();
  const navigate = useNavigate();
  const location = useLocation();
  const { useBreakpoint } = Grid;
  const screens = useBreakpoint();
  const isMobile = !screens.md;
  const { message } = AntApp.useApp();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const items = MENUS[role] || MENUS.STUDENT;
  const selectedKey = useMemo(() => {
    const match = items
      .map((i) => i.key)
      .filter((k) => location.pathname.startsWith(k))
      .sort((a, b) => b.length - a.length)[0];
    return match || location.pathname;
  }, [location.pathname, items]);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 忽略登出接口异常，本地仍清除登录态
    }
    clear();
    message.success('已退出登录');
    navigate('/login');
  };

  const userMenu = {
    items: [
      { key: 'profile', icon: <UserOutlined />, label: '个人中心' },
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
    ],
    onClick: ({ key }) => {
      if (key === 'logout') handleLogout();
      if (key === 'profile') navigate('/me/profile');
    },
  };

  const menuClick = ({ key }) => {
    navigate(key);
    setDrawerOpen(false);
  };

  const brand = (
    <div
      style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', height: 64, paddingInline: 16 }}
      onClick={() => navigate(role === 'ADMIN' ? '/admin/users' : role === 'MANAGER' ? '/manager' : '/activities')}
    >
      <ApartmentOutlined style={{ fontSize: 22, color: '#4f46e5' }} />
      <Typography.Text strong style={{ fontSize: 15, whiteSpace: 'nowrap' }}>
        {isMobile ? '社团系统' : `社团活动报名与签到系统 · ${ROLE_NAME[role] || ''}`}
      </Typography.Text>
    </div>
  );

  if (role === 'STUDENT' || !role) {
    // 学生端：顶部导航 + 移动端抽屉菜单
    return (
      <Layout style={{ minHeight: '100vh' }}>
        <Header
          style={{
            background: '#fff',
            borderBottom: '1px solid #eee',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingInline: isMobile ? 0 : 24,
            position: 'sticky',
            top: 0,
            zIndex: 100,
          }}
        >
          {isMobile ? (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <MenuOutlined onClick={() => setDrawerOpen(true)} style={{ fontSize: 18, marginInline: 12 }} />
                <Typography.Text strong>社团活动系统</Typography.Text>
              </div>
              <Dropdown menu={userMenu}>
                <Avatar style={{ backgroundColor: '#4f46e5', marginInlineEnd: 12 }} icon={<UserOutlined />} />
              </Dropdown>
            </>
          ) : (
            <>
              {brand}
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <Menu
                  mode="horizontal"
                  items={items}
                  selectedKeys={[selectedKey]}
                  onClick={menuClick}
                  style={{ flex: 1, minWidth: 320, borderBottom: 'none' }}
                />
                <Dropdown menu={userMenu}>
                  <span style={{ cursor: 'pointer' }}>
                    <Avatar style={{ backgroundColor: '#4f46e5' }} icon={<UserOutlined />} />
                    <span style={{ marginLeft: 8 }}>{user?.realName}</span>
                  </span>
                </Dropdown>
              </div>
            </>
          )}
        </Header>
        {isMobile && drawerOpen && (
          <Menu
            mode="inline"
            items={items}
            selectedKeys={[selectedKey]}
            onClick={menuClick}
            style={{ borderInlineEnd: 'none', background: '#fff', borderBottom: '1px solid #eee' }}
          />
        )}
        <Content style={{ padding: isMobile ? 12 : 24, maxWidth: 1080, width: '100%', margin: '0 auto' }}>
          <Outlet />
        </Content>
      </Layout>
    );
  }

  // 管理端/社团端：侧边菜单
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" breakpoint="md" collapsedWidth="0" width={208}>
        <div
          style={{
            color: '#fff',
            padding: '0 16px',
            height: 64,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            fontWeight: 600,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
          }}
        >
          <ApartmentOutlined style={{ fontSize: 20 }} />
          <span style={{ fontSize: 14 }}>社团活动系统</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          items={items}
          selectedKeys={[selectedKey]}
          onClick={menuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            borderBottom: '1px solid #eee',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingInline: 24,
          }}
        >
          <Typography.Text strong>{ROLE_NAME[role]}</Typography.Text>
          <Dropdown menu={userMenu}>
            <span style={{ cursor: 'pointer' }}>
              <Avatar style={{ backgroundColor: '#4f46e5' }} icon={<UserOutlined />} />
              <span style={{ marginLeft: 8 }}>{user?.realName}</span>
              <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>({user?.username})</span>
            </span>
          </Dropdown>
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
