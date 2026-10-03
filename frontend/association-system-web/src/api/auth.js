import request from '../utils/request';

/** 认证（04 文档 §4.1） */
export const login = (data) => request.post('/auth/login', data);
export const logout = () => request.post('/auth/logout');
export const changePassword = (data) => request.post('/auth/password', data);
export const getMe = () => request.get('/auth/me');
export const updateProfile = (data) => request.put('/auth/profile', data);
