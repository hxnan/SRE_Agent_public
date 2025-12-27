const LOADTEST_BASE = (import.meta as any).env?.VITE_LOADTEST_BASE || 'http://localhost:8081';

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

export const loadtestFetch = async (path: string, options: RequestInit = {}): Promise<Response> => {
  const base = LOADTEST_BASE.endsWith('/') ? LOADTEST_BASE.slice(0, -1) : LOADTEST_BASE;
  const url = `${base}${path}`;
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

export default loadtestFetch;
