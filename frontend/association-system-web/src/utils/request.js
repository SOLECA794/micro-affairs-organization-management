import axios from 'axios';
import { message } from 'antd';
import { useUserStore } from '../store/user';

/**
 * axios 封装（02 文档 §4 要求）：
 * - 请求头自动携带 Authorization: Bearer <token>；
 * - 响应统一解包 {code, message, data}；
 * - 401 清除登录态并跳转登录页；
 * - 业务错误统一 message 提示（调用方传 silent 可自行处理）。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

request.interceptors.request.use((config) => {
  const token = useUserStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    // Excel 等二进制响应直接返回
    if (response.config.responseType === 'blob') {
      return response;
    }
    const body = response.data;
    if (body == null || typeof body.code !== 'number') {
      return body;
    }
    if (body.code === 0) {
      return body.data;
    }
    if (body.code === 40100) {
      useUserStore.getState().clear();
      if (!window.location.pathname.startsWith('/login')) {
        message.warning(body.message || '登录已失效，请重新登录');
        window.location.href = '/login';
      }
      return Promise.reject(body);
    }
    if (!response.config.silent) {
      message.error(body.message || '操作失败');
    }
    return Promise.reject(body);
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      useUserStore.getState().clear();
      if (!window.location.pathname.startsWith('/login')) {
        message.warning('登录已失效，请重新登录');
        window.location.href = '/login';
      }
    } else if (!error.config?.silent) {
      message.error(error.response?.data?.message || '网络异常，请稍后重试');
    }
    return Promise.reject(error);
  }
);

/** 下载文件（带 Token 的 Excel 导出），文件名取自 Content-Disposition */
export async function downloadFile(url, fallbackName) {
  const response = await request.get(url, { responseType: 'blob' });
  const disposition = response.headers?.['content-disposition'] || '';
  const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  let filename = fallbackName;
  if (match) {
    try {
      filename = decodeURIComponent(match[1]);
    } catch {
      filename = match[1];
    }
  }
  const blob = new Blob([response.data], { type: response.headers?.['content-type'] });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(link.href);
}

export default request;
