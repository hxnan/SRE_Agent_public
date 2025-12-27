import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag, User, Lock } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(username, password);
      navigate('/home');
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败，请检查用户名和密码');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-red-50 to-red-100 flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        {/* Logo and Title */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <ShoppingBag className="h-12 w-12 text-red-600" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">⚡ 秒杀商城</h1>
          <p className="text-gray-600">限时抢购，先到先得！</p>
        </div>

        {/* Login Form */}
        <div className="bg-white rounded-lg shadow-lg p-8">
          <div className="text-center mb-6">
            <h2 className="text-2xl font-semibold text-gray-900">用户登录</h2>
            <p className="text-gray-600 mt-2">请输入您的账户信息</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-red-600 text-sm text-center">{error}</p>
              </div>
            )}

            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
                用户名
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  placeholder="请输入用户名"
                  required
                  disabled={isLoading}
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                密码
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  placeholder="请输入密码"
                  required
                  disabled={isLoading}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-red-600 text-white py-3 px-4 rounded-lg hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200 font-medium"
            >
              {isLoading ? '登录中...' : '登录'}
            </button>
          </form>

          {/* Demo credentials */}
          <div className="mt-6 p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-600 mb-2 text-center">测试账户：</p>
            <div className="space-y-1 text-xs text-gray-500">
              <p>• 用户名: testuser | 密码: test123</p>
              <p>• 用户名: admin | 密码: admin123</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;