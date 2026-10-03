import request from '../utils/request';

/** 学生端：活动 / 报名 / 签到 / 通知（04 文档 §4.2） */
export const pageActivities = (params) => request.get('/activities', { params });
export const getActivityDetail = (id) => request.get(`/activities/${id}`);
export const signupActivity = (id) => request.post(`/activities/${id}/signup`);
export const cancelSignup = (id) => request.post(`/activities/${id}/cancel`);
export const pageMySignups = (params) => request.get('/me/signups', { params });
export const scanSignin = (data) => request.post('/signin/qrcode', data);

/** 公开下拉选项（实现期扩展）：社团/分类 [{id, name}]，用于活动筛选 */
export const getAssociationOptions = () => request.get('/options/associations');
export const getCategoryOptions = () => request.get('/options/categories');
