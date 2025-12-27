import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag, ArrowLeft, Clock, Package, CheckCircle, XCircle } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { useOrderStore } from '../stores/orderStore';
import { formatPrice } from '../lib/utils';

const Orders: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const { orders, loading, fetchOrders } = useOrderStore();
  const [activeTab, setActiveTab] = useState<'all' | 'pending_payment' | 'paid' | 'shipped' | 'completed' | 'cancelled'>('all');

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/');
      return;
    }
    
    let status: number | undefined;
    switch (activeTab) {
      case 'pending_payment':
        status = 1; // PENDING_PAYMENT
        break;
      case 'paid':
        status = 2; // PAID
        break;
      case 'shipped':
        status = 4; // SHIPPED (assuming this value)
        break;
      case 'completed':
        status = 5; // COMPLETED (assuming this value)
        break;
      case 'cancelled':
        status = 3; // CANCELLED
        break;
      default:
        status = undefined;
    }
    
    fetchOrders(1, status);
  }, [isAuthenticated, navigate, fetchOrders, activeTab]);

  const getStatusInfo = (status: number) => {
    switch (status) {
      case 1:
        return { text: '待支付', color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock };
      case 2:
        return { text: '已支付', color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle };
      case 3:
        return { text: '已取消', color: 'text-red-600', bgColor: 'bg-red-100', icon: XCircle };
      default:
        return { text: '未知', color: 'text-gray-600', bgColor: 'bg-gray-100', icon: Package };
    }
  };

  const toNumber = (v: number | string): number => {
    return typeof v === 'number' ? v : parseFloat(v);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const tabs = [
    { key: 'all', label: '全部订单' },
    { key: 'pending', label: '待支付' },
    { key: 'paid', label: '已支付' },
    { key: 'cancelled', label: '已取消' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <button
                onClick={() => navigate('/home')}
                className="flex items-center text-gray-600 hover:text-gray-900 mr-4"
              >
                <ArrowLeft className="h-5 w-5 mr-1" />
                返回
              </button>
              <ShoppingBag className="h-8 w-8 text-red-600 mr-3" />
              <h1 className="text-xl font-bold text-gray-900">我的订单</h1>
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
                  onClick={() => setActiveTab(tab.key as 'all' | 'pending_payment' | 'paid' | 'shipped' | 'completed' | 'cancelled')}
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

        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
          </div>
        )}

        {/* Orders List */}
        {!loading && orders.length > 0 && (
          <div className="space-y-6">
            {orders.map((order) => {
              const statusInfo = getStatusInfo(order.status);
              const StatusIcon = statusInfo.icon;
              
              return (
                <div key={order.id} className="bg-white rounded-lg shadow-md p-6">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center">
                      <div className={`p-2 rounded-full ${statusInfo.bgColor} mr-3`}>
                        <StatusIcon className={`h-5 w-5 ${statusInfo.color}`} />
                      </div>
                      <div>
                        <h3 className="font-semibold text-gray-900">{order.goods_name}</h3>
                        <p className="text-sm text-gray-500">订单号: {order.id}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${statusInfo.bgColor} ${statusInfo.color}`}>
                        {statusInfo.text}
                      </span>
                    </div>
                  </div>

                  <div className="border-t border-gray-200 pt-4">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-gray-500">原价</p>
                        <p className="font-medium text-gray-900">{formatPrice(order.original_price)}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">秒杀价</p>
                        <p className="font-medium text-red-600">{formatPrice(order.seckill_price)}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">节省</p>
                        <p className="font-medium text-green-600">
                          {formatPrice(toNumber(order.original_price) - toNumber(order.seckill_price))}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-500">下单时间</p>
                        <p className="font-medium text-gray-900">{formatDate(order.create_time)}</p>
                      </div>
                    </div>
                  </div>

                  <div className="border-t border-gray-200 pt-4 mt-4">
                    <p className="text-sm text-gray-600">{order.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Empty State */}
        {!loading && orders.length === 0 && (
          <div className="text-center py-12">
            <Package className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无订单</h3>
            <p className="text-gray-500">您还没有任何订单，快去抢购商品吧！</p>
            <button
              onClick={() => navigate('/home')}
              className="mt-4 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
            >
              去抢购
            </button>
          </div>
        )}
      </main>
    </div>
  );
};

export default Orders;
