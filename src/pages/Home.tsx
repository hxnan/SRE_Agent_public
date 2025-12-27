import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag, User, LogOut, Package, BarChart3, Activity, AlertTriangle } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { useProductStore } from '../stores/productStore';
import ProductCard from '../components/ProductCard';

const Home: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout, isAuthenticated } = useAuthStore();
  const { products, loading, fetchProducts, currentPage, totalProducts, pageSize, sortBy, sortOrder, expiredOnly, setPageSize, setSort, setExpiredOnly } = useProductStore();
  const [seckillLoading, setSeckillLoading] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'all' | 'upcoming' | 'active' | 'ended'>('active');

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/');
      return;
    }
    
    fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
  }, [isAuthenticated, navigate, fetchProducts, activeTab]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleSeckill = async (productId: number) => {
    setSeckillLoading(productId);
    
    try {
      const response = await (await import('../lib/api')).apiFetch('/api/seckill', {
        method: 'POST',
        body: JSON.stringify({ goods_id: productId }),
      });

      const data = await response.json();
      
      if (data.success) {
        alert(`抢购成功！订单号: ${data.order_id}`);
        // Refresh products to update stock
        fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
      } else {
        alert(`抢购失败：${data.message}`);
      }
    } catch {
      alert('抢购失败，请稍后重试');
    } finally {
      setSeckillLoading(null);
    }
  };

  const tabs = [
    { key: 'all', label: '全部商品' },
    { key: 'upcoming', label: '即将开始' },
    { key: 'active', label: '正在进行' },
    { key: 'ended', label: '已结束' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <ShoppingBag className="h-8 w-8 text-red-600 mr-3" />
              <h1 className="text-xl font-bold text-gray-900">⚡ 秒杀商城</h1>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center text-sm text-gray-600">
                <User className="h-4 w-4 mr-1" />
                <span>欢迎, {user?.username}</span>
              </div>
              
              <button
                onClick={() => navigate('/orders')}
                className="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                <Package className="h-4 w-4 mr-1" />
                我的订单
              </button>
              
              <button
                onClick={() => navigate('/monitor')}
                className="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                <BarChart3 className="h-4 w-4 mr-1" />
                限流配置
              </button>
              <button
                onClick={() => navigate('/business')}
                className="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                <BarChart3 className="h-4 w-4 mr-1" />
                业务看板
              </button>
              <button
                onClick={() => navigate('/pressure')}
                className="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                <Activity className="h-4 w-4 mr-1" />
                流量压测
              </button>
              <button
                onClick={() => navigate('/faults')}
                className="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                <AlertTriangle className="h-4 w-4 mr-1" />
                故障注入
              </button>
              
              <button
                onClick={handleLogout}
                className="flex items-center px-3 py-2 text-sm text-red-600 hover:text-red-700 transition-colors"
              >
                <LogOut className="h-4 w-4 mr-1" />
                退出
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Tab Navigation */}
        <div className="mb-8">
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex space-x-8">
              {tabs.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key as 'all' | 'upcoming' | 'active' | 'ended')}
                  className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                    activeTab === tab.key
                      ? 'border-red-500 text-red-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </nav>
          </div>
        </div>

        {/* Controls */}
        <div className="mb-6 flex flex-wrap items-center gap-4">
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-600">每页数量</span>
            <select
              className="border rounded px-2 py-1 text-sm"
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
              }}
            >
              {[20,50,100,200,500].map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-600">排序字段</span>
            <select
              className="border rounded px-2 py-1 text-sm"
              value={sortBy}
              onChange={(e) => {
                setSort(e.target.value as 'created_at' | 'stock' | 'start_time' | 'end_time', sortOrder);
                fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
              }}
            >
              <option value="created_at">创建时间</option>
              <option value="stock">库存数量</option>
              <option value="start_time">开始时间</option>
              <option value="end_time">结束时间</option>
            </select>
            <select
              className="border rounded px-2 py-1 text-sm"
              value={sortOrder}
              onChange={(e) => {
                setSort(sortBy, e.target.value as 'asc' | 'desc');
                fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
              }}
            >
              <option value="asc">升序</option>
              <option value="desc">降序</option>
            </select>
          </div>
          <label className="flex items-center space-x-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={expiredOnly}
              onChange={(e) => {
                setExpiredOnly(e.target.checked);
                fetchProducts(1, activeTab === 'all' ? undefined : activeTab);
              }}
            />
            <span>仅显示已过期</span>
          </label>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
          </div>
        )}

        {/* Products Grid */}
        {!loading && products.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onSeckill={handleSeckill}
                seckillLoading={seckillLoading === product.id}
              />
            ))}
          </div>
        )}

        {!loading && totalProducts > 0 && (
          <div className="mt-8 flex items-center justify-center space-x-2">
            {currentPage > 1 && (
              <button
                onClick={() => fetchProducts(currentPage - 1, activeTab === 'all' ? undefined : activeTab)}
                className="px-3 py-2 text-sm border rounded hover:bg-gray-50"
              >
                上一页
              </button>
            )}
            {(() => {
              const limit = 20;
              const totalPages = Math.max(1, Math.ceil(totalProducts / limit));
              const start = Math.max(1, currentPage - 2);
              const end = Math.min(totalPages, currentPage + 2);
              const items = [] as number[];
              for (let p = start; p <= end; p++) items.push(p);
              return items.map((p) => (
                <button
                  key={p}
                  onClick={() => fetchProducts(p, activeTab === 'all' ? undefined : activeTab)}
                  className={`px-3 py-2 text-sm border rounded ${p === currentPage ? 'bg-red-600 text-white border-red-600' : 'hover:bg-gray-50'}`}
                >
                  {p}
                </button>
              ));
            })()}
            {(() => {
              const limit = 20;
              const totalPages = Math.max(1, Math.ceil(totalProducts / limit));
              return currentPage < totalPages ? (
                <button
                  onClick={() => fetchProducts(currentPage + 1, activeTab === 'all' ? undefined : activeTab)}
                  className="px-3 py-2 text-sm border rounded hover:bg-gray-50"
                >
                  下一页
                </button>
              ) : null;
            })()}
          </div>
        )}

        {/* Empty State */}
        {!loading && products.length === 0 && (
          <div className="text-center py-12">
            <ShoppingBag className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无商品</h3>
            <p className="text-gray-500">当前分类下没有可抢购的商品</p>
          </div>
        )}
      </main>
    </div>
  );
};

export default Home;
