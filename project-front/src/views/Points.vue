<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {NButton, NIcon, NSpin} from 'naive-ui'
import {pointsApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatNumber} from '@/utils/format'
import {
  CalendarOutline,
  ChevronForwardOutline,
  GiftOutline,
  ListOutline,
  StarOutline,
  TrendingUpOutline,
  TrophyOutline
} from '@vicons/ionicons5'

const router = useRouter()
const loading = ref(true)
const signing = ref(false)

const balance = ref({
  totalPoints: 0,
  availablePoints: 0,
  todayEarned: 0,
  rank: 0
})

const signStatus = ref({
  signed: false,
  continuousDays: 0,
  totalSignDays: 0,
  signCalendar: []
})

const daysInMonth = ref([])
const currentDay = new Date().getDate()

const signRewards = [
  { days: 3, points: 10 },
  { days: 7, points: 30 },
  { days: 14, points: 60 },
  { days: 30, points: 150 }
]

const initDaysInMonth = () => {
  const year = new Date().getFullYear()
  const month = new Date().getMonth()
  const days = new Date(year, month + 1, 0).getDate()
  daysInMonth.value = Array.from({ length: days }, (_, i) => i + 1)
}

const fetchBalance = async () => {
  try {
    const data = await pointsApi.getBalance()
    balance.value = data
  } catch (e) {
    toast.error('获取积分信息失败')
  }
}

const fetchSignStatus = async () => {
  try {
    const data = await pointsApi.getSignStatus()
    signStatus.value = data
  } catch (e) {
    toast.error('获取签到状态失败')
  }
}

const handleSign = async () => {
  if (signStatus.value.signed || signing.value) return
  
  signing.value = true
  try {
    const result = await pointsApi.doSign()
    const msg = result.extraPoints > 0
      ? `签到成功，连续签到${result.continuousDays}天，额外奖励${result.extraPoints}积分`
      : `签到成功，获得${result.points}积分`
    toast.success(msg)
    signStatus.value.signed = true
    signStatus.value.continuousDays = result.continuousDays
    signStatus.value.totalSignDays = result.totalSignDays
    signStatus.value.signCalendar.push(currentDay)
    balance.value.todayEarned += result.points
    balance.value.totalPoints += result.points
    balance.value.availablePoints += result.points
  } catch (e) {
    toast.error(e.message || '签到失败，请重试')
  } finally {
    signing.value = false
  }
}

const isSigned = (day) => {
  return signStatus.value.signCalendar.includes(day)
}

onMounted(async () => {
  initDaysInMonth()
  await Promise.all([fetchBalance(), fetchSignStatus()])
  loading.value = false
})
</script>

