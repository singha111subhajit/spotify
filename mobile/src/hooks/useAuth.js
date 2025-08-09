import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { loginUser, registerUser, fetchMe } from '../services/api';
import { setToken as setStoredToken, clearToken as clearStoredToken, getToken } from '../services/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  const bootstrapAuth = useCallback(async () => {
    const token = await getToken();
    if (!token) return;
    try {
      const me = await fetchMe();
      setUser(me);
    } catch {
      // ignore
    }
  }, []);

  const login = useCallback(async (userId, password) => {
    setLoading(true);
    try {
      const token = await loginUser(userId, password);
      await setStoredToken(token);
      const me = await fetchMe();
      setUser(me);
    } finally {
      setLoading(false);
    }
  }, []);

  const signup = useCallback(async (username, userId, password) => {
    setLoading(true);
    try {
      await registerUser(username, userId, password);
      await login(userId, password);
    } finally {
      setLoading(false);
    }
  }, [login]);

  const logout = useCallback(async () => {
    await clearStoredToken();
    setUser(null);
  }, []);

  const value = useMemo(() => ({
    user,
    isAuthenticated: !!user,
    loading,
    login,
    signup,
    logout,
    bootstrapAuth
  }), [user, loading, login, signup, logout, bootstrapAuth]);

  return (
    <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}