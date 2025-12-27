const API_BASE = import.meta.env.VITE_API_BASE || '';

const getToken = (): string | null => {
  try {
    const stored = localStorage.getItem('auth-storage');
    if (!stored) return null;
    const parsed = JSON.parse(stored);
    return parsed?.state?.token ?? null;
  } catch {
    return null;
  }
};

export const apiFetch = async (path: string, options: RequestInit = {}): Promise<Response> => {
  const url = API_BASE ? `${API_BASE}${path}` : path;
  const token = getToken();

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const resp = await fetch(url, { ...options, headers });
  return resp;
};

export default apiFetch;
