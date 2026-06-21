<script setup>
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {couponApi} from '@/utils/request'

const router = useRouter()

// 当前筛选状态：all-全部 unused-未使用 used-已使用 expired-已过期
const activeTab = ref('all')

// 优惠券数据及分页
const coupons = ref([])
const loading = ref(false)
const current = ref(1)
const total = ref(0)
const pageSize = ref(20)

// 优惠券统计
const stats = ref({ total: 0, unused: 0, used: 0, expired: 0 })
const statsLoading = ref(false)

// 状态映射
const statusMap = { all: null, unused: 0, used: 1, expired: 2 }

// 加载优惠券列表
const loadCoupons = async () => {
  loading.value = true
  try {
    const data = await couponApi.getMyCoupons(current.value, pageSize.value, statusMap[activeTab.value])
    coupons.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载优惠券失败', e)
  } finally {
    loading.value = false
  }
}

// 加载优惠券统计
const loadStats = async () => {
  statsLoading.value = true
  try {
    const data = await couponApi.getMyCouponStats()
    stats.value = data
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    statsLoading.value = false
  }
}

// 初始加载
loadCoupons()
loadStats()

// 后端已按状态筛选，直接返回
const filteredCoupons = computed(() => coupons.value)

// 各状态数量统计（从后端接口获取）
const statusCounts = computed(() => ({
  all: stats.value.total,
  unused: stats.value.unused,
  used: stats.value.used,
  expired: stats.value.expired
}))

// 获取优惠券类型文本
const getCouponTypeText = (type) => {
  const map = { 1: '满减券', 2: '折扣券', 3: '立减券' }
  return map[type] || '优惠券'
}

// 格式化优惠金额显示
const formatDiscount = (coupon) => {
  if (coupon.couponType === 1) return `¥${coupon.discountAmount}`
  if (coupon.couponType === 2) {
    // 折扣率可能是字符串，先转 Number
    const rate = Number(coupon.discountRate)
    if (isNaN(rate) || rate <= 0) return '折扣券'
    return `${(rate * 10).toFixed(0)}折`
  }
  if (coupon.couponType === 3) return `¥${coupon.discountAmount}`
  return ''
}

// 格式化使用条件
const formatCondition = (coupon) => {
  if (coupon.minOrderAmount && coupon.minOrderAmount > 0) {
    return `满${coupon.minOrderAmount}可用`
  }
  return '无门槛'
}

// 获取状态标签样式
const getStatusClass = (status) => {
  const map = {
    0: 'bg-green-50 text-green-700 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800',
    1: 'bg-gray-50 text-gray-500 border-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-700',
    2: 'bg-red-50 text-red-600 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800'
  }
  return map[status] || 'bg-gray-50 text-gray-500 border-gray-200'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = { 0: '未使用', 1: '已使用', 2: '已过期' }
  return map[status] || '未知'
}

