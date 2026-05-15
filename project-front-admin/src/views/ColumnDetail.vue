<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {columnApi} from '../utils/request.js'
import {formatAbsoluteDate} from '@/utils/format'

const route = useRoute()
const router = useRouter()

const column = ref(null)
const loading = ref(false)

const columnId = computed(() => route.params.id)

const getStatusText = (status) => {
  return status === 1 ? '已发布' : '草稿'
}

const getStatusClass = (status) => {
  return status === 1
    ? 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400'
    : 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400'
}

const fetchColumnDetail = async () => {
  loading.value = true
  try {
    const result = await columnApi.detail(columnId.value)
    column.value = result
  } catch (error) {
    console.error('获取专栏详情失败:', error)
    alert(error.message || '获取专栏详情失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  if (!confirm(`确定要删除专栏"${column.value.title}"吗？删除后将无法恢复。`)) {
    return
  }
  try {
    await columnApi.delete(column.value.id)
    alert('删除成功')
    router.push('/columns')
  } catch (error) {
    console.error('删除专栏失败:', error)
    alert(error.message || '删除失败')
  }
}

const handleStatusChange = async () => {
  const newStatus = column.value.status === 1 ? 0 : 1
  try {
    await columnApi.update(column.value.id, { status: newStatus })
    alert('状态更新成功')
    fetchColumnDetail()
  } catch (error) {
    console.error('更新状态失败:', error)
    alert(error.message || '更新状态失败')
  }
}

const goBack = () => {
  router.push('/columns')
}

const viewArticle = (articleId) => {
  router.push(`/articles/${articleId}`)
}

onMounted(() => {
  fetchColumnDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="mb-6">
        <button
          @click="goBack"
          class="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          <span>返回专栏列表</span>
        </button>
      </div>

      <div v-if="loading" class="bg-white dark:bg-gray-800 rounded-lg shadow p-8 text-center">
        <svg class="animate-spin w-8 h-8 mx-auto text-red-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="mt-4 text-gray-500 dark:text-gray-400">加载中...</p>
      </div>

      <template v-else-if="column">
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <div class="relative h-48 bg-gradient-to-r from-red-500 to-orange-500">
            <img
              v-if="column.cover"
              :src="column.cover"
              :alt="column.title"
              class="w-full h-full object-cover opacity-50"
            />
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
            <div class="absolute bottom-0 left-0 right-0 p-6">
              <div class="flex items-center justify-between">
                <div>
                  <h1 class="text-2xl font-bold text-white">{{ column.title }}</h1>
                  <p class="mt-1 text-white/80 text-sm">{{ column.description || '暂无描述' }}</p>
                </div>
                <span
                  class="px-3 py-1 text-sm rounded-full cursor-pointer"
                  :class="getStatusClass(column.status)"
                  @click="handleStatusChange"
                  :title="column.status === 1 ? '点击切换为草稿' : '点击发布'"
                >
                  {{ getStatusText(column.status) }}
                </span>
              </div>
            </div>
          </div>

          <div class="p-6">
            <div class="flex flex-wrap items-center gap-6 mb-6 pb-6 border-b border-gray-200 dark:border-gray-700">
              <div class="flex items-center gap-3">
                <img
                  v-if="column.authorAvatar"
                  :src="column.authorAvatar"
                  :alt="column.authorName"
                  class="w-10 h-10 rounded-full object-cover"
                />
                <div
                  v-else
                  class="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-600 flex items-center justify-center"
                >
                  <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <div>
                  <div class="text-sm font-medium text-gray-900 dark:text-white">{{ column.authorName || '未知作者' }}</div>
                  <div class="text-xs text-gray-500 dark:text-gray-400">作者</div>
                </div>
              </div>

              <div class="flex items-center gap-6 text-sm text-gray-500 dark:text-gray-400">
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <span>{{ column.articlesCount || 0 }} 篇文章</span>
                </div>
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  <span>{{ column.views || 0 }} 次浏览</span>
                </div>
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  <span>{{ column.subscriptionCount || 0 }} 人订阅</span>
                </div>
              </div>

              <div class="ml-auto flex items-center gap-2">
                <button
                  @click="handleDelete"
                  class="px-4 py-2 text-red-500 border border-red-500 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                >
                  删除专栏
                </button>
              </div>
            </div>

            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">专栏文章</h2>
              
              <div v-if="!column.articles || column.articles.length === 0" class="py-8 text-center">
                <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p class="mt-2 text-gray-500 dark:text-gray-400">暂无文章</p>
              </div>

              <div v-else class="space-y-4">
                <div
                  v-for="article in column.articles"
                  :key="article.id"
                  class="flex items-center gap-4 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors cursor-pointer"
                  @click="viewArticle(article.id)"
                >
                  <img
                    v-if="article.cover"
                    :src="article.cover"
                    :alt="article.title"
                    class="w-16 h-12 rounded object-cover flex-shrink-0"
                  />
                  <div
                    v-else
                    class="w-16 h-12 rounded bg-gray-200 dark:bg-gray-600 flex items-center justify-center flex-shrink-0"
                  >
                    <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="text-sm font-medium text-gray-900 dark:text-white line-clamp-1">
                      {{ article.title }}
                    </div>
                    <div class="text-xs text-gray-500 dark:text-gray-400 line-clamp-1 mt-1">
                      {{ article.summary || '暂无摘要' }}
                    </div>
                  </div>
                  <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                    <span class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      {{ article.views || 0 }}
                    </span>
                    <span class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      {{ article.likes || 0 }}
                    </span>
                    <span class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                      </svg>
                      {{ article.comments || 0 }}
                    </span>
                    <span>{{ formatAbsoluteDate(article.createdAt) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-4">专栏信息</h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <div class="text-gray-500 dark:text-gray-400">专栏ID</div>
              <div class="text-gray-900 dark:text-white font-mono">{{ column.id }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">创建时间</div>
              <div class="text-gray-900 dark:text-white">{{ formatAbsoluteDate(column.createdAt) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">更新时间</div>
              <div class="text-gray-900 dark:text-white">{{ formatAbsoluteDate(column.updatedAt) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">作者ID</div>
              <div class="text-gray-900 dark:text-white font-mono">{{ column.authorId }}</div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="bg-white dark:bg-gray-800 rounded-lg shadow p-8 text-center">
        <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <p class="mt-4 text-gray-500 dark:text-gray-400">专栏不存在或已被删除</p>
        <button
          @click="goBack"
          class="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
        >
          返回列表
        </button>
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
