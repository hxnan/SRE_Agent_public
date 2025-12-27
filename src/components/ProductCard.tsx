import React, { useState } from 'react';
import { ShoppingBag, Clock } from 'lucide-react';
import CountdownTimer from './CountdownTimer';
import { useAuthStore } from '../stores/authStore';

interface Product {
  id: number;
  name: string;
  description: string;
  stock: number;
  original_price: number;
  seckill_price: number;
  start_time: string;
  end_time: string;
}

interface ProductCardProps {
  product: Product;
  onSeckill: (productId: number) => void;
  seckillLoading?: boolean;
}

const ProductCard: React.FC<ProductCardProps> = ({ 
  product, 
  onSeckill, 
  seckillLoading = false 
}) => {
  const [status, setStatus] = useState<'upcoming' | 'active' | 'ended'>('upcoming');
  const { isAuthenticated } = useAuthStore();
  const isSoldOut = product.stock <= 0;

  const handleStatusChange = (newStatus: 'upcoming' | 'active' | 'ended') => {
    setStatus(newStatus);
  };

  const handleSeckill = () => {
    if (status === 'active' && isAuthenticated) {
      onSeckill(product.id);
    }
  };

  const getButtonText = () => {
    if (!isAuthenticated) return '请先登录';
    if (isSoldOut) return '已售罄';
    switch (status) {
      case 'upcoming':
        return '即将开始';
      case 'active':
        return '立即抢购';
      case 'ended':
        return '已结束';
      default:
        return '未知状态';
    }
  };

  const getButtonStyle = () => {
    if (!isAuthenticated) return 'bg-gray-400 cursor-not-allowed';
    if (isSoldOut) return 'bg-gray-400 cursor-not-allowed';
    switch (status) {
      case 'upcoming':
        return 'bg-gray-500 cursor-not-allowed';
      case 'active':
        return 'bg-red-600 hover:bg-red-700 cursor-pointer';
      case 'ended':
        return 'bg-gray-400 cursor-not-allowed';
      default:
        return 'bg-gray-400 cursor-not-allowed';
    }
  };

  const formatPrice = (price: number | string) => {
    const n = typeof price === 'number' ? price : parseFloat(price);
    return Number.isFinite(n) ? `¥${n.toFixed(2)}` : '¥-';
  };

  const truncateText = (text: string, maxLength: number) => {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  return (
    <div className={`bg-white rounded-lg shadow-md overflow-hidden transition-shadow duration-200 ${status === 'ended' || isSoldOut ? 'opacity-60' : 'hover:shadow-lg'}`}>
      {/* Product Image Placeholder */}
      <div className="h-48 bg-gradient-to-br from-red-100 to-red-200 flex items-center justify-center">
        <ShoppingBag className="h-16 w-16 text-red-500" />
      </div>

      <div className="p-4">
        {/* Product Name */}
        <h3 className="font-semibold text-gray-900 mb-2">
          {truncateText(product.name, 32)}
        </h3>

        {/* Description */}
        <p className="text-gray-600 text-sm mb-4 line-clamp-2">
          {truncateText(product.description, 50)}
        </p>

        {/* Stock Info */}
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm text-gray-500">
            库存: {product.stock}
          </span>
          <div className="flex items-center text-sm text-gray-500">
            <Clock className="h-4 w-4 mr-1" />
            <span>限时</span>
          </div>
        </div>

        {/* Price Display */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <span className="text-2xl font-bold text-red-600">
              {formatPrice(product.seckill_price)}
            </span>
            <span className="text-sm text-gray-500 line-through">
              {formatPrice(product.original_price)}
            </span>
          </div>
          <div className="text-sm text-red-600 font-medium">
            {(() => {
              const orig = typeof product.original_price === 'number' ? product.original_price : parseFloat(product.original_price as unknown as string);
              const sec = typeof product.seckill_price === 'number' ? product.seckill_price : parseFloat(product.seckill_price as unknown as string);
              const discount = Number.isFinite(orig) && Number.isFinite(sec) ? orig - sec : NaN;
              return `省 ${formatPrice(discount)}`;
            })()}
          </div>
        </div>

        {/* Countdown Timer */}
        <div className="mb-4">
          <CountdownTimer 
            startTime={product.start_time} 
            endTime={product.end_time}
            onStatusChange={handleStatusChange}
          />
        </div>

        {/* Seckill Button */}
        <button
          onClick={handleSeckill}
          disabled={status !== 'active' || !isAuthenticated || seckillLoading || isSoldOut}
          className={`w-full py-3 px-4 rounded-lg text-white font-medium transition-colors duration-200 ${getButtonStyle()}`}
        >
          {seckillLoading ? '抢购中...' : getButtonText()}
        </button>
      </div>
    </div>
  );
};

export default ProductCard;
