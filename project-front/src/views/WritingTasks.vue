<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {aiApi} from '@/utils/request'
import {toast} from '@/utils/toast'

const router = useRouter()

const loading = ref(false)
const tasks = ref([])

const statusMap = {
  planning: { label: '规划中', color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
  plan_ready: { label: '待确认', color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z' },
  executing: { label: '写作中', color: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' },
  reflecting: { label: '反思中', color: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400', icon: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z' },
  finalized: { label: '已完成', color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
  finalized_pending: { label: '待处理', color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
  error: { label: '失败', color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z' },
  cancelled: { label: '已取消', color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400', icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z' },
  stopped: { label: '已停止', color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400', icon: 'M21 12a9 9 0 11-18 0 9 9 0 0118 0z M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z' }
}

const stepMap = {
  generating_plan: '生成计划',
  regenerating_plan: '重新生成计划',
  awaiting_approval: '等待确认',
  title: '生成标题',
  summary: '生成摘要',
  content: '生成正文',
  tags: '生成标签',
  completed: '已完成'
}

const getStatusInfo = (status, finalAction) => {
  // finalized 状态且没有 finalAction → 显示"待处理"
  if (status === 'finalized' && !finalAction) {
    return statusMap.finalized_pending
  }
  return statusMap[status] || statusMap.planning
}

const getStepText = (step) => {
  return stepMap[step] || step || '-'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatRelativeTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatDate(dateStr)
}

// 获取任务列表
const fetchTasks = async () => {
  loading.value = true
  try {
    tasks.value = await aiApi.getMyWritingTasks()
  } catch (error) {
    toast.error(error.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleDeleteTask = async (taskId) => {
  if (!confirm('确定要删除这个写作任务吗？')) return
  try {
    await aiApi.deleteWritingTask(taskId)
    tasks.value = tasks.value.filter(t => t.id !== taskId)
    toast.success('删除成功')
  } catch (error) {
    toast.error(error.message || '删除失败')
  }
}

const handleCreateNew = () => {
  router.push('/ai-writing')
}

const canDelete = (task) => {
  return !['planning', 'executing', 'reflecting'].includes(task.status)
}

const getButtonText = (task) => {
  if (task.status === 'finalized' && !task.finalAction) return '发布文章'
  if (task.status === 'finalized') return '查看文章'
  if (task.status === 'plan_ready') return '确认计划'
  if (task.status === 'error') return '重试'
  if (['planning', 'executing', 'reflecting'].includes(task.status)) return '查看进度'
  return '继续任务'
}

const getButtonClass = (task) => {
  if (task.status === 'finalized' && !task.finalAction) return 'text-white bg-green-500 hover:bg-green-600'
  if (task.status === 'finalized') return 'text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/20 hover:bg-primary-100 dark:hover:bg-primary-900/30'
  if (task.status === 'error') return 'text-white bg-orange-500 hover:bg-orange-600'
  if (['planning', 'executing', 'reflecting'].includes(task.status)) return 'text-white bg-gray-500 hover:bg-gray-600'
  return 'text-white bg-primary-500 hover:bg-primary-600'
}

const handleTaskAction = (task) => {
  if (task.status === 'finalized' && task.finalAction && task.articleId) {
    router.push(`/article/${task.articleId}`)
  } else {
    router.push(`/ai-writing?taskId=${task.id}`)
  }
}

const filteredTasks = computed(() => {
  return tasks.value
})

onMounted(() => {
  fetchTasks()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">我的写作任务</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">管理您的 AI 辅助写作任务</p>
        </div>
        <button
          @click="handleCreateNew"
          class="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl text-sm font-semibold hover:from-primary-600 hover:to-primary-700 transition-all shadow-lg shadow-primary-500/25"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新建写作任务
        </button>
      </div>

      <!-- 任务列表 -->
      <div v-if="loading" class="space-y-4">
        <div v-for="i in 3" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700 animate-pulse">
          <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded w-1/4 mb-4"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-3"></div>
          <div class="flex gap-4">
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
          </div>
        </div>
      </div>

      <div v-else-if="filteredTasks.length === 0" class="text-center py-16">
        <div class="w-24 h-24 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-primary-100 to-primary-200 dark:from-primary-900/30 dark:to-primary-800/30 flex items-center justify-center">
          <svg class="w-12 h-12 text-primary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">暂无写作任务</h3>
        <p class="text-gray-500 dark:text-gray-400 mb-6">开始使用 AI 辅助写作，快速生成高质量文章</p>
        <button
          @click="handleCreateNew"
          class="inline-flex items-center gap-2 px-6 py-3 bg-primary-500 text-white rounded-xl font-medium hover:bg-primary-600 transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          开始写作
        </button>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="task in filteredTasks"
          :key="task.id"
          class="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700 hover:shadow-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300"
        >
          <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <!-- 状态标签 -->
              <div class="flex items-center gap-2 mb-3">
                <span
                  class="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-lg"
                  :class="getStatusInfo(task.status, task.finalAction).color"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="getStatusInfo(task.status, task.finalAction).icon" />
                  </svg>
                  {{ getStatusInfo(task.status, task.finalAction).label }}
                </span>
                <span v-if="task.currentStep && task.status !== 'finalized' && task.status !== 'error'" class="text-xs text-gray-500 dark:text-gray-400">
                  · {{ getStepText(task.currentStep) }}
                </span>
                <span v-if="task.revisionCount > 0" class="text-xs text-gray-400">
                  · 已修订 {{ task.revisionCount }} 次
                </span>
              </div>

              <!-- 任务描述 -->
              <p class="text-gray-900 dark:text-white font-medium mb-2 line-clamp-2">{{ task.userRequest }}</p>

              <!-- 关联文章 -->
              <div v-if="task.articleId" class="flex items-center gap-2 text-sm text-primary-600 dark:text-primary-400 mb-3">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span class="truncate">已关联文章</span>
              </div>

              <!-- 错误信息 -->
              <div v-if="task.status === 'error' && task.errorMessage" class="flex items-center gap-2 text-sm text-red-600 dark:text-red-400 mb-3">
                <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>{{ task.errorMessage }}</span>
              </div>

              <!-- 时间信息 -->
              <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  创建于 {{ formatRelativeTime(task.createdAt) }}
                </span>
                <span v-if="task.completedAt" class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  完成于 {{ formatRelativeTime(task.completedAt) }}
                </span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex items-center gap-2 sm:flex-shrink-0">
              <button
                @click="handleTaskAction(task)"
                class="px-4 py-2 text-sm rounded-lg transition-colors font-medium"
                :class="getButtonClass(task)"
              >
                {{ getButtonText(task) }}
              </button>
              <button
                v-if="canDelete(task)"
                @click="handleDeleteTask(task.id)"
                class="px-3 py-2 text-sm text-gray-500 dark:text-gray-400 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
