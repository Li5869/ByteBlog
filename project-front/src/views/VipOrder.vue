<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {toast} from '@/utils/toast'
import {pointsApi, vipApi} from '@/utils/request'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const paying = ref(false)
const paySuccess = ref(false)

// 订单取消倒计时
const countdown = ref(0)
let countdownTimer = null

// 用户积分（从路由参数读取）
const userPoints = ref(Number(route.query.points) || 0)

// 订单信息（从后端创建预订单接口获取）
const order = ref(null)
// 可用优惠券列表
const availableCoupons = ref([])
// 当前选中的优惠券 ID（初始为 null，加载订单后根据后端返回设置）
const selectedCouponId = ref(null)

// 从订单快照中解析套餐信息
const planInfo = computed(() => {
  if (!order.value?.bizSnapshot) return null
  try {
    return typeof order.value.bizSnapshot === 'string' ? JSON.parse(order.value.bizSnapshot) : order.value.bizSnapshot
  } catch { return null }
})

// 格式化倒计时为 MM:SS
const countdownText = computed(() => {
  if (countdown.value <= 0) return '00:00'
  const minutes = Math.floor(countdown.value / 60)
  const seconds = countdown.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// 倒计时是否已过期
const isExpired = computed(() => countdown.value <= 0 && order.value?.expireTime)

// 启动倒计时
const startCountdown = (expireTime) => {
  if (!expireTime) return
  const expireTs = new Date(expireTime).getTime()
  const now = Date.now()
  countdown.value = Math.max(0, Math.floor((expireTs - now) / 1000))
  countdownTimer = setInterval(() => {
    countdown.value = Math.max(0, countdown.value - 1)
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

// 当前选中的优惠券对象
const selectedCoupon = computed(() => {
  if (!selectedCouponId.value) return null
  return availableCoupons.value.find(c => c.id === selectedCouponId.value) || null
})

// 优惠金额（以后端返回的订单数据为准，切换优惠券时实时预览）
const couponDiscount = computed(() => {
  if (selectedCoupon.value) {
    // 前端实时计算预览
    const c = selectedCoupon.value
    const price = order.value?.pointsCost || 0
    if (c.couponType === 1 || c.couponType === 3) return c.discountAmount
    if (c.couponType === 2) return Math.round(price * (1 - c.discountRate))
  }
  // 默认使用后端计算的值
  return order.value?.couponDiscount || 0
})

// 最终价格
const finalPrice = computed(() => {
  return Math.max(0, (order.value?.pointsCost || 0) - couponDiscount.value)
})

const pointsEnough = computed(() => userPoints.value >= finalPrice.value)

// 优惠券下拉框
const couponDropdownOpen = ref(false)
const couponDropdownRef = ref(null)

const couponDiscountText = computed(() => {
  if (!selectedCoupon.value) return ''
  const c = selectedCoupon.value
  return c.couponType === 1 || c.couponType === 3 ? `减${c.discountAmount}` : `${c.discountRate * 10}折`
})

// 切换优惠券 → 调用后端更新订单
const selectCoupon = async (couponId) => {
  selectedCouponId.value = couponId
  couponDropdownOpen.value = false
  try {
    const updated = await vipApi.updateOrderCoupon(route.query.orderId, { couponId: couponId || null })
    order.value = updated
  } catch (e) {
    console.error('更新优惠券失败:', e)
    toast.error('更新优惠券失败')
  }
}

// 点击外部关闭下拉
const handleClickOutside = (e) => {
  if (couponDropdownRef.value && !couponDropdownRef.value.contains(e.target)) {
    couponDropdownOpen.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  const orderId = route.query.orderId
  if (!orderId) {
    toast.error('订单参数缺失，请重新选择套餐')
    router.push('/mine')
    return
  }
  try {
    // 并行获取订单详情、积分余额、可用优惠券
    const [orderRes, balanceRes] = await Promise.all([
      vipApi.getOrder(orderId),
      pointsApi.getBalance()
    ])
    order.value = orderRes
    userPoints.value = balanceRes?.availablePoints ?? 0

    // 启动订单取消倒计时
    if (orderRes?.expireTime) {
      startCountdown(orderRes.expireTime)
    }

    // 加载可用优惠券（用套餐价格筛选）
    if (orderRes?.pointsCost) {
      const couponsRes = await vipApi.getAvailableCoupons(orderRes.pointsCost)
      availableCoupons.value = couponsRes || []
      // 设置后端自动匹配的优惠券为默认选中（用 couponId 精确匹配）
      if (orderRes.couponId) {
        const matched = availableCoupons.value.find(c => String(c.id) === String(orderRes.couponId))
        if (matched) selectedCouponId.value = matched.id
      }
    }
  } catch (e) {
    console.error('加载订单数据失败:', e)
    toast.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  if (countdownTimer) clearInterval(countdownTimer)
})

// 确认支付 → 调用后端 confirmOrder
const handlePay = async () => {
  if (!pointsEnough.value) {
    toast.error('积分余额不足')
    return
  }
  paying.value = true
  try {
    await vipApi.confirmOrder(route.query.orderId)
    paySuccess.value = true
    toast.success('支付成功！VIP已开通')
  } catch (e) {
    console.error('支付失败:', e)
    toast.error(e?.msg || '支付失败，请重试')
  } finally {
    paying.value = false
  }
}

const handleBack = () => {
  router.push('/mine')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10">
      <div class="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        <button @click="handleBack" class="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          <span class="text-sm font-medium">返回</span>
        </button>
        <h1 class="text-sm font-semibold text-gray-900 dark:text-white">确认订单</h1>
        <div class="w-16"></div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="max-w-6xl mx-auto px-6 py-8">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-32">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-500"></div>
      </div>

      <!-- 支付成功 -->
      <div v-else-if="paySuccess" class="max-w-md mx-auto text-center py-24">
        <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-green-400 to-emerald-500 rounded-full flex items-center justify-center shadow-lg shadow-green-500/30">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">支付成功</h2>
        <p class="text-gray-500 dark:text-gray-400 mb-8">恭喜你成为 VIP 会员！</p>
        <div class="flex justify-center gap-3">
          <button @click="router.push('/mine/orders')" class="px-6 py-2.5 text-sm font-medium text-gray-600 dark:text-gray-400 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
            查看订单
          </button>
          <button @click="router.push('/')" class="px-6 py-2.5 text-sm font-bold text-white bg-gradient-to-r from-amber-500 to-yellow-500 rounded-lg hover:from-amber-600 hover:to-yellow-600 transition-all shadow-lg shadow-amber-500/25">
            去首页看看
          </button>
        </div>
      </div>

      <!-- 订单内容 - PC 双栏布局 -->
      <template v-else>
        <!-- 订单取消倒计时提示条 -->
        <div v-if="order?.expireTime && !paySuccess" class="mb-6">
          <div v-if="!isExpired" class="flex items-center justify-center gap-3 py-3 px-4 rounded-xl border"
               :class="countdown < 120
                 ? 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
                 : 'bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-800'">
            <svg class="w-5 h-5" :class="countdown < 120 ? 'text-red-500' : 'text-amber-500'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm" :class="countdown < 120 ? 'text-red-600 dark:text-red-400' : 'text-amber-600 dark:text-amber-400'">
              订单将在
              <span class="font-bold font-mono text-base mx-0.5">{{ countdownText }}</span>
              后自动取消
            </span>
          </div>
          <!-- 已过期提示 -->
          <div v-else class="flex items-center justify-center gap-3 py-3 px-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
            <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <span class="text-sm text-red-600 dark:text-red-400 font-medium">订单已过期，请重新下单</span>
          </div>
        </div>

        <div class="grid grid-cols-12 gap-6">
          <!-- 左栏：订单详情 -->
          <div class="col-span-8 space-y-5">
            <!-- 订单信息卡片 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">订单信息</h2>
              </div>
              <div class="p-6">
                <div class="flex items-start gap-6">
                  <div class="w-20 h-20 bg-gradient-to-br from-amber-400 to-yellow-500 rounded-2xl flex items-center justify-center shadow-lg shadow-amber-500/20 flex-shrink-0">
                    <svg class="w-10 h-10 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                    </svg>
                  </div>
                  <div class="flex-1">
                    <h3 class="text-lg font-bold text-gray-900 dark:text-white">VIP {{ planInfo?.planName || '会员' }}</h3>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">会员时长：{{ planInfo?.durationMonths || 0 }}个月</p>
                    <div class="mt-3 flex items-center gap-4">
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        专属标识
                      </div>
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        内容特权
                      </div>
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                        优先体验
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 用户信息卡片 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">账户信息</h2>
              </div>
              <div class="p-6">
                <div class="flex items-center gap-4">
                  <div class="w-12 h-12 rounded-lg bg-gradient-to-br from-amber-400 to-yellow-500 flex items-center justify-center flex-shrink-0">
                    <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="font-medium text-gray-900 dark:text-white">积分余额</p>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">当前可用：<span class="font-semibold text-amber-600 dark:text-amber-400">{{ userPoints }}</span> 积分</p>
                  </div>
                  <span v-if="!pointsEnough" class="px-3 py-1 text-xs font-medium text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400 rounded-full">
                    余额不足
                  </span>
                </div>
              </div>
            </div>

            <!-- 优惠券卡片（去除 overflow-hidden 避免截断下拉面板） -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">优惠券</h2>
                <span v-if="availableCoupons.length > 0" class="text-xs text-gray-400">{{ availableCoupons.length }} 张可用</span>
                <span v-else class="text-xs text-gray-400">暂无可用优惠券</span>
              </div>
              <div class="p-6">
                <!-- 自定义下拉选择器 -->
                <div class="relative" ref="couponDropdownRef">
                  <!-- 选中显示框 -->
                  <button
                    type="button"
                    @click="couponDropdownOpen = !couponDropdownOpen"
                    :disabled="availableCoupons.length === 0"
                    class="w-full flex items-center justify-between px-4 py-3 text-sm bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl transition-all"
                    :class="availableCoupons.length === 0
                      ? 'opacity-50 cursor-not-allowed'
                      : 'cursor-pointer hover:border-amber-400 dark:hover:border-amber-500 ' + (couponDropdownOpen ? 'ring-2 ring-amber-500 border-amber-500' : '')"
                  >
                    <div class="flex items-center gap-2">
                      <svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
                      </svg>
                      <span :class="selectedCoupon ? 'text-gray-900 dark:text-white' : 'text-gray-500 dark:text-gray-400'">
                        {{ selectedCoupon ? `${selectedCoupon.couponName}（${couponDiscountText}）` : availableCoupons.length === 0 ? '暂无可用优惠券' : '不使用优惠券' }}
                      </span>
                    </div>
                    <svg class="w-4 h-4 text-gray-400 transition-transform" :class="{ 'rotate-180': couponDropdownOpen }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>

                  <!-- 下拉选项 -->
                  <div
                    v-show="couponDropdownOpen"
                    class="absolute z-20 w-full mt-1.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-xl shadow-lg overflow-hidden"
                  >
                    <div class="max-h-48 overflow-y-auto">
                      <!-- 不使用 -->
                      <div
                        @click="selectCoupon(null)"
                        class="flex items-center gap-3 px-4 py-2.5 cursor-pointer transition-colors text-sm"
                        :class="!selectedCouponId
                          ? 'bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400'
                          : 'hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'"
                      >
                        <span class="flex-1">不使用优惠券</span>
                        <svg v-if="!selectedCouponId" class="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                      <!-- 优惠券选项 -->
                      <div
                        v-for="coupon in availableCoupons"
                        :key="coupon.id"
                        @click="selectCoupon(coupon.id)"
                        class="flex items-center gap-3 px-4 py-2.5 cursor-pointer transition-colors"
                        :class="selectedCouponId === coupon.id
                          ? 'bg-amber-50 dark:bg-amber-900/20'
                          : 'hover:bg-gray-50 dark:hover:bg-gray-700'"
                      >
                        <!-- 左侧标签 -->
                        <span class="px-1.5 py-0.5 text-[10px] font-bold text-white bg-gradient-to-r from-red-500 to-orange-500 rounded whitespace-nowrap">
                          {{ coupon.couponType === 1 || coupon.couponType === 3 ? `减${coupon.discountAmount}` : `${coupon.discountRate * 10}折` }}
                        </span>
                        <!-- 中间信息 -->
                        <div class="flex-1 min-w-0">
                          <span class="text-sm font-medium text-gray-900 dark:text-white">{{ coupon.couponName }}</span>
                          <p class="text-[11px] text-gray-400 mt-0.5">满{{ coupon.minOrderAmount }}积分可用</p>
                        </div>
                        <!-- 右侧减免 -->
                        <span class="text-sm font-bold text-red-500 dark:text-red-400 whitespace-nowrap">
                          -{{ coupon.couponType === 1 || coupon.couponType === 3 ? coupon.discountAmount : Math.round((order?.pointsCost || 0) * (1 - coupon.discountRate)) }}
                        </span>
                        <!-- 勾选 -->
                        <svg v-if="selectedCouponId === coupon.id" class="w-4 h-4 text-amber-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-if="selectedCoupon" class="text-xs text-gray-400 mt-2.5">
                  有效期至 {{ selectedCoupon.expireTime }}
                </p>
              </div>
            </div>

            <!-- 支付方式卡片 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">支付方式</h2>
              </div>
              <div class="p-6">
                <div class="flex items-center gap-4 p-4 rounded-xl border-2 border-amber-400 bg-amber-50/50 dark:bg-amber-900/10">
                  <div class="w-10 h-10 bg-gradient-to-br from-amber-400 to-yellow-500 rounded-lg flex items-center justify-center">
                    <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                    </svg>
                  </div>
                  <div class="flex-1">
                    <p class="font-medium text-gray-900 dark:text-white">积分支付</p>
                    <p class="text-sm text-gray-500 dark:text-gray-400">使用账户积分直接支付</p>
                  </div>
                  <div class="w-5 h-5 rounded-full border-2 border-amber-500 bg-amber-500 flex items-center justify-center">
                    <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                </div>
                <p v-if="!pointsEnough" class="mt-4 text-sm text-red-500 flex items-center gap-1.5">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                  </svg>
                  积分余额不足，还需 <span class="font-semibold">{{ finalPrice - userPoints }}</span> 积分
                </p>
              </div>
            </div>
          </div>

          <!-- 右栏：支付摘要 -->
          <div class="col-span-4">
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden sticky top-20">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">支付摘要</h2>
              </div>
              <div class="p-6 space-y-4">
                <!-- 套餐信息 -->
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">商品</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">VIP {{ planInfo?.planName || '会员' }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">时长</span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ planInfo?.durationMonths || 0 }}个月</span>
                </div>

                <div class="border-t border-gray-100 dark:border-gray-700 pt-4">
                  <div v-if="couponDiscount > 0" class="flex items-center justify-between mb-1">
                    <span class="text-sm text-gray-500 dark:text-gray-400">优惠券</span>
                    <span class="text-sm text-green-600 dark:text-green-400">-{{ couponDiscount }} 积分</span>
                  </div>
                </div>

                <div class="border-t border-gray-100 dark:border-gray-700 pt-4">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-gray-900 dark:text-white">应付金额</span>
                    <div class="flex items-baseline gap-1">
                      <span class="text-2xl font-bold text-amber-600 dark:text-amber-400">{{ finalPrice }}</span>
                      <span class="text-xs text-gray-500 dark:text-gray-400">积分</span>
                    </div>
                  </div>
                </div>

                <!-- 支付按钮 -->
                <button
                  @click="handlePay"
                  :disabled="paying || !pointsEnough || isExpired"
                  class="w-full py-3 text-sm font-bold text-white bg-gradient-to-r from-amber-500 to-yellow-500 rounded-lg hover:from-amber-600 hover:to-yellow-600 transition-all shadow-lg shadow-amber-500/25 hover:shadow-amber-500/40 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-2"
                >
                  <svg v-if="paying" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isExpired ? '订单已过期' : paying ? '支付中...' : '确认支付' }}
                </button>

                <!-- 提示信息 -->
                <p class="text-xs text-center text-gray-400 dark:text-gray-500">
                  支付即表示同意 <span class="text-amber-500 cursor-pointer hover:underline">会员服务协议</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
