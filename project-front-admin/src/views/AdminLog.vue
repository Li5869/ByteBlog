<script setup>
import {onMounted, ref, watch} from 'vue'
import {adminLogApi} from '../utils/request.js'
import {formatAbsoluteDate} from '@/utils/format'

const searchQuery = ref('')
const actionTypeFilter = ref('all')
const targetTypeFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const totalPages = ref(0)
const logs = ref([])
const loading = ref(false)

let searchTimer = null

const actionTypeOptions = [
  { label: '全部操作', value: 'all' },
  { label: '登录', value: 'login' },
  { label: '退出', value: 'logout' },
  { label: '新增', value: 'create' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '审核', value: 'review' }
]

const targetTypeOptions = [
  { label: '全部对象', value: 'all' },
  { label: '文章', value: 'article' },
  { label: '用户', value: 'user' },
  { label: '评论', value: 'comment' },
  { label: '问题', value: 'question' },
  { label: '回答', value: 'answer' },
  { label: '分类', value: 'category' },
  { label: '标签', value: 'tag' }
]

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    if (actionTypeFilter.value !== 'all') {
      params.actionType = actionTypeFilter.value
    }
    if (targetTypeFilter.value !== 'all') {
      params.targetType = targetTypeFilter.value
    }
    const res = await adminLogApi.getPage(params)
    logs.value = res.records || []
    total.value = res.total || 0
    totalPages.value = Math.ceil(total.value / pageSize.value)
  } catch (e) {
    console.error('获取操作日志失败:', e)
    logs.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchLogs()
  }, 300)
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchLogs()
}

watch(actionTypeFilter, handleFilterChange)
watch(targetTypeFilter, handleFilterChange)

const getActionTypeText = (type) => {
  const map = {
    login: '登录',
    logout: '退出',
    create: '新增',
    update: '修改',
    delete: '删除',
    review: '审核'
  }
  return map[type] || type
}

const getActionTypeClass = (type) => {
  const map = {
    login: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    logout: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
    create: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
    update: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    delete: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    review: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400'
  }
  return map[type] || ''
}

const getTargetTypeText = (type) => {
  if (!type) return '-'
  const map = {
    article: '文章',
    user: '用户',
    comment: '评论',
    question: '问题',
    answer: '回答',
    category: '分类',
    tag: '标签'
  }
  return map[type] || type
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchLogs()
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchLogs()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchLogs()
  }
}

const exportLogs = () => {
}

onMounted(() => {
  fetchLogs()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-7xl mx-auto">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">操作日志</h1>
            <button
              @click="exportLogs"
              class="inline-flex items-center justify-center px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors text-sm font-medium"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              导出日志
            </button>
          </div>
        </div>

        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col lg:flex-row gap-4">
            <div class="flex-1">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  @input="handleSearch"
                  type="text"
                  placeholder="搜索操作内容或管理员名称..."
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
                />
              </div>
            </div>

            <div class="flex flex-wrap gap-2">
              <select
                v-model="actionTypeFilter"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              >
                <option v-for="option in actionTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>

              <select
                v-model="targetTypeFilter"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              >
                <option v-for="option in targetTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作人</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作类型</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">操作对象</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作描述</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">IP地址</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">操作时间</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="log in logs"
                :key="log.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">{{ log.adminName || '-' }}</td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getActionTypeClass(log.actionType)"
                  >
                    {{ getActionTypeText(log.actionType) }}
                  </span>
                </td>
                <td class="px-4 py-4 hidden md:table-cell">
                  <div v-if="log.targetType" class="flex flex-col">
                    <span class="text-xs text-gray-500 dark:text-gray-400">{{ getTargetTypeText(log.targetType) }}</span>
                    <span v-if="log.targetId" class="text-sm text-gray-500 dark:text-gray-400">
                      #{{ log.targetId }}
                    </span>
                  </div>
                  <span v-else class="text-sm text-gray-400">-</span>
                </td>
                <td class="px-4 py-4">
                  <div class="text-sm text-gray-900 dark:text-white max-w-xs truncate" :title="log.description">
                    {{ log.description }}
                  </div>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">
                  <span class="font-mono text-xs">{{ log.ipAddress || '-' }}</span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden xl:table-cell">
                  {{ formatAbsoluteDate(log.createdAt) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!loading && logs.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p class="text-gray-500 dark:text-gray-400">暂无日志记录</p>
        </div>

        <div v-if="total > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-600 dark:text-gray-400">
              共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="prevPage"
                :disabled="currentPage === 1"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                上一页
              </button>
              <div class="flex items-center gap-1">
                <button
                  v-for="page in totalPages"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 text-sm rounded-lg transition-colors"
                  :class="currentPage === page
                    ? 'bg-primary-500 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
                >
                  {{ page }}
                </button>
              </div>
              <button
                @click="nextPage"
                :disabled="currentPage === totalPages"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
