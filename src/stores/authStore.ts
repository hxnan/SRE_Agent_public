import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { apiFetch } from '../lib/api';

interface User {
  id: number;
  username: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  checkAuth: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,

      login: async (username: string, password: string) => {
        const response = await apiFetch('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ username, password }),
        });

        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.error || 'Login failed');
        }

        const data = await response.json();
        
        set({
          user: data.user,
          token: data.token,
          isAuthenticated: true,
        });
      },

      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },

      checkAuth: () => {
        const state = get();
        if (!state.token) {
          return false;
        }

        // Check if token is expired (basic check)
        try {
          const payload = JSON.parse(atob(state.token.split('.')[1]));
          const currentTime = Date.now() / 1000;
          
          if (payload.exp && payload.exp < currentTime) {
            get().logout();
            return false;
          }
          
          return true;
        } catch {
          get().logout();
          return false;
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
