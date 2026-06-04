<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {couponApi, isLoggedIn} from '@/utils/request'
import {toast} from '@/utils/toast'

const router = useRouter()

// 倒计时相关
const countdowns = ref({}) // 存储每个优惠券的倒计时文本
let countdownTimer = null

// 当前筛选：all-全部 free-免费 points-积分兑换
const activeTab = ref('all')

// 优惠券数据及分页
const coupons = ref([])
const loading = ref(false)
const current = ref(1)
const total = ref(0)
const pageSize = ref(20)

// 筛选类型映射
const typeMap = { all: 0, free: 1, points: 2 }

// 加载优惠券列表
const loadCoupons = async () => {
  loading.value = true
  try {
    const data = await couponApi.getZoneList(current.value, pageSize.value, typeMap[activeTab.value])
    coupons.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载优惠券失败', e)
  } finally {
    loading.value = false
  }
}

// 初始加载
loadCoupons()

// 后端已按类型筛选，直接返回
const filteredCoupons = computed(() => coupons.value)

// 判断优惠券是否未到抢券时间
const isNotStarted = (coupon) => {
  if (!coupon.startTime) return false
  return new Date(coupon.startTime).getTime() > Date.now()
}

// 更新所有倒计时
const updateCountdowns = () => {
  const now = Date.now()
  const newCountdowns = {}

  coupons.value.forEach(coupon => {
    if (!coupon.startTime) return
    const start = new Date(coupon.startTime).getTime()
    const diff = start - now

    if (diff <= 0) {
      // 已开始，不显示倒计时
      return
    }

    // 计算天时分秒
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)

    // 格式化倒计时文本
    if (days > 0) {
      newCountdowns[coupon.id] = `${days}天${hours}时${minutes}分${seconds}秒`
    } else {
      newCountdowns[coupon.id] = `${hours}时${minutes}分${seconds}秒`
    }
  })

  countdowns.value = newCountdowns
}

// 启动倒计时定时器
const startCountdown = () => {
  updateCountdowns()
  countdownTimer = setInterval(updateCountdowns, 1000)
}

// 页面挂载后启动倒计时
onMounted(() => {
  startCountdown()
})

// 页面卸载时清除定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

// 格式化优惠金额
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

// 获取优惠券类型文本
const getCouponTypeText = (type) => {
  const map = { 1: '满减券', 2: '折扣券', 3: '立减券' }
  return map[type] || '优惠券'
}

// 格式化使用条件
const formatCondition = (coupon) => {
  if (coupon.minOrderAmount > 0) return `满${coupon.minOrderAmount}可用`
  return '无门槛'
}

// 计算库存百分比（totalCount 为 0 时返回 100 避免除零）
const getStockPercent = (coupon) => {
  if (!coupon.totalCount) return 100
  return Math.round((coupon.stock / coupon.totalCount) * 100)
}

// 获取库存状态颜色
const getStockColor = (coupon) => {
  const percent = getStockPercent(coupon)
  if (percent <= 20) return 'bg-red-500'
  if (percent <= 50) return 'bg-yellow-500'
  return 'bg-green-500'
}

