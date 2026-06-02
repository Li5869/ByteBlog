<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {NButton, NIcon, NPagination, NSelect, NSpin} from 'naive-ui'
import {pointsApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatAbsoluteDate} from '@/utils/format'
import {ArrowBackOutline, CalendarOutline, TrendingDownOutline, TrendingUpOutline} from '@vicons/ionicons5'

const router = useRouter()
const loading = ref(true)

// 分页参数
const pagination = ref({
  current: 1,
  size: 20,
  total: 0
})

// 筛选类型
const filterType = ref(null)
const typeOptions = [
  { label: '全部类型', value: null },
  { label: '每日签到', value: 'sign' },
  { label: '发布文章', value: 'article_published' },
  { label: '文章被点赞', value: 'article_liked' },
  { label: '文章被收藏', value: 'article_collected' },
  { label: '管理员调整', value: 'admin_adjust' }
]

// 积分流水数据
const logs = ref([])

// 获取积分类型显示文本
const getTypeLabel = (type) => {
  const map = {
    'sign': '每日签到',
    'article_published': '发布文章',
    'article_liked': '文章被点赞',
    'article_collected': '文章被收藏',
    'comment_liked': '评论被点赞',
    'answer_liked': '回答被点赞',
    'exchange': '积分兑换',
    'admin_adjust': '管理员调整'
  }
  return map[type] || type
}

// 获取积分类型图标
const getTypeIcon = (type) => {
  const iconMap = {
    'sign': '📅',
    'article_published': '📝',
    'article_liked': '👍',
    'article_collected': '⭐',
    'comment_liked': '💬',
    'answer_liked': '✅',
    'exchange': '🎁',
    'admin_adjust': '👤'
  }
  return iconMap[type] || '📌'
}

// 获取积分流水数据
const fetchLogs = async () => {
  loading.value = true
  try {
    const data = await pointsApi.getPointLogs(
      pagination.value.current,
      pagination.value.size,
      filterType.value
    )
    logs.value = data.records
    pagination.value.total = data.total
  } catch (e) {
    toast.error('获取积分流水失败')
  } finally {
    loading.value = false
  }
}

// 切换页码
const handlePageChange = (page) => {
  pagination.value.current = page
  fetchLogs()
}

// 切换筛选类型
const handleTypeChange = (type) => {
  filterType.value = type
  pagination.value.current = 1
  fetchLogs()
}

onMounted(() => {
  fetchLogs()
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
        <h1 class="text-lg font-semibold text-gray-900 dark:text-white">积分流水</h1>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-4 py-6">
      <!-- 筛选器 -->
      <div class="mb-6">
        <n-select
          v-model:value="filterType"
          :options="typeOptions"
          placeholder="选择类型筛选"
          clearable
          @update:value="handleTypeChange"
        />
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-20">
        <n-spin size="large" />
      </div>

      <template v-else>
        <!-- 流水列表 -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <div class="divide-y divide-gray-100 dark:divide-gray-700/50">
            <div v-for="log in logs" :key="log.id"
              class="px-6 py-4 flex items-center gap-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
              
              <!-- 类型图标 -->
              <div class="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-xl flex items-center justify-center text-lg">
                {{ getTypeIcon(log.type) }}
              </div>

              <!-- 信息 -->
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-900 dark:text-white">
                  {{ log.description || getTypeLabel(log.type) }}
                </p>
                <div class="flex items-center gap-2 mt-1">
                  <n-icon :component="CalendarOutline" :size="14" class="text-gray-400" />
                  <span class="text-sm text-gray-500 dark:text-gray-400">
                    {{ formatAbsoluteDate(log.createdAt) }}
                  </span>
                </div>
              </div>

              <!-- 积分变动 -->
              <div class="text-right">
                <p class="text-lg font-bold" :class="log.points > 0 ? 'text-green-500' : 'text-red-500'">
                  {{ log.points > 0 ? '+' : '' }}{{ log.points }}
                </p>
                <div class="flex items-center gap-1 justify-end mt-1">
                  <n-icon 
                    :component="log.points > 0 ? TrendingUpOutline : TrendingDownOutline" 
                    :size="14" 
                    :class="log.points > 0 ? 'text-green-500' : 'text-red-500'" 
                  />
                  <span class="text-xs text-gray-500 dark:text-gray-400">积分</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="logs.length === 0" class="py-12 text-center text-gray-500 dark:text-gray-400">
            暂无积分流水记录
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="pagination.total > pagination.size" class="mt-6 flex justify-center">
          <n-pagination
            v-model:page="pagination.current"
            :page-size="pagination.size"
            :item-count="pagination.total"
            @update:page="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
</style>
