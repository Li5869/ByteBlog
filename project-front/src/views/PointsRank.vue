<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {NAvatar, NButton, NIcon, NSpin} from 'naive-ui'
import {pointsApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatNumber} from '@/utils/format'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import {ArrowBackOutline, MedalOutline, TrophyOutline} from '@vicons/ionicons5'

const router = useRouter()
const loading = ref(true)

// 排行榜数据
const rankData = ref({
  yearMonth: '',
  totalUsers: 0,
  myRank: 0,
  myPoints: 0,
  records: []
})

// 获取排行榜数据
const fetchRankList = async () => {
  try {
    const data = await pointsApi.getRankList(50)
    rankData.value = data
  } catch (e) {
    toast.error('获取排行榜失败')
  } finally {
    loading.value = false
  }
}

// 获取排名样式
const getRankStyle = (rank) => {
  if (rank === 1) return 'bg-yellow-400 text-white'
  if (rank === 2) return 'bg-gray-300 text-white'
  if (rank === 3) return 'bg-orange-400 text-white'
  return 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
}

// 获取排名图标颜色
const getRankIconColor = (rank) => {
  if (rank === 1) return '#FFD700'
  if (rank === 2) return '#C0C0C0'
  if (rank === 3) return '#CD7F32'
  return null
}

onMounted(() => {
  fetchRankList()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航 -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-10">
      <div class="max-w-4xl mx-auto px-4 py-4 flex items-center gap-4">
        <n-button quaternary circle @click="router.back()">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
        </n-button>
        <h1 class="text-lg font-semibold text-gray-900 dark:text-white">积分排行榜</h1>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-4 py-6">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-20">
        <n-spin size="large" />
      </div>

      <template v-else>
        <!-- 我的排名 -->
        <div class="bg-gradient-to-r from-primary-500 to-primary-600 rounded-2xl p-6 mb-6 text-white">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/80">我的排名</p>
              <p class="text-3xl font-bold mt-1">第 {{ rankData.myRank }} 名</p>
            </div>
            <div class="text-right">
              <p class="text-sm text-white/80">本月积分</p>
              <p class="text-3xl font-bold mt-1">{{ formatNumber(rankData.myPoints) }}</p>
            </div>
          </div>
          <p class="text-sm text-white/60 mt-4">共 {{ rankData.totalUsers }} 人参与排名</p>
        </div>

        <!-- 排行榜列表 -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <!-- 列表头部 -->
          <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700/50">
            <div class="flex items-center gap-2">
              <n-icon :component="TrophyOutline" :size="20" class="text-yellow-500" />
              <h2 class="font-semibold text-gray-900 dark:text-white">本月排行榜</h2>
            </div>
          </div>

          <!-- 排行列表 -->
          <div class="divide-y divide-gray-100 dark:divide-gray-700/50">
            <div v-for="item in rankData.records" :key="item.userId"
              class="px-6 py-4 flex items-center gap-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              :class="{ 'bg-primary-50 dark:bg-primary-900/20': item.rank === rankData.myRank }">
              
              <!-- 排名 -->
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm"
                :class="getRankStyle(item.rank)">
                <template v-if="item.rank <= 3">
                  <n-icon :component="MedalOutline" :size="20" />
                </template>
                <template v-else>
                  {{ item.rank }}
                </template>
              </div>

              <!-- 头像 -->
              <n-avatar
                :size="44"
                :src="item.avatar || DEFAULT_AVATAR"
                round
                lazy
              />

              <!-- 用户信息 -->
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-900 dark:text-white truncate">
                  {{ item.nickname }}
                  <span v-if="item.rank === rankData.myRank" class="text-xs text-primary-500 ml-2">(我)</span>
                </p>
                <p class="text-sm text-gray-500 dark:text-gray-400">
                  {{ item.rank <= 3 ? ['冠军', '亚军', '季军'][item.rank - 1] : `第 ${item.rank} 名` }}
                </p>
              </div>

              <!-- 积分 -->
              <div class="text-right">
                <p class="text-lg font-bold text-primary-500">{{ formatNumber(item.points) }}</p>
                <p class="text-xs text-gray-500 dark:text-gray-400">积分</p>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="rankData.records.length === 0" class="py-12 text-center text-gray-500 dark:text-gray-400">
            暂无排行数据
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
/* 自定义样式 */
</style>