<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {adminArticleApi} from '@/utils/request'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => route.params.id)
const loading = ref(true)
const article = ref(null)

const fetchDetail = async () => {
  loading.value = true
  try {
    article.value = await adminArticleApi.getDetail(articleId.value)
  } catch (e) {
    console.error('获取文章详情失败:', e)
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const texts = { published: '已发布', draft: '草稿', offline: '已下架' }
  return texts[status] || status
}

const getStatusClass = (status) => {
  const classes = {
    published: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    draft: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    offline: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
  }
  return classes[status] || ''
}

const getReviewStatusText = (status) => {
  const texts = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
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

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const goBack = () => router.back()

const approveArticle = async () => {
  try {
    await adminArticleApi.approve(articleId.value)
    article.value.reviewStatus = 'approved'
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

const rejectArticle = async () => {
  const reason = prompt('请输入拒绝原因（可选）:')
  try {
    await adminArticleApi.reject(articleId.value, reason)
    article.value.reviewStatus = 'rejected'
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

const toggleTop = async () => {
  try {
    await adminArticleApi.toggleTop(articleId.value, !article.value.isTop)
    article.value.isTop = !article.value.isTop
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="mb-6">
        <button
          @click="goBack"
          class="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          返回列表
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-12">
        <div class="inline-block w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="mt-2 text-gray-500 dark:text-gray-400">加载中...</p>
      </div>

      <div v-else-if="article" class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <!-- 封面 -->
        <div v-if="article.cover" class="w-full h-48 sm:h-64 overflow-hidden rounded-t-lg">
          <img :src="article.cover" :alt="article.title" class="w-full h-full object-cover" />
        </div>

        <!-- 标题区 -->
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-2">
                <span class="px-2 py-1 text-xs rounded-full" :class="getStatusClass(article.status)">
                  {{ getStatusText(article.status) }}
                </span>
                <span class="px-2 py-1 text-xs rounded-full" :class="getReviewStatusClass(article.reviewStatus)">
                  {{ getReviewStatusText(article.reviewStatus) }}
                </span>
                <span v-if="article.isTop" class="px-2 py-1 text-xs bg-red-500 text-white rounded">置顶</span>
              </div>
              <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">{{ article.title }}</h1>
              <div class="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                <div class="flex items-center gap-2">
                  <img :src="article.authorAvatar" :alt="article.authorName" class="w-6 h-6 rounded-full object-cover" />
                  <span>{{ article.authorName }}</span>
                </div>
                <span>{{ article.categoryName }}</span>
                <span>创建于 {{ formatDate(article.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 摘要 -->
        <div v-if="article.summary" class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">文章摘要</h2>
          <p class="text-gray-600 dark:text-gray-300 leading-relaxed">{{ article.summary }}</p>
        </div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length > 0" class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-wrap gap-2">
            <span
              v-for="tag in article.tags"
              :key="tag.id"
              class="px-3 py-1 text-sm bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded-full"
            >
              # {{ tag.name }}
            </span>
          </div>
        </div>

        <!-- 统计 -->
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
            <div class="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ article.views }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">浏览量</div>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ article.likes }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">点赞数</div>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ article.comments }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">评论数</div>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
              <div class="text-xl font-bold text-gray-900 dark:text-white">{{ article.collections }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400">收藏数</div>
            </div>
          </div>
        </div>

        <!-- 文章内容 -->
        <div class="p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">文章内容</h2>
          <div class="prose dark:prose-invert max-w-none">
            <div class="text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-wrap">
              {{ article.content }}
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700/50 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-500 dark:text-gray-400">
              最后更新：{{ formatDate(article.updatedAt) }}
            </span>
            <div class="flex items-center gap-2">
              <button
                @click="toggleTop"
                class="px-4 py-2 text-sm rounded-lg transition-colors"
                :class="article.isTop
                  ? 'bg-gray-200 dark:bg-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-500'
                  : 'bg-red-500 text-white hover:bg-red-600'"
              >
                {{ article.isTop ? '取消置顶' : '设为置顶' }}
              </button>
              <button
                v-if="article.reviewStatus === 'pending'"
                @click="approveArticle"
                class="px-4 py-2 text-sm bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
              >
                通过审核
              </button>
              <button
                v-if="article.reviewStatus === 'pending'"
                @click="rejectArticle"
                class="px-4 py-2 text-sm bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition-colors"
              >
                拒绝
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
