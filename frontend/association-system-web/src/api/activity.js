import request from '../utils/request';

/** 学生端：活动 / 报名 / 签到 / 通知（04 文档 §4.2） */
export const pageActivities = (params) => request.get('/activities', { params });
export const getActivityDetail = (id) => request.get(`/activities/${id}`);
export const signupActivity = (id) => request.post(`/activities/${id}/signup`);
export const cancelSignup = (id) => request.post(`/activities/${id}/cancel`);
export const pageMySignups = (params) => request.get('/me/signups', { params });
export const scanSignin = (data) => request.post('/signin/qrcode', data);
