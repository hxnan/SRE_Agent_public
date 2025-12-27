import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

type LoadTestParams = {
  user_count: number;
  concurrency: number;
  duration_seconds?: number;
  ramp_up_seconds?: number;
  operation_mix: { query_pct: number; seckill_pct: number };
  think_time_ms_min?: number;
  think_time_ms_max?: number;
  qps_target?: number;
  goods_ids?: number[];
  retry_on_fail?: number;
  debug?: boolean;
};

type BatchCreateParams = {
  count: number;
  stock_per_item: number;
  seckill_price: number;
  original_price: number;
  start_time: string;
  end_time: string;
  name_prefix: string;
  description?: string;
};

const PressureTest: React.FC = () => {
  const navigate = useNavigate();
  const [params, setParams] = useState<LoadTestParams>({
    user_count: 1000,
    concurrency: 200,
    duration_seconds: 60,
    ramp_up_seconds: 10,
    operation_mix: { query_pct: 50, seckill_pct: 50 },
    think_time_ms_min: 50,
    think_time_ms_max: 150,
    qps_target: 0,
    goods_ids: [],
    retry_on_fail: 0,
    debug: false,
  });
  const [goodsIdsInput, setGoodsIdsInput] = useState<string>('');
  const [taskId, setTaskId] = useState<string | null>(null);
  const [status, setStatus] = useState<unknown | null>(null);
  const [results, setResults] = useState<unknown | null>(null);
  const [loading, setLoading] = useState(false);
  const pollerRef = useRef<number | null>(null);

  const [batchParams, setBatchParams] = useState<BatchCreateParams>({
    count: 1000,
    stock_per_item: 10000,
    seckill_price: 9.9,
    original_price: 19.9,
    start_time: new Date(Date.now() + 60_000).toISOString().slice(0, 16),
    end_time: new Date(Date.now() + 3_600_000).toISOString().slice(0, 16),
    name_prefix: '压测商品',
    description: '用于压测的大批量商品',
  });

  useEffect(() => {
    return () => {
      if (pollerRef.current) {
        window.clearInterval(pollerRef.current);
      }
    };
  }, []);

  const goodsIds = useMemo(() => {
    const ids = goodsIdsInput
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0)
      .map((s) => Number(s))
      .filter((n) => Number.isFinite(n) && n > 0);
    return ids;
  }, [goodsIdsInput]);

  const handleStart = async () => {
    setLoading(true);
    setResults(null);
    try {
      const body: LoadTestParams = { ...params, goods_ids: goodsIds.length ? goodsIds : undefined };
      const response = await (await import('../lib/api')).apiFetch('/api/loadtest/tasks', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      const data = await response.json();
      setTaskId(data.taskId || data.task_id || null);
      if (pollerRef.current) window.clearInterval(pollerRef.current);
      pollerRef.current = window.setInterval(handlePoll, 1000);
    } catch {
      alert('启动压测失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePoll = async () => {
    if (!taskId) return;
    try {
      const response = await (await import('../lib/api')).apiFetch(`/api/loadtest/tasks/${taskId}`);
      const data = await response.json();
      setStatus(data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleStop = async () => {
    if (!taskId) return;
    setLoading(true);
    try {
      await (await import('../lib/api')).apiFetch(`/api/loadtest/tasks/${taskId}`, { method: 'DELETE' });
      if (pollerRef.current) window.clearInterval(pollerRef.current);
      const resp = await (await import('../lib/api')).apiFetch(`/api/loadtest/tasks/${taskId}/results`);
      const data = await resp.json();
      setResults(data);
    } catch {
      alert('停止/获取结果失败');
    } finally {
      setLoading(false);
    }
  };

  const handleBatchCreate = async () => {
    setLoading(true);
    try {
      const payload = {
        ...batchParams,
        count: Number(batchParams.count),
        stock_per_item: Number(batchParams.stock_per_item),
        seckill_price: Number(batchParams.seckill_price),
        original_price: Number(batchParams.original_price),
        start_time: new Date(batchParams.start_time).toISOString(),
        end_time: new Date(batchParams.end_time).toISOString(),
      };
      const resp = await (await import('../lib/api')).apiFetch('/api/products/batch', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      const data = await resp.json();
      alert(`批量新增完成：新增 ${data.inserted || data.count || 0} 条，ID范围 ${data.id_start || ''} - ${data.id_end || ''}`);
    } catch {
      alert('批量新增失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <h1 className="text-xl font-bold text-gray-900">⚙️ 流量压测</h1>
            <button
              onClick={() => navigate('/home')}
              className="px-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
            >
              返回首页
            </button>
          </div>
        </div>
      </header>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <div className="bg-white shadow rounded-lg p-6 border">
          <h2 className="text-lg font-semibold mb-4">压测参数</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="block">
              <span className="text-sm text-gray-600">用户数</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.user_count}
                onChange={(e) => setParams({ ...params, user_count: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">并发</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.concurrency}
                onChange={(e) => setParams({ ...params, concurrency: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">压测时长(秒)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.duration_seconds ?? 0}
                onChange={(e) => setParams({ ...params, duration_seconds: Number(e.target.value) || undefined })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">预热时间(秒)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.ramp_up_seconds ?? 0}
                onChange={(e) => setParams({ ...params, ramp_up_seconds: Number(e.target.value) || undefined })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">查询比例(%)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.operation_mix.query_pct}
                onChange={(e) => setParams({ ...params, operation_mix: { ...params.operation_mix, query_pct: Number(e.target.value) } })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">抢购比例(%)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.operation_mix.seckill_pct}
                onChange={(e) => setParams({ ...params, operation_mix: { ...params.operation_mix, seckill_pct: Number(e.target.value) } })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">思考时间最小(ms)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.think_time_ms_min ?? 0}
                onChange={(e) => setParams({ ...params, think_time_ms_min: Number(e.target.value) || undefined })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">思考时间最大(ms)</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={params.think_time_ms_max ?? 0}
                onChange={(e) => setParams({ ...params, think_time_ms_max: Number(e.target.value) || undefined })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">Debug模式</span>
              <select className="mt-1 w-full border rounded px-3 py-2" value={params.debug ? 'true' : 'false'}
                onChange={(e) => setParams({ ...params, debug: e.target.value === 'true' })}>
                <option value="false">关闭</option>
                <option value="true">开启</option>
              </select>
            </label>
            <label className="block md:col-span-2">
              <span className="text-sm text-gray-600">商品ID列表(逗号分隔，可选)</span>
              <input type="text" className="mt-1 w-full border rounded px-3 py-2" value={goodsIdsInput}
                onChange={(e) => setGoodsIdsInput(e.target.value)} placeholder="例如: 1,2,3" />
            </label>
          </div>
          <div className="mt-6 space-x-3">
            <button onClick={handleStart} disabled={loading} className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700">开始压测</button>
            <button onClick={handleStop} disabled={loading || !taskId} className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300">停止并获取结果</button>
          </div>
          <div className="mt-6">
            <h3 className="text-md font-semibold mb-2">任务状态</h3>
            <pre className="bg-gray-50 border rounded p-3 text-sm overflow-auto max-h-64">{JSON.stringify(status, null, 2)}</pre>
          </div>
          <div className="mt-6">
            <h3 className="text-md font-semibold mb-2">最终结果</h3>
            <pre className="bg-gray-50 border rounded p-3 text-sm overflow-auto max-h-64">{JSON.stringify(results, null, 2)}</pre>
          </div>
        </div>

        <div className="bg-white shadow rounded-lg p-6 border">
          <h2 className="text-lg font-semibold mb-4">批量新增抢购商品</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="block">
              <span className="text-sm text-gray-600">商品数量</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.count}
                onChange={(e) => setBatchParams({ ...batchParams, count: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">每个库存</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.stock_per_item}
                onChange={(e) => setBatchParams({ ...batchParams, stock_per_item: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">秒杀价</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.seckill_price}
                onChange={(e) => setBatchParams({ ...batchParams, seckill_price: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">原价</span>
              <input type="number" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.original_price}
                onChange={(e) => setBatchParams({ ...batchParams, original_price: Number(e.target.value) })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">开始时间</span>
              <input type="datetime-local" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.start_time}
                onChange={(e) => setBatchParams({ ...batchParams, start_time: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm text-gray-600">结束时间</span>
              <input type="datetime-local" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.end_time}
                onChange={(e) => setBatchParams({ ...batchParams, end_time: e.target.value })} />
            </label>
            <label className="block md:col-span-2">
              <span className="text-sm text-gray-600">名称前缀</span>
              <input type="text" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.name_prefix}
                onChange={(e) => setBatchParams({ ...batchParams, name_prefix: e.target.value })} />
            </label>
            <label className="block md:col-span-2">
              <span className="text-sm text-gray-600">描述模板</span>
              <input type="text" className="mt-1 w-full border rounded px-3 py-2" value={batchParams.description ?? ''}
                onChange={(e) => setBatchParams({ ...batchParams, description: e.target.value })} />
            </label>
          </div>
          <div className="mt-6">
            <button onClick={handleBatchCreate} disabled={loading} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">新增一批抢购商品</button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default PressureTest;
