<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {NCheckbox} from 'naive-ui'
import {toast} from '@/utils/toast'
import {columnApi} from '@/utils/request'
import {formatAbsoluteDate, formatNumber} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const columnId = route.params.id

const loading = ref(false)
const column = ref(null)
const articles = ref([])
const availableArticles = ref([])
const selectedAvailableArticles = ref([])
const selectedColumnArticles = ref([])
const showAddModal = ref(false)
const submitting = ref(false)

const fetchColumnDetail = async () => {
  loading.value = true
  try {
    const result = await columnApi.getColumnDetail(columnId)
    column.value = result
    articles.value = result.articles || []
  } catch (error) {
    console.error('获取专栏详情失败:', error)
    toast.error('获取专栏详情失败')
  } finally {
    loading.value = false
  }
}

const fetchAvailableArticles = async () => {
  try {
    availableArticles.value = await columnApi.getAvailableArticles(columnId)
  } catch (error) {
    console.error('获取可添加文章失败:', error)
    toast.error('获取可添加文章失败')
  }
}

const openAddModal = async () => {
  selectedAvailableArticles.value = []
  showAddModal.value = true
  await fetchAvailableArticles()
}

const handleAddArticles = async () => {
  if (selectedAvailableArticles.value.length === 0) {
    toast.error('请选择要添加的文章')
    return
  }

  submitting.value = true
  try {
    await columnApi.addArticles(columnId, selectedAvailableArticles.value)
    toast.success('添加成功')
    showAddModal.value = false
    fetchColumnDetail()
  } catch (error) {
    console.error('添加文章失败:', error)
    toast.error(error.message || '添加失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleRemoveArticles = async () => {
  if (selectedColumnArticles.value.length === 0) {
    toast.error('请选择要移除的文章')
    return
  }

  submitting.value = true
  try {
    await columnApi.removeArticles(columnId, selectedColumnArticles.value)
    toast.success('移除成功')
    selectedColumnArticles.value = []
    fetchColumnDetail()
  } catch (error) {
    console.error('移除文章失败:', error)
    toast.error(error.message || '移除失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const goToArticle = (articleId) => {
  router.push({ name: 'Article', params: { id: articleId } })
}

const goBack = () => {
  router.back()
}


onMounted(() => {
  fetchColumnDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="bg-gradient-to-r from-purple-500 via-pink-500 to-rose-500 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <button
              @click="goBack"
              class="p-2 bg-white/20 backdrop-blur-sm rounded-xl text-white hover:bg-white/30 transition-all"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 class="text-2xl sm:text-3xl font-bold text-white mb-2">管理专栏文章</h1>
              <p v-if="column" class="text-white/80 text-sm sm:text-base">{{ column.title }}</p>
            </div>
          </div>
          <button
            @click="openAddModal"
            class="px-5 py-2.5 bg-white text-purple-600 rounded-xl text-sm font-semibold hover:bg-white/90 transition-all shadow-lg hover:shadow-xl hover:-translate-y-0.5 flex items-center gap-2"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加文章
          </button>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div v-if="loading" class="space-y-4">
        <div v-for="i in 3" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
          <div class="flex gap-4">
            <div class="w-6 h-6 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
            <div class="flex-1 space-y-3">
              <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
              <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="articles.length === 0" class="text-center py-20">
        <div class="w-24 h-24 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
          <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <p class="text-gray-500 dark:text-gray-400 text-lg font-medium mb-2">暂无文章</p>
        <p class="text-sm text-gray-400 dark:text-gray-500 mb-6">点击上方"添加文章"按钮，将你的文章添加到专栏中</p>
        <button
          @click="openAddModal"
          class="px-6 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl text-sm font-semibold hover:from-purple-600 hover:to-pink-600 transition-all shadow-lg"
        >
          添加文章
        </button>
      </div>

      <template v-else>
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500 dark:text-gray-400">
              已选择 {{ selectedColumnArticles.length }} 篇文章
            </span>
          </div>
          <button
            v-if="selectedColumnArticles.length > 0"
            @click="handleRemoveArticles"
            :disabled="submitting"
            class="px-4 py-2 bg-red-500 text-white rounded-lg text-sm font-medium hover:bg-red-600 transition-colors disabled:opacity-50"
          >
            移除选中
          </button>
        </div>

        <div class="space-y-3">
          <div
            v-for="(article, index) in articles"
            :key="article.id"
            class="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700 hover:shadow-lg transition-all"
          >
            <div class="flex gap-4">
              <div class="flex items-center">
                <n-checkbox
                  :checked="selectedColumnArticles.includes(article.id)"
                  @update:checked="(val) => {
                    if (val) {
                      selectedColumnArticles.push(article.id)
                    } else {
                      selectedColumnArticles = selectedColumnArticles.filter(id => id !== article.id)
                    }
                  }"
                />
              </div>
              <div class="flex-shrink-0 w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold text-sm">
                {{ index + 1 }}
              </div>
              <div class="flex-1 min-w-0">
                <h3
                  class="font-semibold text-gray-900 dark:text-white mb-2 cursor-pointer hover:text-purple-500 transition-colors line-clamp-1"
                  @click="goToArticle(article.id)"
                >
                  {{ article.title }}
                </h3>
                <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">
                  {{ article.summary || '暂无摘要' }}
                </p>
                <div class="flex items-center gap-4 text-xs text-gray-400">
                  <span class="flex items-center gap-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ formatNumber(article.views) }}
                  </span>
                  <span class="flex items-center gap-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                    {{ formatNumber(article.likes) }}
                  </span>
                  <span class="flex items-center gap-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    {{ formatNumber(article.comments) }}
                  </span>
                  <span>{{ formatAbsoluteDate(article.createdAt) }}</span>
                </div>
              </div>
              <div v-if="article.cover" class="flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden hidden sm:block">
                <img :src="article.cover" :alt="article.title" class="w-full h-full object-cover" />
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 添加文章弹窗 -->
    <div
      v-if="showAddModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      @click.self="showAddModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
          <h3 class="text-lg font-bold text-gray-900 dark:text-white">添加文章到专栏</h3>
          <button
            @click="showAddModal = false"
            class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          >
            <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="p-6 overflow-y-auto max-h-[60vh]">
          <div v-if="availableArticles.length === 0" class="text-center py-12">
            <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <p class="text-gray-500 dark:text-gray-400">暂无可添加的文章</p>
            <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">你发布的文章都已在此专栏中</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="article in availableArticles"
              :key="article.id"
              @click="selectedAvailableArticles.includes(article.id) 
                ? selectedAvailableArticles = selectedAvailableArticles.filter(id => id !== article.id)
                : selectedAvailableArticles.push(article.id)"
              class="p-4 rounded-xl border-2 cursor-pointer transition-all"
              :class="selectedAvailableArticles.includes(article.id)
                ? 'border-purple-500 bg-purple-50 dark:bg-purple-900/20'
                : 'border-gray-100 dark:border-gray-700 hover:border-purple-200 dark:hover:border-purple-800'"
            >
              <div class="flex gap-3">
                <div class="flex items-center">
                  <div
                    class="w-5 h-5 rounded border-2 flex items-center justify-center transition-colors"
                    :class="selectedAvailableArticles.includes(article.id)
                      ? 'bg-purple-500 border-purple-500'
                      : 'border-gray-300 dark:border-gray-600'"
                  >
                    <svg v-if="selectedAvailableArticles.includes(article.id)" class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium text-gray-900 dark:text-white mb-1 line-clamp-1">{{ article.title }}</h4>
                  <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1">{{ article.summary || '暂无摘要' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="px-6 py-4 border-t border-gray-100 dark:border-gray-700 flex items-center justify-between">
          <span class="text-sm text-gray-500 dark:text-gray-400">
            已选择 {{ selectedAvailableArticles.length }} 篇文章
          </span>
          <div class="flex gap-3">
            <button
              @click="showAddModal = false"
              class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="handleAddArticles"
              :disabled="selectedAvailableArticles.length === 0 || submitting"
              class="px-4 py-2 bg-purple-500 text-white rounded-lg font-medium hover:bg-purple-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ submitting ? '添加中...' : '添加' }}
            </button>
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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
