import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Activity } from 'lucide-react';

const Monitor: React.FC = () => {
  const navigate = useNavigate();
  const [rateLimit, setRateLimit] = useState({ seckill_window_seconds: 10, seckill_max: 3, api_window_seconds: 60, api_max: 100 });
  const [savingRateLimit, setSavingRateLimit] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const resp = await (await import('../lib/api')).apiFetch('/api/monitor/ratelimit');
        const data = await resp.json();
        setRateLimit({
          seckill_window_seconds: Number(data.seckill_window_seconds ?? 10),
          seckill_max: Number(data.seckill_max ?? 3),
          api_window_seconds: Number(data.api_window_seconds ?? 60),
          api_max: Number(data.api_max ?? 100),
        });
      } catch (e) {
        console.error(e);
      }
    })();
  }, []);

  const saveRateLimit = async () => {
    setSavingRateLimit(true);
    try {
      const resp = await (await import('../lib/api')).apiFetch('/api/monitor/ratelimit', {
        method: 'PUT',
        body: JSON.stringify(rateLimit),
      });
      const data = await resp.json();
      setRateLimit({
        seckill_window_seconds: Number(data.seckill_window_seconds ?? rateLimit.seckill_window_seconds),
        seckill_max: Number(data.seckill_max ?? rateLimit.seckill_max),
        api_window_seconds: Number(data.api_window_seconds ?? rateLimit.api_window_seconds),
        api_max: Number(data.api_max ?? rateLimit.api_max),
      });
      alert('限流配置已更新');
    } catch {
      alert('更新限流配置失败');
    } finally {
      setSavingRateLimit(false);
    }
  };

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
              <Activity className="h-8 w-8 text-red-600 mr-3" />
              <h1 className="text-xl font-bold text-gray-900">限流配置</h1>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Rate Limit Config */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">限流配置</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="block">
              <span className="text-sm text-gray-600">秒杀窗口(秒)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={rateLimit.seckill_window_seconds}
                onChange={(e) => setRateLimit({ ...rateLimit, seckill_window_seconds: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">秒杀最大次数</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={rateLimit.seckill_max}
                onChange={(e) => setRateLimit({ ...rateLimit, seckill_max: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">通用窗口(秒)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={rateLimit.api_window_seconds}
                onChange={(e) => setRateLimit({ ...rateLimit, api_window_seconds: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">通用最大次数</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={rateLimit.api_max}
                onChange={(e) => setRateLimit({ ...rateLimit, api_max: Number(e.target.value) })} />
            </label>
          </div>
          <div className="mt-6">
            <button onClick={saveRateLimit} disabled={savingRateLimit}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
              保存配置
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Monitor;