<template>
  <div class="h-screen bg-gray-50 dark:bg-gray-900 flex flex-col overflow-hidden">
    <!-- 顶部渐变背景 -->
    <div class="bg-gradient-to-r from-primary-500 via-primary-600 to-primary-700 relative overflow-hidden flex-shrink-0">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="max-w-7xl mx-auto px-6 py-4 relative z-10">
        <h1 class="text-lg font-bold text-white mb-2">我的积分</h1>
        <div class="bg-white/20 backdrop-blur-md rounded-xl p-4 text-white">
          <div class="grid grid-cols-4 gap-4">
            <div class="border-r border-white/20 pr-4">
              <p class="text-xs text-white/70">累计积分</p>
              <p class="text-2xl font-bold">{{ formatNumber(balance.totalPoints) }}</p>
            </div>
            <div class="border-r border-white/20 pr-4">
              <p class="text-xs text-white/70">可用积分</p>
              <p class="text-2xl font-bold">{{ formatNumber(balance.availablePoints) }}</p>
            </div>
            <div class="border-r border-white/20 pr-4">
              <p class="text-xs text-white/70">今日获得</p>
              <p class="text-2xl font-bold text-green-300">+{{ balance.todayEarned }}</p>
            </div>
            <div>
              <p class="text-xs text-white/70">当前排名</p>
              <p class="text-2xl font-bold text-yellow-300">{{ balance.rank }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 max-w-7xl mx-auto w-full px-6 py-4 overflow-hidden">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center items-center h-full">
        <n-spin size="large" />
      </div>

      <template v-else>
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 h-full">
          <!-- 左侧：签到和规则 -->
          <div class="lg:col-span-2 space-y-4">
            <!-- 签到卡片 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50">
              <div class="p-4">
                <div class="flex items-center justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center">
                      <n-icon :component="CalendarOutline" :size="16" class="text-primary-500" />
                    </div>
                    <h2 class="text-base font-semibold text-gray-900 dark:text-white">每日签到</h2>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="text-center">
                      <p class="text-base font-bold text-primary-500">{{ signStatus.totalSignDays }}</p>
                      <p class="text-[10px] text-gray-500 dark:text-gray-400">本月签到</p>
                    </div>
                    <div class="w-px h-6 bg-gray-200 dark:bg-gray-700"></div>
                    <div class="text-center">
                      <p class="text-base font-bold text-orange-500">{{ signStatus.continuousDays }}</p>
                      <p class="text-[10px] text-gray-500 dark:text-gray-400">连续签到</p>
                    </div>
                  </div>
                </div>

                <div class="grid grid-cols-7 gap-1 mb-3">
                  <div v-for="day in daysInMonth" :key="day"
                    class="h-7 flex items-center justify-center rounded-lg text-xs transition-all cursor-default"
                    :class="{
                      'bg-primary-500 text-white': isSigned(day),
                      'bg-gray-100 dark:bg-gray-700 text-gray-400': !isSigned(day) && day < currentDay,
                      'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400 border border-primary-300 dark:border-primary-600': day === currentDay && !isSigned(day),
                      'text-gray-300 dark:text-gray-600': day > currentDay
                    }">
                    {{ day }}
                  </div>
                </div>

                <n-button
                  type="primary"
                  block
                  size="small"
                  :disabled="signStatus.signed || signing"
                  :loading="signing"
                  @click="handleSign"
                >
                  {{ signStatus.signed ? '✓ 今日已签到' : '立即签到 +5积分' }}
                </n-button>
              </div>
            </div>

            <!-- 积分规则 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50">
              <div class="p-4">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
                    <n-icon :component="StarOutline" :size="16" class="text-blue-500" />
                  </div>
                  <h2 class="text-base font-semibold text-gray-900 dark:text-white">积分规则</h2>
                </div>
                <div class="grid grid-cols-4 gap-3">
                  <div class="flex flex-col items-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                    <div class="w-10 h-10 bg-green-100 dark:bg-green-900/30 rounded-xl flex items-center justify-center mb-2">
                      <n-icon :component="TrendingUpOutline" :size="18" class="text-green-500" />
                    </div>
                    <p class="text-xs font-medium text-gray-900 dark:text-white">每日签到</p>
                    <p class="text-[10px] text-primary-500 font-bold">5积分</p>
                  </div>
                  <div class="flex flex-col items-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                    <div class="w-10 h-10 bg-purple-100 dark:bg-purple-900/30 rounded-xl flex items-center justify-center mb-2">
                      <n-icon :component="TrendingUpOutline" :size="18" class="text-purple-500" />
                    </div>
                    <p class="text-xs font-medium text-gray-900 dark:text-white">发布文章</p>
                    <p class="text-[10px] text-primary-500 font-bold">20积分</p>
                  </div>
                  <div class="flex flex-col items-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                    <div class="w-10 h-10 bg-yellow-100 dark:bg-yellow-900/30 rounded-xl flex items-center justify-center mb-2">
                      <n-icon :component="TrendingUpOutline" :size="18" class="text-yellow-500" />
                    </div>
                    <p class="text-xs font-medium text-gray-900 dark:text-white">被点赞</p>
                    <p class="text-[10px] text-primary-500 font-bold">2积分</p>
                  </div>
                  <div class="flex flex-col items-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                    <div class="w-10 h-10 bg-pink-100 dark:bg-pink-900/30 rounded-xl flex items-center justify-center mb-2">
                      <n-icon :component="TrendingUpOutline" :size="18" class="text-pink-500" />
                    </div>
                    <p class="text-xs font-medium text-gray-900 dark:text-white">被收藏</p>
                    <p class="text-[10px] text-primary-500 font-bold">3积分</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：快捷入口和奖励 -->
          <div class="space-y-4">
            <!-- 快捷入口 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50">
              <div class="p-4">
                <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">快捷入口</h3>
                <div class="space-y-2">
                  <div class="flex items-center gap-3 p-3 bg-yellow-50 dark:bg-yellow-900/20 rounded-xl cursor-pointer hover:bg-yellow-100 dark:hover:bg-yellow-900/30 transition-colors"
                       @click="router.push('/points/rank')">
                    <div class="w-10 h-10 bg-yellow-100 dark:bg-yellow-900/30 rounded-xl flex items-center justify-center">
                      <n-icon :component="TrophyOutline" :size="20" class="text-yellow-500" />
                    </div>
                    <div class="flex-1">
                      <p class="text-sm font-medium text-gray-900 dark:text-white">积分排行榜</p>
                      <p class="text-[11px] text-gray-500 dark:text-gray-400">查看本月积分排名</p>
                    </div>
                    <n-icon :component="ChevronForwardOutline" :size="16" class="text-gray-400" />
                  </div>
                  <div class="flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-xl cursor-pointer hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors"
                       @click="router.push('/points/logs')">
                    <div class="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-xl flex items-center justify-center">
                      <n-icon :component="ListOutline" :size="20" class="text-blue-500" />
                    </div>
                    <div class="flex-1">
                      <p class="text-sm font-medium text-gray-900 dark:text-white">积分流水</p>
                      <p class="text-[11px] text-gray-500 dark:text-gray-400">查看积分变动记录</p>
                    </div>
                    <n-icon :component="ChevronForwardOutline" :size="16" class="text-gray-400" />
                  </div>
                </div>
              </div>
            </div>

            <!-- 连续签到奖励 -->
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700/50">
              <div class="p-4">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 bg-orange-100 dark:bg-orange-900/30 rounded-lg flex items-center justify-center">
                    <n-icon :component="GiftOutline" :size="16" class="text-orange-500" />
                  </div>
                  <h3 class="text-sm font-semibold text-gray-900 dark:text-white">连续签到奖励</h3>
                </div>
                <div class="space-y-2">
                  <div v-for="reward in signRewards" :key="reward.days"
                    class="flex items-center justify-between px-3 py-2 rounded-lg"
                    :class="signStatus.continuousDays >= reward.days ? 'bg-green-50 dark:bg-green-900/20' : 'bg-gray-50 dark:bg-gray-700/50'">
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold"
                        :class="signStatus.continuousDays >= reward.days ? 'bg-green-500 text-white' : 'bg-gray-200 dark:bg-gray-600 text-gray-500 dark:text-gray-400'">
                        {{ reward.days }}
                      </div>
                      <span class="text-xs" :class="signStatus.continuousDays >= reward.days ? 'text-green-700 dark:text-green-400' : 'text-gray-600 dark:text-gray-400'">
                        连续{{ reward.days }}天
                      </span>
                    </div>
                    <span class="text-xs font-bold" :class="signStatus.continuousDays >= reward.days ? 'text-green-600 dark:text-green-400' : 'text-gray-400 dark:text-gray-500'">
                      +{{ reward.points }}积分
                    </span>
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

<style scoped>
</style>
