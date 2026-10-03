import request from '../utils/request';

/** 站内通知（04 文档 §4.2） */
export const pageMyNotifications = (params) => request.get('/me/notifications', { params });
export const readNotification = (id) => request.post(`/me/notifications/${id}/read`);
