import dayjs from 'dayjs';

/** 活动状态中文映射（02 文档 §5.2 状态机） */
export const ACTIVITY_STATUS = {
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  PUBLISHED: '报名中',
  CANCELLED: '已取消',
  ENDED: '已结束',
  ARCHIVED: '已归档',
};

export const ACTIVITY_STATUS_COLOR = {
  DRAFT: 'default',
  PENDING: 'gold',
  APPROVED: 'cyan',
  REJECTED: 'red',
  PUBLISHED: 'green',
  CANCELLED: 'volcano',
  ENDED: 'blue',
  ARCHIVED: 'purple',
};

/** 报名状态映射 */
export const SIGNUP_STATUS = {
  ACTIVE: '已报名',
  WAITING: '候补中',
  CANCELLED: '已取消',
};

export const SIGNUP_STATUS_COLOR = {
  ACTIVE: 'green',
  WAITING: 'gold',
  CANCELLED: 'default',
};

/** 通知类型映射（03 文档 notification.type） */
export const NOTIFY_TYPE = {
  AUDIT: '审核',
  SIGNUP: '报名',
  WAITING: '候补',
  PROMOTED: '递补',
  CANCELLED: '取消',
  SIGNIN: '签到',
};

export function formatDateTime(value) {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

export function activityStatusText(status) {
  return ACTIVITY_STATUS[status] || status || '-';
}

export function signupStatusText(status) {
  return SIGNUP_STATUS[status] || status || '-';
}