// 格式化有效期显示
const formatValidity = (coupon) => {
  // 固定时间类型：有开始和结束时间
  if (coupon.startTime && coupon.endTime) {
    return formatDate(coupon.endTime)
  }
  // 领取后N天有效
  if (coupon.validDays) {
    return `领取后${coupon.validDays}天有效`
  }
  return '长期有效'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 格式化库存显示
const formatStock = (coupon) => {
  // 不限量时不显示具体数量
  if (coupon.totalCount == null) return '不限量'
  return `${coupon.stock}/${coupon.totalCount}`
}

// 领取优惠券（后端校验积分和库存，前端仅做登录检查）
const claimCoupon = (coupon) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  if (coupon.claimed) {
    toast.warning('您已领取过该优惠券')
    return
  }
  if (coupon.stock <= 0) {
    toast.error('库存不足')
    return
  }
  // TODO: 对接领取接口后替换
  toast.success('领取成功！')
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
          <span class="text-gray-900 dark:text-white">优惠券专区</span>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-6 py-8">
      <!-- 优惠券统计栏 -->
      <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-6">
            <div>
              <div class="text-sm text-gray-500 dark:text-gray-400 mb-1">可领取优惠券</div>
              <div class="text-3xl font-bold text-red-500">{{ total }}</div>
            </div>
          </div>
          <button
            @click="router.push('/points')"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-md transition-colors"
          >
            积分中心
          </button>
        </div>
      </div>

      <!-- 筛选标签 -->
      <div class="flex items-center gap-4 mb-6">
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">筛选：</span>
        <div class="flex gap-2">
          <button 
            v-for="tab in [
              { key: 'all', label: '全部' },
              { key: 'free', label: '免费领取' },
              { key: 'points', label: '积分兑换' }
            ]"
            :key="tab.key"
            @click="activeTab = tab.key; current = 1; loadCoupons()"
            class="px-4 py-2 text-sm font-medium rounded-md transition-colors"
            :class="activeTab === tab.key 
              ? 'bg-red-500 text-white' 
              : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-750'"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 py-16 text-center">
        <div class="inline-block w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-gray-500 dark:text-gray-400 mt-3">加载中...</p>
      </div>

      <!-- 优惠券卡片网格 -->
      <div v-else-if="filteredCoupons.length > 0">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <!-- 单张优惠券卡片 -->
          <div
            v-for="coupon in filteredCoupons"
            :key="coupon.id"
            class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50 hover:shadow-md hover:border-primary-200 dark:hover:border-primary-700 transition-all overflow-hidden flex"
          >
            <!-- 左侧：优惠金额区域（渐变背景） -->
            <div
              class="flex flex-col items-center justify-center px-4 py-6 flex-shrink-0"
              :class="coupon.claimed
                ? 'bg-gray-200 dark:bg-gray-700'
                : 'bg-gradient-to-br from-red-500 to-red-600'"
            >
              <div class="text-xl font-bold text-white">{{ formatDiscount(coupon) }}</div>
              <div class="text-xs text-white/80 mt-1">{{ formatCondition(coupon) }}</div>
            </div>

            <!-- 右侧：优惠券详情 -->
            <div class="flex-1 min-w-0 p-4 flex flex-col justify-between">
              <!-- 名称 & 类型标签 -->
              <div>
                <div class="flex items-start justify-between gap-2">
                  <h3
                    class="font-medium text-gray-900 dark:text-white truncate"
                    :class="{ 'text-gray-400 dark:text-gray-500': coupon.claimed }"
                  >
                    {{ coupon.couponName }}
                  </h3>
                  <span class="flex-shrink-0 inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300">
                    {{ getCouponTypeText(coupon.couponType) }}
                  </span>
                </div>
                <!-- 使用条件 -->
                <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                  {{ coupon.pointsCost > 0 ? `${coupon.pointsCost}积分兑换` : '免费领取' }}
                </p>
              </div>

              <!-- 有效期 & 库存 & 操作 -->
              <div class="mt-3 space-y-2">
                <!-- 有效期 -->
                <div class="flex items-center gap-1 text-xs text-gray-400 dark:text-gray-500">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>{{ formatValidity(coupon) }}</span>
                </div>
                <!-- 库存信息：限量时显示进度条，不限量时只显示文字 -->
                <div v-if="coupon.totalCount != null" class="flex items-center gap-2">
                  <div class="flex-1 h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all"
                      :class="getStockColor(coupon)"
                      :style="{ width: `${getStockPercent(coupon)}%` }"
                    ></div>
                  </div>
                  <span class="flex-shrink-0 text-xs text-gray-500 dark:text-gray-400">
                    库存 {{ formatStock(coupon) }}
                  </span>
                </div>
                <div v-else class="text-xs text-gray-400 dark:text-gray-500">
                  {{ formatStock(coupon) }}
                </div>
                <!-- 倒计时/领取按钮 -->
                <div v-if="isNotStarted(coupon) && countdowns[coupon.id]" class="w-full px-3 py-1.5 text-sm font-medium rounded-lg bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400 text-center">
                  距开始：{{ countdowns[coupon.id] }}
                </div>
                <button
                  v-else
                  @click="claimCoupon(coupon)"
                  :disabled="coupon.claimed"
                  class="w-full px-3 py-1.5 text-sm font-medium rounded-lg transition-colors"
                  :class="coupon.claimed
                    ? 'bg-gray-100 dark:bg-gray-700 text-gray-400 dark:text-gray-500 cursor-not-allowed'
                    : coupon.pointsCost > 0
                      ? 'bg-purple-500 text-white hover:bg-purple-600'
                      : 'bg-red-500 text-white hover:bg-red-600'"
                >
                  {{ coupon.claimed ? '已领取' : '立即领取' }}
                </button>
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
        <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 6v.75m0 3v.75m0 3v.75m0 3V18m-9-5.25h5.25M7.5 15h3M3.375 5.25c-.621 0-1.125.504-1.125 1.125v3.026a2.999 2.999 0 010 5.198v3.026c0 .621.504 1.125 1.125 1.125h17.25c.621 0 1.125-.504 1.125-1.125v-3.026a2.999 2.999 0 010-5.198V6.375c0-.621-.504-1.125-1.125-1.125H3.375z" />
        </svg>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-1">暂无优惠券</h3>
        <p class="text-gray-500 dark:text-gray-400">该分类下暂无可用优惠券</p>
      </div>
    </div>
  </div>
</template>
