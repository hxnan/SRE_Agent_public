import { create } from 'zustand';

interface Product {
  id: number;
  name: string;
  description: string;
  stock: number;
  original_price: number;
  seckill_price: number;
  start_time: string;
  end_time: string;
  created_at: string;
  updated_at: string;
}

interface ProductState {
  products: Product[];
  loading: boolean;
  error: string | null;
  currentPage: number;
  totalProducts: number;
  pageSize: number;
  sortBy: 'created_at' | 'stock' | 'start_time' | 'end_time';
  sortOrder: 'asc' | 'desc';
  expiredOnly: boolean;
  setPageSize: (size: number) => void;
  setSort: (by: 'created_at' | 'stock' | 'start_time' | 'end_time', order: 'asc' | 'desc') => void;
  setExpiredOnly: (expired: boolean) => void;
  fetchProducts: (page?: number, status?: string) => Promise<void>;
  getProductById: (id: number) => Product | undefined;
}

export const useProductStore = create<ProductState>((set, get) => ({
  products: [],
  loading: false,
  error: null,
  currentPage: 1,
  totalProducts: 0,
  pageSize: 20,
  sortBy: 'start_time',
  sortOrder: 'asc',
  expiredOnly: false,

  setPageSize: (size: number) => {
    set({ pageSize: size });
  },
  setSort: (by, order) => {
    set({ sortBy: by, sortOrder: order });
  },
  setExpiredOnly: (expired: boolean) => {
    set({ expiredOnly: expired });
  },

  fetchProducts: async (page = 1, status?: string) => {
    set({ loading: true, error: null });
    
    try {
      const { pageSize, sortBy, sortOrder, expiredOnly } = get();
      const params = new URLSearchParams({
        page: page.toString(),
        limit: String(pageSize),
      });
      
      if (status) {
        params.append('status', status);
      }
      if (sortBy) {
        params.append('sort_by', sortBy);
      }
      if (sortOrder) {
        params.append('sort_order', sortOrder);
      }
      if (expiredOnly) {
        params.append('expired', 'true');
      } else if (status !== 'ended') {
        params.append('expired', 'false');
        params.append('available', 'true');
      }
      
      const response = await (await import('../lib/api')).apiFetch(`/api/products?${params.toString()}`);
      
      if (!response.ok) {
        throw new Error('Failed to fetch products');
      }
      
      const data = await response.json();
      
      set({
        products: data.products,
        currentPage: data.page,
        totalProducts: data.total,
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch products',
        loading: false,
      });
    }
  },

  getProductById: (id: number) => {
    return get().products.find(product => product.id === id);
  },
}));
