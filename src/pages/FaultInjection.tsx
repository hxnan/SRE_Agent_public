import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

const FaultInjection: React.FC = () => {
  const navigate = useNavigate();
  const [locked, setLocked] = useState(false);
  const [tableName, setTableName] = useState('seckill_goods');
  const [loading, setLoading] = useState(false);

  const refreshStatus = async () => {
    try {
      const resp = await (await import('../lib/loadtest-api')).loadtestFetch('/faults/db/deadlock/status');
      const data = await resp.json();
      setLocked(Boolean(data.locked));
      if (data.table) setTableName(String(data.table));
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    refreshStatus();
  }, []);

  const lock = async () => {
    setLoading(true);
    try {
      const resp = await (await import('../lib/loadtest-api')).loadtestFetch('/faults/db/deadlock/lock', {
        method: 'POST',
        body: JSON.stringify({ table_name: tableName }),
      });
      const data = await resp.json();
      if (resp.ok) {
        setLocked(true);
        alert('已注入：表锁生效');
      } else {
        alert(`注入失败：${data.error || 'unknown error'}`);
      }
    } catch {
      alert('注入失败');
    } finally {
      setLoading(false);
    }
  };

  const unlock = async () => {
    setLoading(true);
    try {
      const resp = await (await import('../lib/loadtest-api')).loadtestFetch('/faults/db/deadlock/unlock', {
        method: 'POST',
      });
      await resp.json().catch(() => ({}));
      setLocked(false);
      alert('已恢复：表锁释放');
    } catch {
      alert('恢复失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
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
              <ShieldAlert className="h-8 w-8 text-orange-600 mr-3" />
              <h1 className="text-xl font-bold text-gray-900">故障注入</h1>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">数据库死锁注入（表锁）</h2>
          <p className="text-sm text-gray-600 mb-4">
            使用 MySQL <code className="bg-gray-100 px-1 rounded">LOCK TABLES</code> 锁定商品表，阻断库存更新与下单流程；点击“恢复”释放表锁。
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="block">
              <span className="text-sm text-gray-600">表名</span>
              <input
                type="text"
                className="mt-1 w-full border rounded px-3 py-2"
                value={tableName}
                onChange={(e) => setTableName(e.target.value)}
              />
            </label>
            <div className="block">
              <span className="text-sm text-gray-600">当前状态</span>
              <div className="mt-1 px-3 py-2 rounded border">
                {locked ? (
                  <span className="text-red-600 font-medium">已加锁（阻断中）</span>
                ) : (
                  <span className="text-green-600 font-medium">未加锁（正常）</span>
                )}
              </div>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <button
              onClick={lock}
              disabled={loading || locked}
              className="px-4 py-2 bg-orange-600 text-white rounded hover:bg-orange-700 disabled:opacity-50"
            >
              注入死锁
            </button>
            <button
              onClick={unlock}
              disabled={loading || !locked}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              恢复
            </button>
            <button
              onClick={refreshStatus}
              disabled={loading}
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300"
            >
              刷新状态
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default FaultInjection;