// 获取来源文本
const getSourceText = (sourceType) => {
  return sourceType === 1 ? '免费领取' : '积分兑换'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div class="max-w-6xl mx-auto px-6 py-4">
        <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <router-link to="/" class="hover:text-gray-700 dark:hover:text-gray-300">首页</router-link>
          <span>/</span>
          <span class="text-gray-900 dark:text-white">我的优惠券</span>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-6 py-8">
      <!-- 页面标题 -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">我的优惠券</h1>
        <p class="text-gray-500 dark:text-gray-400 mt-1">管理您的优惠券，查看使用状态</p>
      </div>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-4 gap-4 mb-6">
        <div 
          v-for="stat in [
            { key: 'all', label: '全部优惠券', count: statusCounts.all, color: 'text-gray-900 dark:text-white' },
            { key: 'unused', label: '未使用', count: statusCounts.unused, color: 'text-green-600 dark:text-green-400' },
            { key: 'used', label: '已使用', count: statusCounts.used, color: 'text-blue-600 dark:text-blue-400' },
            { key: 'expired', label: '已过期', count: statusCounts.expired, color: 'text-red-600 dark:text-red-400' }
          ]"
          :key="stat.key"
          @click="activeTab = stat.key; current = 1; loadCoupons()"
          class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 cursor-pointer hover:border-red-300 dark:hover:border-red-700 transition-colors"
          :class="{ 'border-red-500 dark:border-red-500 bg-red-50 dark:bg-red-900/10': activeTab === stat.key }"
        >
          <div class="text-3xl font-bold mb-1" :class="stat.color">{{ stat.count }}</div>
          <div class="text-sm text-gray-500 dark:text-gray-400">{{ stat.label }}</div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 py-16 text-center">
        <div class="inline-block w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-gray-500 dark:text-gray-400 mt-3">加载中...</p>
      </div>

      <!-- 优惠券卡片列表 -->
      <div v-else-if="filteredCoupons.length > 0">
        <!-- 卡片网格 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="coupon in filteredCoupons"
            :key="coupon.id"
            class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50 hover:shadow-md hover:border-primary-200 dark:hover:border-primary-700 transition-all duration-300 overflow-hidden flex"
          >
            <!-- 左侧：优惠金额区域 -->
            <div
              class="flex-shrink-0 w-28 flex flex-col items-center justify-center"
              :class="coupon.status === 0
                ? 'bg-gradient-to-br from-red-500 to-red-600'
                : 'bg-gray-200 dark:bg-gray-700'"
            >
              <div class="text-xl font-bold text-white">{{ formatDiscount(coupon) }}</div>
              <div class="text-xs text-white/80 mt-1">{{ formatCondition(coupon) }}</div>
            </div>

            <!-- 右侧：优惠券详情 -->
            <div class="flex-1 min-w-0 p-4 flex flex-col justify-between">
              <div>
                <!-- 优惠券名称 -->
                <h3 class="font-medium text-gray-900 dark:text-white truncate"
                  :class="{ 'text-gray-400 dark:text-gray-500': coupon.status !== 0 }"
                >
                  {{ coupon.couponName }}
                </h3>
                <!-- 描述 -->
                <p class="text-sm text-gray-500 dark:text-gray-400 mt-1 line-clamp-1">{{ coupon.description }}</p>
                <!-- 类型标签 + 来源 -->
                <div class="flex items-center gap-2 mt-2">
                  <span class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300">
                    {{ getCouponTypeText(coupon.couponType) }}
                  </span>
                  <span class="text-xs text-gray-400 dark:text-gray-500">{{ getSourceText(coupon.sourceType) }}</span>
                </div>
              </div>
              <!-- 有效期 + 操作 -->
              <div class="flex items-center justify-between mt-3">
                <div class="text-xs text-gray-400 dark:text-gray-500">
                  有效期至 {{ formatDate(coupon.expireTime) }}
                </div>
                <div>
                  <span
                    v-if="coupon.status !== 0"
                    class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium border"
                    :class="getStatusClass(coupon.status)"
                  >
                    {{ getStatusText(coupon.status) }}
                  </span>
                  <button
                    v-else
                    class="px-3 py-1 text-xs font-medium text-white bg-red-500 hover:bg-red-600 rounded-md transition-colors"
                  >
                    立即使用
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="flex justify-center mt-6">
          <n-pagination
            v-model:page="current"
            :page-size="pageSize"
            :item-count="total"
            @update:page="loadCoupons"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 py-16 text-center">
        <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
        </svg>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-1">暂无优惠券</h3>
        <p class="text-gray-500 dark:text-gray-400 mb-4">
          {{ activeTab === 'unused' ? '您还没有未使用的优惠券' : activeTab === 'used' ? '您还没有使用过的优惠券' : '您没有已过期的优惠券' }}
        </p>
        <button 
          @click="router.push('/coupon-zone')"
          class="px-4 py-2 text-sm font-medium text-white bg-red-500 hover:bg-red-600 rounded-md transition-colors"
        >
          去领取优惠券
        </button>
      </div>
    </div>
  </div>
</template>
