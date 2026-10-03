import { Tag } from 'antd';
import {
  ACTIVITY_STATUS,
  ACTIVITY_STATUS_COLOR,
  SIGNUP_STATUS,
  SIGNUP_STATUS_COLOR,
} from '../utils/format';

/** 状态标签：type = activity | signup */
export default function StatusTag({ status, type = 'activity' }) {
  const map = type === 'signup' ? SIGNUP_STATUS : ACTIVITY_STATUS;
  const colorMap = type === 'signup' ? SIGNUP_STATUS_COLOR : ACTIVITY_STATUS_COLOR;
  const text = map[status] || status || '-';
  return <Tag color={colorMap[status] || 'default'}>{text}</Tag>;
}
