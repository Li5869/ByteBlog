<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {adminCommentApi} from '../utils/request'

const router = useRouter()

const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const comments = ref([])

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' }
]

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '文章评论', value: 'article' }
]

const fetchComments = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) params.keyword = searchQuery.value
    if (statusFilter.value !== 'all') params.status = statusFilter.value
    if (typeFilter.value !== 'all') params.targetType = typeFilter.value

    const res = await adminCommentApi.getPage(params)
    comments.value = res.records || []
    total.value = res.total || 0
    totalPages.value = res.pages || 0
  } catch (e) {
    console.error('获取评论列表失败:', e)
  } finally {
    loading.value = false
  }
}

let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchComments()
  }, 300)
}

const onStatusChange = (status) => {
  statusFilter.value = status
  currentPage.value = 1
  fetchComments()
}

const onTypeChange = () => {
  currentPage.value = 1
  fetchComments()
}

const getStatusText = (status) => {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    pending: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    approved: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    rejected: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400'
  }
  return map[status] || ''
}

const truncateContent = (content, maxLength = 30) => {
  if (!content) return ''
  if (content.length <= maxLength) return content
  return content.slice(0, maxLength) + '...'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleApprove = async (comment) => {
  try {
    await adminCommentApi.approve(comment.id)
    comment.status = 'approved'
  } catch (e) {
    console.error('审核失败:', e)
    alert(e.message || '操作失败')
  }
}

const handleReject = async (comment) => {
  try {
    await adminCommentApi.reject(comment.id)
    comment.status = 'rejected'
  } catch (e) {
    console.error('审核失败:', e)
    alert(e.message || '操作失败')
  }
}

const handleDelete = async (comment) => {
  if (confirm('确定要删除该评论吗？')) {
    try {
      await adminCommentApi.delete(comment.id)
      fetchComments()
    } catch (e) {
      console.error('删除失败:', e)
      alert(e.message || '删除失败')
    }
  }
}

const viewDetail = (comment) => {
  router.push(`/comments/${comment.id}`)
}

const handlePageChange = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchComments()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchComments()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchComments()
  }
}

onMounted(() => {
  fetchComments()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-7xl mx-auto">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">评论管理</h1>
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
                  @input="onSearch"
                  type="text"
                  placeholder="搜索评论内容..."
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
                />
              </div>
            </div>

            <div class="flex flex-wrap gap-2">
              <div class="flex gap-2">
                <button
                  v-for="option in statusOptions"
                  :key="option.value"
                  @click="onStatusChange(option.value)"
                  class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                  :class="statusFilter === option.value
                    ? 'bg-primary-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
                >
                  {{ option.label }}
                </button>
              </div>

              <select
                v-model="typeFilter"
                @change="onTypeChange"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              >
                <option v-for="option in typeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">ID</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">评论者</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">评论内容</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">关联内容</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">点赞量</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">创建时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="comment in comments"
                :key="comment.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">{{ comment.id }}</td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <div class="flex items-center gap-3">
                    <img
                      :src="comment.authorAvatar"
                      :alt="comment.authorName"
                      class="w-8 h-8 rounded-full object-cover"
                    />
                    <span class="text-sm font-medium text-gray-900 dark:text-white">{{ comment.authorName }}</span>
                  </div>
                </td>
                <td class="px-4 py-4">
                  <div
                    class="text-sm text-gray-600 dark:text-gray-300 max-w-xs cursor-help"
                    :title="comment.content"
                  >
                    {{ truncateContent(comment.content, 25) }}
                  </div>
                </td>
                <td class="px-4 py-4 hidden md:table-cell">
                  <span class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1 max-w-[200px]">
                    {{ comment.targetTitle || '-' }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getStatusClass(comment.status)"
                  >
                    {{ getStatusText(comment.status) }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">
                  <div class="flex items-center gap-1">
                    <svg class="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd" />
                    </svg>
                    <span>{{ comment.likes }}</span>
                  </div>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden xl:table-cell">
                  {{ formatDate(comment.createdAt) }}
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm">
                  <div class="flex items-center gap-2">
                    <button
                      @click="viewDetail(comment)"
                      class="p-1.5 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
                      title="查看详情"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button
                      v-if="comment.status !== 'approved'"
                      @click="handleApprove(comment)"
                      class="p-1.5 text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20 rounded transition-colors"
                      title="通过"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button
                      v-if="comment.status !== 'rejected'"
                      @click="handleReject(comment)"
                      class="p-1.5 text-orange-500 hover:bg-orange-50 dark:hover:bg-orange-900/20 rounded transition-colors"
                      title="拒绝"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                    <button
                      @click="handleDelete(comment)"
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

        <div v-if="!loading && comments.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无评论数据</p>
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
                  v-for="page in Math.min(totalPages, 7)"
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

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
