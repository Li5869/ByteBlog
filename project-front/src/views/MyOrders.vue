<script setup>
import {onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {NPagination} from 'naive-ui'
import {vipApi} from '@/utils/request'
import {toast} from '@/utils/toast'

const router = useRouter()
const loading = ref(true)
const activeFilter = ref('all')

// 状态筛选标签
const filters = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待确认' },
  { key: '2', label: '已完成' },
  { key: '3', label: '已取消' },
  { key: '4', label: '已关闭' }
]

// 订单列表
const orders = ref([])

// 状态映射
const statusMap = {
  0: { text: '待确认', class: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
  1: { text: '已冻结', class: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400' },
  2: { text: '已完成', class: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
  3: { text: '已取消', class: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400' },
  4: { text: '已关闭', class: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400' }
}

// 分页配置
const pagination = ref({ current: 1, size: 10, total: 0 })

// 获取订单列表（服务端分页 + 筛选）
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.value.current,
      size: pagination.value.size
    }
    // 非 'all' 时传入状态筛选参数
    if (activeFilter.value !== 'all') {
      params.status = activeFilter.value
    }
    const res = await vipApi.getOrderList(params)
    // request 已返回 data.data，res 就是分页对象本身
    const records = res?.records || []
    // bizSnapshot 是 JSON 字符串，需要解析为对象
    orders.value = records.map(item => ({
      ...item,
      bizSnapshot: typeof item.bizSnapshot === 'string' ? JSON.parse(item.bizSnapshot) : item.bizSnapshot
    }))
    pagination.value.total = res?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    toast.error('获取订单列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 监听筛选标签切换，重置页码并重新请求
watch(activeFilter, () => {
  pagination.value.current = 1
  fetchOrders()
})

onMounted(() => {
  fetchOrders()
})

const goToDetail = (orderId) => {
  router.push({ name: 'OrderDetail', params: { id: orderId } })
}

// 切换分页
const handlePageChange = (page) => {
  pagination.value.current = page
  fetchOrders()
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10">
      <div class="max-w-5xl mx-auto px-6 h-14 flex items-center justify-between">
        <button @click="router.push('/mine')" class="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          <span class="text-sm font-medium">返回</span>
        </button>
        <h1 class="text-sm font-semibold text-gray-900 dark:text-white">我的订单</h1>
        <div class="w-16"></div>
      </div>
    </div>

    <div class="max-w-5xl mx-auto px-6 py-6">
      <!-- 筛选标签 -->
      <div class="flex items-center gap-2 mb-6 flex-wrap">
        <button
          v-for="filter in filters"
          :key="filter.key"
          @click="activeFilter = filter.key"
          class="px-4 py-1.5 text-sm font-medium rounded-full transition-all"
          :class="activeFilter === filter.key
            ? 'bg-amber-500 text-white shadow-md shadow-amber-500/25'
            : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:border-amber-300 dark:hover:border-amber-600'"
        >
          {{ filter.label }}
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-20">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-500"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="orders.length === 0" class="text-center py-20">
        <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
          <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        </div>
        <p class="text-gray-500 dark:text-gray-400">暂无订单</p>
      </div>

      <!-- 订单列表 -->
      <div v-else class="space-y-4">
        <div
          v-for="order in orders"
          :key="order.id"
          @click="goToDetail(order.id)"
          class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
        >
          <!-- 订单头部 -->
          <div class="px-5 py-3 bg-gray-50 dark:bg-gray-750 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
            <div class="flex items-center gap-4">
              <span class="text-xs text-gray-500 dark:text-gray-400">{{ order.createdAt }}</span>
              <span class="text-xs text-gray-400">订单号：{{ order.orderNo }}</span>
            </div>
            <span class="px-2.5 py-0.5 text-xs font-semibold rounded-full" :class="statusMap[order.status]?.class">
              {{ statusMap[order.status]?.text }}
            </span>
          </div>

          <!-- 订单内容 -->
          <div class="p-5 flex items-center gap-4">
            <div class="w-14 h-14 bg-gradient-to-br from-amber-400 to-yellow-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg shadow-amber-500/20">
              <svg class="w-7 h-7 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-semibold text-gray-900 dark:text-white">VIP {{ order.bizSnapshot.planName }}</span>
                <span class="text-xs text-gray-400">{{ order.bizSnapshot.durationMonths }}个月</span>
              </div>
            </div>
            <div class="text-right flex-shrink-0">
              <div class="text-lg font-bold text-amber-600 dark:text-amber-400">{{ order.actualPoints }}</div>
              <div class="text-xs text-gray-400">积分</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页组件 -->
      <div v-if="pagination.total > 0" class="flex justify-center pt-6">
        <n-pagination
          v-model:page="pagination.current"
          :page-size="pagination.size"
          :item-count="pagination.total"
          @update:page="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
