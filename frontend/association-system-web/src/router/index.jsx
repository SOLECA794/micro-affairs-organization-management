import { createBrowserRouter, Navigate } from 'react-router-dom';
import { useUserStore } from '../store/user';
import AppLayout from '../layouts/AppLayout';
import Login from '../views/Login';
import StudentActivityList from '../views/student/ActivityList';
import StudentActivityDetail from '../views/student/ActivityDetail';
import MySignups from '../views/student/MySignups';
import Profile from '../views/student/Profile';
import Notifications from '../views/student/Notifications';
import ManagerDashboard from '../views/manager/Dashboard';
import ManagerActivityList from '../views/manager/ActivityList';
import ManagerActivityEdit from '../views/manager/ActivityEdit';
import ManagerSignupList from '../views/manager/SignupList';
import ManagerSignin from '../views/manager/Signin';
import ManagerStats from '../views/manager/Stats';
import AdminUsers from '../views/admin/Users';
import AdminAssociations from '../views/admin/Associations';
import AdminCategories from '../views/admin/Categories';
import AdminAudits from '../views/admin/Audits';
import AdminLogs from '../views/admin/Logs';
import RequireAuth, { homeOf } from './RequireAuth';

/**
 * 路由表（05 文档 §2 的 17 个页面）。
 * 守卫规则：学生页要求 STUDENT，社团端要求 MANAGER，管理端要求 ADMIN；
 * 登录后默认页按角色跳转。
 */
export const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <RoleHome /> },
      // 学生端
      {
        element: <RequireAuth roles={['STUDENT', 'MANAGER', 'ADMIN']}><StudentActivityList /></RequireAuth>,
        path: 'activities',
      },
      {
        element: <RequireAuth roles={['STUDENT']}><StudentActivityDetail /></RequireAuth>,
        path: 'activities/:id',
      },
      {
        element: <RequireAuth roles={['STUDENT']}><MySignups /></RequireAuth>,
        path: 'me/signups',
      },
      {
        element: <RequireAuth roles={['STUDENT', 'MANAGER', 'ADMIN']}><Profile /></RequireAuth>,
        path: 'me/profile',
      },
      {
        element: <RequireAuth roles={['STUDENT', 'MANAGER', 'ADMIN']}><Notifications /></RequireAuth>,
        path: 'me/notifications',
      },
      // 社团端
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerDashboard /></RequireAuth>,
        path: 'manager',
      },
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerActivityList /></RequireAuth>,
        path: 'manager/activities',
      },
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerActivityEdit /></RequireAuth>,
        path: 'manager/activities/:id/edit',
      },
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerSignupList /></RequireAuth>,
        path: 'manager/activities/:id/signups',
      },
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerSignin /></RequireAuth>,
        path: 'manager/activities/:id/signin',
      },
      {
        element: <RequireAuth roles={['MANAGER']}><ManagerStats /></RequireAuth>,
        path: 'manager/activities/:id/stats',
      },
      // 管理端
      {
        element: <RequireAuth roles={['ADMIN']}><AdminUsers /></RequireAuth>,
        path: 'admin/users',
      },
      {
        element: <RequireAuth roles={['ADMIN']}><AdminAssociations /></RequireAuth>,
        path: 'admin/associations',
      },
      {
        element: <RequireAuth roles={['ADMIN']}><AdminCategories /></RequireAuth>,
        path: 'admin/categories',
      },
      {
        element: <RequireAuth roles={['ADMIN']}><AdminAudits /></RequireAuth>,
        path: 'admin/audits',
      },
      {
        element: <RequireAuth roles={['ADMIN']}><AdminLogs /></RequireAuth>,
        path: 'admin/logs',
      },
      { path: 'admin', element: <Navigate to="/admin/users" replace /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);

/** 按角色跳转默认页 */
function RoleHome() {
  const role = useUserStore((s) => s.role);
  return <Navigate to={homeOf(role)} replace />;
}
