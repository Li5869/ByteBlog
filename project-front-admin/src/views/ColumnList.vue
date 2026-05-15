<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {columnApi} from '../utils/request.js'
import {formatAbsoluteDate} from '@/utils/format'

const router = useRouter()

const columns = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('all')
const orderByFilter = ref('created_at')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: '1', label: '已发布' },
  { value: '0', label: '草稿' }
]

const orderByOptions = [
  { value: 'created_at', label: '按创建时间' },
  { value: 'updated_at', label: '按更新时间' },
  { value: 'articles_count', label: '按文章数' },
  { value: 'views', label: '按浏览量' }
]

const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value) || 1
})

const getStatusClass = (status) => {
  return status === 1
    ? 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400'
    : 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400'
}

const getStatusText = (status) => {
  return status === 1 ? '已发布' : '草稿'
}

const fetchColumns = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      orderBy: orderByFilter.value
    }
    if (statusFilter.value !== 'all') {
      params.status = parseInt(statusFilter.value)
    }
    const result = await columnApi.list(params)
    columns.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    console.error('获取专栏列表失败:', error)
    alert(error.message || '获取专栏列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchColumns()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchColumns()
}

const viewDetail = (column) => {
  router.push(`/columns/${column.id}`)
}

const handleDelete = async (column) => {
  if (!confirm(`确定要删除专栏"${column.title}"吗？删除后将无法恢复。`)) {
    return
  }
  try {
    await columnApi.delete(column.id)
    alert('删除成功')
    fetchColumns()
  } catch (error) {
    console.error('删除专栏失败:', error)
    alert(error.message || '删除失败')
  }
}

const handleStatusChange = async (column, newStatus) => {
  try {
    await columnApi.update(column.id, { status: newStatus })
    alert('状态更新成功')
    fetchColumns()
  } catch (error) {
    console.error('更新状态失败:', error)
    alert(error.message || '更新状态失败')
  }
}

onMounted(() => {
  fetchColumns()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">专栏管理</h1>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">管理所有专栏内容，支持状态切换和删除操作</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col gap-4">
            <div class="flex flex-col sm:flex-row gap-3">
              <div class="flex-1">
                <div class="relative">
                  <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input
                    v-model="searchQuery"
                    type="text"
                    placeholder="搜索专栏标题..."
                    class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
                    @keyup.enter="handleSearch"
                  />
                </div>
              </div>
              <button
                @click="handleSearch"
                class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
              >
                搜索
              </button>
            </div>

            <div class="flex flex-wrap gap-3">
              <select
                v-model="statusFilter"
                @change="handleSearch"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>

              <select
                v-model="orderByFilter"
                @change="handleSearch"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option v-for="option in orderByOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <div v-if="loading" class="p-8 text-center">
          <svg class="animate-spin w-8 h-8 mx-auto text-red-500" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">加载中...</p>
        </div>

        <div v-else-if="columns.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无专栏数据</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">专栏信息</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">作者</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">文章数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">订阅数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">浏览量</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">创建时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="column in columns"
                :key="column.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4">
                  <div class="flex items-center gap-3">
                    <img
                      v-if="column.cover"
                      :src="column.cover"
                      :alt="column.title"
                      class="w-12 h-12 rounded-lg object-cover flex-shrink-0"
                    />
                    <div
                      v-else
                      class="w-12 h-12 rounded-lg bg-gray-200 dark:bg-gray-600 flex items-center justify-center flex-shrink-0"
                    >
                      <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                    </div>
                    <div class="min-w-0">
                      <div class="text-sm font-medium text-gray-900 dark:text-white line-clamp-1 max-w-xs">
                        {{ column.title }}
                      </div>
                      <div class="text-xs text-gray-500 dark:text-gray-400 line-clamp-1 max-w-xs">
                        {{ column.description || '暂无描述' }}
                      </div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4 hidden md:table-cell">
                  <div class="flex items-center gap-2">
                    <img
                      v-if="column.authorAvatar"
                      :src="column.authorAvatar"
                      :alt="column.authorName"
                      class="w-6 h-6 rounded-full object-cover"
                    />
                    <span class="text-sm text-gray-500 dark:text-gray-400">{{ column.authorName || '未知' }}</span>
                  </div>
                </td>
                <td class="px-4 py-4">
                  <span
                    class="px-2 py-1 text-xs rounded-full cursor-pointer"
                    :class="getStatusClass(column.status)"
                    @click="handleStatusChange(column, column.status === 1 ? 0 : 1)"
                    :title="column.status === 1 ? '点击切换为草稿' : '点击发布'"
                  >
                    {{ getStatusText(column.status) }}
                  </span>
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden lg:table-cell">
                  {{ column.articlesCount || 0 }}
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden lg:table-cell">
                  {{ column.subscriptionCount || 0 }}
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden xl:table-cell">
                  {{ column.views || 0 }}
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden sm:table-cell">
                  {{ formatAbsoluteDate(column.createdAt) }}
                </td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-1">
                    <button
                      @click="viewDetail(column)"
                      class="p-1.5 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
                      title="查看详情"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button
                      @click="handleDelete(column)"
                      class="p-1.5 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                      title="删除"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="total > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-500 dark:text-gray-400">
              共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="handlePageChange(currentPage - 1)"
                :disabled="currentPage === 1"
                class="px-3 py-1 rounded border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                上一页
              </button>
              <div class="flex items-center gap-1">
                <button
                  v-for="page in Math.min(5, totalPages)"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 rounded text-sm transition-colors"
                  :class="currentPage === page
                    ? 'bg-red-500 text-white'
                    : 'hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'"
                >
                  {{ page }}
                </button>
                <span v-if="totalPages > 5" class="px-2 text-gray-400">...</span>
                <button
                  v-if="totalPages > 5"
                  @click="handlePageChange(totalPages)"
                  class="w-8 h-8 rounded text-sm transition-colors"
                  :class="currentPage === totalPages
                    ? 'bg-red-500 text-white'
                    : 'hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'"
                >
                  {{ totalPages }}
                </button>
              </div>
              <button
                @click="handlePageChange(currentPage + 1)"
                :disabled="currentPage === totalPages"
                class="px-3 py-1 rounded border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
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

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
