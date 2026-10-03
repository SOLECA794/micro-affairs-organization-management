import request from '../utils/request';

/** 管理端：用户、社团、分类、审核、日志（04 文档 §4.4） */
export const pageUsers = (params) => request.get('/admin/users', { params });
export const updateUserStatus = (id, data) => request.put(`/admin/users/${id}`, data);
export const pageAssociations = (params) => request.get('/admin/associations', { params });
export const createAssociation = (data) => request.post('/admin/associations', data);
export const updateAssociation = (id, data) => request.put(`/admin/associations/${id}`, data);
export const pageCategories = (params) => request.get('/admin/categories', { params });
export const createCategory = (data) => request.post('/admin/categories', data);
export const updateCategory = (id, data) => request.put(`/admin/categories/${id}`, data);
export const deleteCategory = (id) => request.delete(`/admin/categories/${id}`);
export const pageAudits = (params) => request.get('/admin/audits', { params });
export const approveAudit = (id) => request.post(`/admin/audits/${id}/approve`);
export const rejectAudit = (id, data) => request.post(`/admin/audits/${id}/reject`, data);
export const pageLogs = (params) => request.get('/admin/logs', { params });
