import { create } from 'zustand';
import { apiFetch } from '../lib/api';

interface Order {
  id: number;
  user_id: number;
  goods_id: number;
  create_time: string;
  status: number;
  goods_name: string;
  original_price: number;
  seckill_price: number;
  description: string;
}

interface OrderState {
  orders: Order[];
  loading: boolean;
  error: string | null;
  currentPage: number;
  totalOrders: number;
  fetchOrders: (page?: number, status?: number) => Promise<void>;
}

export const useOrderStore = create<OrderState>((set) => ({
  orders: [],
  loading: false,
  error: null,
  currentPage: 1,
  totalOrders: 0,

  fetchOrders: async (page = 1, status?: number) => {
    set({ loading: true, error: null });
    
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        limit: '20',
      });
      
      if (status !== undefined) {
        params.append('status', status.toString());
      }
      
      const response = await apiFetch(`/api/orders?${params.toString()}`);
      
      if (!response.ok) {
        throw new Error('Failed to fetch orders');
      }
      
      const data = await response.json();
      
      set({
        orders: data.orders,
        currentPage: data.page,
        totalOrders: data.total,
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch orders',
        loading: false,
      });
    }
  },
}));
