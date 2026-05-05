<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {adminArticleApi, adminCategoryApi} from '@/utils/request'

const router = useRouter()

const categories = ref([])
const articles = ref([])
const loading = ref(false)
const totalArticles = ref(0)
const totalPages = ref(0)

const searchQuery = ref('')
const statusFilter = ref('all')
const reviewStatusFilter = ref('all')
const categoryFilter = ref('')
const currentPage = ref(1)
const pageSize = 10

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'published', label: '已发布' },
  { value: 'draft', label: '草稿' },
  { value: 'offline', label: '已下架' }
]

const reviewStatusOptions = [
  { value: 'all', label: '全部审核' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已拒绝' }
]

const fetchCategories = async () => {
  try {
    categories.value = await adminCategoryApi.getList()
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize,
      keyword: searchQuery.value || undefined,
      status: statusFilter.value !== 'all' ? statusFilter.value : undefined,
      reviewStatus: reviewStatusFilter.value !== 'all' ? reviewStatusFilter.value : undefined,
      categoryId: categoryFilter.value || undefined,
      sortField: 'created_at',
      sortOrder: 'desc'
    }
    const data = await adminArticleApi.getPage(params)
    articles.value = data.records || []
    totalArticles.value = data.total || 0
    totalPages.value = data.pages || 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchArticles()
})

const filteredArticles = computed(() => articles.value)

const paginatedArticles = computed(() => articles.value)

const getStatusText = (status) => {
  const texts = {
    published: '已发布',
    draft: '草稿',
    offline: '已下架'
  }
  return texts[status] || status
}

const getReviewStatusClass = (status) => {
  const classes = {
    pending: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    approved: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    rejected: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400'
  }
  return classes[status] || ''
}

const getReviewStatusText = (status) => {
  const texts = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return texts[status] || status
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const getStatusClass = (status) => {
  const classes = {
    published: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    draft: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    offline: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
  }
  return classes[status] || ''
}

const toggleTop = async (article) => {
  try {
    await adminArticleApi.toggleTop(article.id, !article.isTop)
    article.isTop = !article.isTop
  } catch (error) {
    console.error('设置置顶失败:', error)
    alert('操作失败: ' + (error.message || '未知错误'))
  }
}

const approveArticle = async (article) => {
  try {
    await adminArticleApi.approve(article.id)
    article.reviewStatus = 'approved'
  } catch (error) {
    console.error('审核通过失败:', error)
    alert('操作失败: ' + (error.message || '未知错误'))
  }
}

const rejectArticle = async (article) => {
  const reason = prompt('请输入拒绝原因（可选）:')
  try {
    await adminArticleApi.reject(article.id, reason)
    article.reviewStatus = 'rejected'
  } catch (error) {
    console.error('审核拒绝失败:', error)
    alert('操作失败: ' + (error.message || '未知错误'))
  }
}

const handleDelete = async (article) => {
  if (confirm(`确定要删除文章"${article.title}"吗？`)) {
    try {
      await adminArticleApi.delete(article.id)
      fetchArticles()
    } catch (error) {
      console.error('删除文章失败:', error)
      alert('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

const viewDetail = (article) => {
  router.push(`/articles/${article.id}`)
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchArticles()
}

const handleSearch = () => {
  currentPage.value = 1
  fetchArticles()
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchArticles()
}
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">文章管理</h1>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">管理您的所有文章内容，支持置顶和审核功能</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col gap-4">
            <div class="flex-1">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="搜索文章标题..."
                  @keyup.enter="handleSearch"
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
                />
              </div>
            </div>

            <div class="flex flex-wrap gap-3">
              <select
                v-model="statusFilter"
                @change="handleFilterChange"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>

              <select
                v-model="reviewStatusFilter"
                @change="handleFilterChange"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option v-for="option in reviewStatusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>

              <select
                v-model="categoryFilter"
                @change="handleFilterChange"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option value="">全部分类</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">封面</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">标题</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">作者</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">分类</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">审核</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">置顶</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">浏览量</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">创建时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="article in paginatedArticles"
                :key="article.id"
                :class="[
                  'hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors',
                  article.isTop ? 'bg-yellow-50 dark:bg-yellow-900/10' : ''
                ]"
              >
                <td class="px-4 py-4">
                  <img
                    v-if="article.cover"
                    :src="article.cover"
                    :alt="article.title"
                    class="w-16 h-10 object-cover rounded"
                  />
                  <div v-else class="w-16 h-10 bg-gray-200 dark:bg-gray-600 rounded flex items-center justify-center">
                    <svg class="w-6 h-6 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                </td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-2">
                    <div class="text-sm font-medium text-gray-900 dark:text-white line-clamp-1 max-w-xs">
                      {{ article.title }}
                    </div>
                    <span v-if="article.isTop" class="px-1.5 py-0.5 text-xs bg-red-500 text-white rounded">置顶</span>
                  </div>
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden sm:table-cell">{{ article.authorName }}</td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden md:table-cell">{{ article.categoryName }}</td>
                <td class="px-4 py-4">
                  <span
                    class="px-2 py-1 text-xs rounded-full"
                    :class="getStatusClass(article.status)"
                  >
                    {{ getStatusText(article.status) }}
                  </span>
                </td>
                <td class="px-4 py-4">
                  <span
                    class="px-2 py-1 text-xs rounded-full"
                    :class="getReviewStatusClass(article.reviewStatus)"
                  >
                    {{ getReviewStatusText(article.reviewStatus) }}
                  </span>
                </td>
                <td class="px-4 py-4 hidden lg:table-cell">
                  <button
                    @click="toggleTop(article)"
                    :class="[
                      'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                      article.isTop ? 'bg-red-500' : 'bg-gray-300 dark:bg-gray-600'
                    ]"
                    :title="article.isTop ? '取消置顶' : '设为置顶'"
                  >
                    <span
                      :class="[
                        'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                        article.isTop ? 'translate-x-4' : 'translate-x-0.5'
                      ]"
                    />
                  </button>
                </td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden xl:table-cell">{{ article.views }}</td>
                <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400 hidden sm:table-cell">{{ formatDate(article.createdAt) }}</td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-1">
                    <button
                      @click="viewDetail(article)"
                      class="p-1.5 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
                      title="查看详情"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button
                      v-if="article.reviewStatus === 'pending'"
                      @click="approveArticle(article)"
                      class="p-1.5 text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20 rounded transition-colors"
                      title="通过审核"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button
                      v-if="article.reviewStatus === 'pending'"
                      @click="rejectArticle(article)"
                      class="p-1.5 text-orange-500 hover:bg-orange-50 dark:hover:bg-orange-900/20 rounded transition-colors"
                      title="拒绝"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                    <button
                      @click="handleDelete(article)"
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

        <div v-if="totalArticles === 0 && !loading" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无文章数据</p>
        </div>

        <div v-if="totalArticles > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-500 dark:text-gray-400">
              共 {{ totalArticles }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
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
                  v-for="page in Math.min(totalPages, 10)"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 rounded text-sm transition-colors"
                  :class="currentPage === page
                    ? 'bg-red-500 text-white'
                    : 'hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'"
                >
                  {{ page }}
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
