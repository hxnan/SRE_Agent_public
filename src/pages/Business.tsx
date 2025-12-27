import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, BarChart3, DollarSign, ShoppingCart } from 'lucide-react'
import { apiFetch } from '../lib/api'
import { formatPrice, toNumber } from '../lib/utils'

interface OverviewResp {
  turnover: number
  total_sold: number
}

interface SalesItem {
  goods_id: number
  goods_name: string
  seckill_price: number
  sold_count: number
}

const Business: React.FC = () => {
  const navigate = useNavigate()
  const [overview, setOverview] = useState<OverviewResp>({ turnover: 0, total_sold: 0 })
  const [items, setItems] = useState<SalesItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        setError(null)
        const oResp = await apiFetch('/api/business/overview')
        const sResp = await apiFetch('/api/business/sales')
        if (!oResp.ok || !sResp.ok) throw new Error('加载业务看板失败')
        const o = await oResp.json()
        const s = await sResp.json()
        setOverview(o as OverviewResp)
        setItems((s.items || []) as SalesItem[])
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载失败')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <button onClick={() => navigate('/home')} className="flex items-center text-gray-600 hover:text-gray-900 mr-4">
                <ArrowLeft className="h-5 w-5 mr-1" />
                返回
              </button>
              <BarChart3 className="h-8 w-8 text-red-600 mr-3" />
              <h1 className="text-xl font-bold text-gray-900">业务看板</h1>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded mb-6">{error}</div>
        )}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center">
              <div className="p-3 bg-green-100 rounded-full">
                <DollarSign className="h-6 w-6 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm text-gray-500">营业额</p>
                <p className="text-2xl font-bold text-gray-900">{formatPrice(overview.turnover)}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center">
              <div className="p-3 bg-blue-100 rounded-full">
                <ShoppingCart className="h-6 w-6 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm text-gray-500">商品售出总量</p>
                <p className="text-2xl font-bold text-gray-900">{overview.total_sold}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">商品销量榜</h2>
          {loading ? (
            <div className="text-gray-600">加载中...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">商品</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">秒杀价</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">售出数量</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">销售额</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {items.map((it) => {
                    const revenue = toNumber(it.seckill_price) * toNumber(it.sold_count)
                    return (
                      <tr key={it.goods_id}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{it.goods_name}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{formatPrice(it.seckill_price)}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{it.sold_count}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{formatPrice(revenue)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}

export default Business

