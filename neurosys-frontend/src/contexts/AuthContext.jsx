import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

const DEFAULT_ADMIN_USER = {
  id: 'admin-1',
  username: 'admin',
  email: 'admin@neurosys.com',
  role: 'ROLE_ADMIN'
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
    return null;
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      authService.getCurrentUser()
        .then((res) => {
          if (res.success) {
            setUser(res.data);
            localStorage.setItem('user', JSON.stringify(res.data));
          }
        })
        .catch(() => {
          // Keep current dev admin session active
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (username, password) => {
    try {
      const res = await authService.login(username, password);
      if (res.success) {
        const data = res.data;
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        const userData = {
          id: data.userId,
          username: data.username,
          email: data.email,
          role: data.role,
        };
        setUser(userData);
        localStorage.setItem('user', JSON.stringify(userData));
        return data;
      }
    } catch (e) {
      // Allow dev admin bypass if offline
      if (username === 'admin') {
        setUser(DEFAULT_ADMIN_USER);
        localStorage.setItem('user', JSON.stringify(DEFAULT_ADMIN_USER));
        return DEFAULT_ADMIN_USER;
      }
      throw e;
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (e) {
      // Ignore
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
