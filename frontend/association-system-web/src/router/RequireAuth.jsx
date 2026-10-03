import { Navigate, useLocation } from 'react-router-dom';
import { useUserStore } from '../store/user';

/**
 * 路由守卫：未登录跳登录页；已登录但角色不符跳本人首页（01 文档 §4 权限隔离）。
 */
export default function RequireAuth({ roles, children }) {
  const token = useUserStore((s) => s.token);
  const role = useUserStore((s) => s.role);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && role && !roles.includes(role)) {
    return <Navigate to={homeOf(role)} replace />;
  }
  return children;
}

export function homeOf(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'MANAGER') return '/manager';
  return '/activities';
}
