import request, { downloadFile } from '../utils/request';

/** 社团端（负责人）：活动管理、名单、签到、统计、社团资料（04 文档 §4.3） */
export const pageManagerActivities = (params) => request.get('/manager/activities', { params });
export const getManagerActivityDetail = (id) => request.get(`/manager/activities/${id}`);
export const createActivity = (data) => request.post('/manager/activities', data);
export const updateActivity = (id, data) => request.put(`/manager/activities/${id}`, data);
export const submitAudit = (id) => request.post(`/manager/activities/${id}/submit`);
export const publishActivity = (id) => request.post(`/manager/activities/${id}/publish`);
export const cancelActivity = (id) => request.post(`/manager/activities/${id}/cancel`);
export const archiveActivity = (id) => request.post(`/manager/activities/${id}/archive`);
export const pageSignupList = (id, params) => request.get(`/manager/activities/${id}/signups`, { params });
export const openSignin = (id) => request.post(`/manager/activities/${id}/signin/open`);
export const manualSignin = (id, data) => request.post(`/manager/activities/${id}/signin/manual`, data);
export const getStatistics = (id) => request.get(`/manager/activities/${id}/statistics`);
export const getAttendanceList = (id) => request.get(`/manager/activities/${id}/attendance`);
export const getUnsignedList = (id) => request.get(`/manager/activities/${id}/unsigned`);
export const exportSignups = (id, name) =>
  downloadFile(`/manager/activities/${id}/export/signups`, name);
export const exportAttendance = (id, name) =>
  downloadFile(`/manager/activities/${id}/export/attendance`, name);
export const getMyAssociation = () => request.get('/manager/association');
export const updateMyAssociation = (data) => request.put('/manager/association', data);
export const getManagerOverview = (params) => request.get('/manager/statistics', { params });
