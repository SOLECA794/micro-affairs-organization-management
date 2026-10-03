import { create } from 'zustand';

const STORAGE_KEY = 'assoc_user';

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : { token: null, user: null };
  } catch {
    return { token: null, user: null };
  }
}

/**
 * 登录态与当前用户（对应 02 文档 store 职责：Token、用户信息、角色）。
 * persist 到 localStorage，刷新不丢失。
 */
export const useUserStore = create((set) => {
  const initial = loadFromStorage();
  return {
    token: initial.token,
    user: initial.user,
    role: initial.user?.role || null,

    setLogin: (token, user) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }));
      set({ token, user, role: user?.role || null });
    },

    setUser: (user) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: useUserStore.getState().token, user }));
      set({ user, role: user?.role || null });
    },

    clear: () => {
      localStorage.removeItem(STORAGE_KEY);
      set({ token: null, user: null, role: null });
    },
  };
});
