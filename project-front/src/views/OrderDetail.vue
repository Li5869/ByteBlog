<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {toast} from '@/utils/toast'
import {vipApi} from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(true)
const confirming = ref(false)
const cancelling = ref(false)

const orderId = computed(() => route.params.id)

// 订单数据（初始为 null，由接口返回后赋值）
const order = ref(null)
// 接口请求错误信息
const errorMsg = ref('')

// 状态映射
const statusMap = {
  0: { text: '待确认', class: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', desc: '请在15分钟内确认购买，超时订单将自动关闭' },
  1: { text: '已冻结', class: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', desc: '积分已冻结，正在处理中...' },
  2: { text: '已完成', class: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', desc: 'VIP会员已开通成功' },
  3: { text: '已取消', class: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400', desc: '订单已取消' },
  4: { text: '已关闭', class: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400', desc: '订单已超时关闭' }
}

// 订单时间线（需防御 order 为 null 的情况）
const timeline = computed(() => {
  if (!order.value) return []
  const items = [
    { time: order.value.createdAt, text: '创建订单', done: true }
  ]
  if (order.value.status === 2) {
    items.push({ time: order.value.updatedAt, text: 'VIP会员开通成功', done: true })
  }
  if (order.value.status === 3) {
    items.push({ time: order.value.updatedAt, text: '订单已取消', done: true })
  }
  if (order.value.status === 4) {
    items.push({ time: order.value.updatedAt, text: '订单超时关闭', done: true })
  }
  return items
})

const isPending = computed(() => order.value?.status === 0)
const isFrozen = computed(() => order.value?.status === 1)

// 订单取消倒计时
const countdown = ref(0)
let countdownTimer = null

// 格式化倒计时为 MM:SS
const countdownText = computed(() => {
  if (countdown.value <= 0) return '00:00'
  const minutes = Math.floor(countdown.value / 60)
  const seconds = countdown.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

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

// 页面挂载后请求真实订单数据
onMounted(async () => {
  try {
    const res = await vipApi.getOrder(route.params.id)
    order.value = res

    // 启动订单取消倒计时（仅待确认状态）
    if (res?.expireTime && res.status === 0) {
      startCountdown(res.expireTime)
    }
  } catch (e) {
    errorMsg.value = e.message || '订单加载失败'
    toast.error(errorMsg.value)
  } finally {
    loading.value = false
  }
})

// 组件卸载时清除定时器
onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

// 确认购买 → 调用后端 confirmOrder
const handleConfirm = async () => {
  confirming.value = true
  try {
    await vipApi.confirmOrder(orderId.value)
    // 刷新订单数据，获取最新状态
    const res = await vipApi.getOrder(orderId.value)
    order.value = res
    toast.success('确认购买成功，积分已冻结')
  } catch (e) {
    console.error('确认购买失败:', e)
    toast.error(e?.msg || '确认失败，请重试')
  } finally {
    confirming.value = false
  }
}

// 取消订单（后端写接口未实现，暂用 setTimeout 模拟）
const handleCancel = async () => {
  cancelling.value = true
  setTimeout(() => {
    order.value.status = 3
    order.value.tccStatus = 2
    order.value.cancelReason = '用户主动取消'
    order.value.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    cancelling.value = false
    toast.success('订单已取消')
  }, 1000)
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10">
      <div class="max-w-4xl mx-auto px-6 h-14 flex items-center justify-between">
        <button @click="router.push('/mine/orders')" class="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          <span class="text-sm font-medium">返回订单列表</span>
        </button>
        <h1 class="text-sm font-semibold text-gray-900 dark:text-white">订单详情</h1>
        <div class="w-24"></div>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-6 py-8">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-32">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-500"></div>
      </div>

      <!-- 加载失败提示 -->
      <div v-else-if="errorMsg" class="flex flex-col items-center justify-center py-32">
        <svg class="w-16 h-16 text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-gray-500 dark:text-gray-400 mb-4">{{ errorMsg }}</p>
        <button @click="router.push('/mine/orders')" class="px-4 py-2 text-sm font-medium text-amber-600 bg-amber-50 dark:bg-amber-900/20 rounded-lg hover:bg-amber-100 dark:hover:bg-amber-900/40 transition-colors">
          返回订单列表
        </button>
      </div>

      <template v-else>
        <!-- 订单状态头部 -->
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden mb-6">
          <div class="p-6 flex items-center justify-between">
            <div class="flex items-center gap-4">
              <div class="w-14 h-14 rounded-2xl flex items-center justify-center"
                :class="order.status === 2 ? 'bg-gradient-to-br from-green-400 to-emerald-500' : order.status === 0 ? 'bg-gradient-to-br from-yellow-400 to-amber-500' : 'bg-gray-200 dark:bg-gray-600'">
                <svg v-if="order.status === 2" class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
                </svg>
                <svg v-else-if="order.status === 0" class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <svg v-else class="w-7 h-7 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="px-2.5 py-0.5 text-xs font-semibold rounded-full" :class="statusMap[order.status]?.class">
                    {{ statusMap[order.status]?.text }}
                  </span>
                </div>
                <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ statusMap[order.status]?.desc }}</p>
                <!-- 待确认状态显示倒计时 -->
                <div v-if="isPending && order.expireTime" class="mt-2">
                  <div v-if="countdown > 0" class="flex items-center gap-2">
                    <svg class="w-4 h-4" :class="countdown < 120 ? 'text-red-500' : 'text-amber-500'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span class="text-sm font-medium" :class="countdown < 120 ? 'text-red-600 dark:text-red-400' : 'text-amber-600 dark:text-amber-400'">
                      剩余 <span class="font-mono text-base">{{ countdownText }}</span> 自动关闭
                    </span>
                  </div>
                  <div v-else class="flex items-center gap-2 text-red-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                    </svg>
                    <span class="text-sm font-medium">订单已超时</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 操作按钮 -->
            <div v-if="isPending" class="flex gap-3">
              <button
                @click="handleCancel"
                :disabled="cancelling"
                class="px-5 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors disabled:opacity-50"
              >
                {{ cancelling ? '取消中...' : '取消订单' }}
              </button>
              <button
                @click="handleConfirm"
                :disabled="confirming"
                class="px-5 py-2 text-sm font-bold text-white bg-gradient-to-r from-amber-500 to-yellow-500 rounded-lg hover:from-amber-600 hover:to-yellow-600 transition-all shadow-lg shadow-amber-500/25 disabled:opacity-50 flex items-center gap-2"
              >
                <svg v-if="confirming" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ confirming ? '确认中...' : '确认购买' }}
              </button>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-12 gap-6">
          <!-- 左栏：订单信息 -->
          <div class="col-span-8 space-y-5">
            <!-- 套餐信息 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">套餐信息</h2>
              </div>
              <div class="p-6">
                <div class="flex items-start gap-5">
                  <div class="w-16 h-16 bg-gradient-to-br from-amber-400 to-yellow-500 rounded-2xl flex items-center justify-center shadow-lg shadow-amber-500/20 flex-shrink-0">
                    <svg class="w-8 h-8 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                    </svg>
                  </div>
                  <div class="flex-1">
                    <h3 class="text-lg font-bold text-gray-900 dark:text-white">VIP {{ order.bizSnapshot.planName }}</h3>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">会员时长：{{ order.bizSnapshot.durationMonths }}个月</p>
                    <div class="mt-3 flex items-center gap-4">
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                        专属标识
                      </div>
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                        内容特权
                      </div>
                      <div class="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                        双倍积分
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 订单信息 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">订单信息</h2>
              </div>
              <div class="p-6 space-y-3">
                <div class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">订单号</span>
                  <span class="text-sm font-mono text-gray-900 dark:text-white">{{ order.orderNo }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">业务类型</span>
                  <span class="text-sm text-gray-900 dark:text-white">VIP会员</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">优惠券</span>
                  <span class="text-sm text-gray-900 dark:text-white">{{ order.couponName || '未使用' }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">创建时间</span>
                  <span class="text-sm text-gray-900 dark:text-white">{{ order.createdAt }}</span>
                </div>
                <div v-if="order.expireTime" class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">过期时间</span>
                  <span class="text-sm text-gray-900 dark:text-white">{{ order.expireTime }}</span>
                </div>
                <div v-if="order.cancelReason" class="flex justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">取消原因</span>
                  <span class="text-sm text-red-500">{{ order.cancelReason }}</span>
                </div>
              </div>
            </div>

            <!-- 订单时间线 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">订单时间线</h2>
              </div>
              <div class="p-6">
                <div class="space-y-4">
                  <div v-for="(item, index) in timeline" :key="index" class="flex gap-4">
                    <div class="flex flex-col items-center">
                      <div class="w-3 h-3 rounded-full flex-shrink-0"
                        :class="item.done ? 'bg-amber-500' : 'bg-gray-300 dark:bg-gray-600'"></div>
                      <div v-if="index < timeline.length - 1" class="w-0.5 flex-1 bg-gray-200 dark:bg-gray-600 mt-1"></div>
                    </div>
                    <div class="pb-4">
                      <p class="text-sm font-medium text-gray-900 dark:text-white">{{ item.text }}</p>
                      <p class="text-xs text-gray-400 mt-0.5">{{ item.time }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右栏：费用明细 -->
          <div class="col-span-4">
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden sticky top-20">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-base font-semibold text-gray-900 dark:text-white">费用明细</h2>
              </div>
              <div class="p-6 space-y-4">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">原价</span>
                  <span class="text-sm text-gray-500 dark:text-gray-400 line-through">{{ order.pointsCost }} 积分</span>
                </div>
                <div v-if="order.couponDiscount > 0" class="flex items-center justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">{{ order.couponName }}</span>
                  <span class="text-sm text-green-600 dark:text-green-400">-{{ order.couponDiscount }} 积分</span>
                </div>
                <div class="border-t border-gray-100 dark:border-gray-700 pt-4">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-gray-900 dark:text-white">实付积分</span>
                    <div class="flex items-baseline gap-1">
                      <span class="text-2xl font-bold text-amber-600 dark:text-amber-400">{{ order.actualPoints }}</span>
                      <span class="text-xs text-gray-500 dark:text-gray-400">积分</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
