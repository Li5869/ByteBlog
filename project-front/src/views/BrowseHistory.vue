<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {interactionApi, isLoggedIn} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatAbsoluteDate} from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const historyList = ref([])
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

const hasMore = computed(() => {
  return pagination.value.current < pagination.value.pages
})


const fetchBrowseHistory = async (isLoadMore = false) => {
  if (!isLoggedIn()) {
    router.push({ name: 'Home', query: { login: 'true', redirect: '/browse-history' } })
    return
  }

  if (isLoadMore) {
    pagination.value.current++
  }

  loading.value = true
  try {
    const data = await interactionApi.getBrowseHistory(pagination.value.current, pagination.value.size)
    
    if (isLoadMore) {
      historyList.value = [...historyList.value, ...data.records]
    } else {
      historyList.value = data.records || []
    }
    
    pagination.value = {
      current: data.current || 1,
      size: data.size || 10,
      total: data.total || 0,
      pages: data.pages || 0
    }
  } catch (error) {
    console.error('获取浏览历史失败:', error)
  } finally {
    loading.value = false
  }
}

const goToArticle = (articleId, isDeleted) => {
  if (isDeleted) {
    toast.error('该文章已被删除，无法查看')
    return
  }
  router.push({ name: 'Article', params: { id: articleId } })
}

const loadMore = () => {
  if (!loading.value && hasMore.value) {
    fetchBrowseHistory(true)
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchBrowseHistory()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 pb-16 md:pb-0">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="flex items-center gap-4 mb-6">
        <button 
          @click="goBack"
          class="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div class="flex-1">
          <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">浏览历史</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">共 {{ pagination.total }} 条记录</p>
        </div>
      </div>

      <div v-if="loading && historyList.length === 0" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
      </div>

      <div v-else-if="historyList.length === 0" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-200 dark:border-gray-700 p-12 text-center">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-gray-500 dark:text-gray-400 mb-2">暂无浏览历史</p>
        <p class="text-sm text-gray-400 dark:text-gray-500">您浏览过的文章将在这里显示</p>
        <RouterLink 
          to="/blog" 
          class="inline-block mt-4 px-4 py-2 bg-primary-500 text-white rounded-lg text-sm hover:bg-primary-600 transition-colors"
        >
          去浏览文章
        </RouterLink>
      </div>

      <div v-else class="space-y-4">
        <div 
          v-for="item in historyList" 
          :key="item.id"
          @click="goToArticle(item.articleId, item.isDeleted)"
          class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-4 hover:shadow-md hover:border-primary-200 dark:hover:border-primary-700 transition-all cursor-pointer"
          :class="{ 'opacity-60': item.isDeleted }"
        >
          <div class="flex gap-4">
            <div 
              v-if="item.articleCover && !item.isDeleted"
              class="flex-shrink-0 w-24 h-24 sm:w-32 sm:h-24 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-700"
            >
              <img 
                :src="item.articleCover" 
                :alt="item.articleTitle"
                class="w-full h-full object-cover"
              />
            </div>
            <div 
              v-else
              class="flex-shrink-0 w-24 h-24 sm:w-32 sm:h-24 rounded-lg flex items-center justify-center"
              :class="item.isDeleted ? 'bg-gray-200 dark:bg-gray-700' : 'bg-gradient-to-br from-primary-400 to-orange-400'"
            >
              <svg v-if="item.isDeleted" class="w-8 h-8 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              <svg v-else class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            
            <div class="flex-1 min-w-0">
              <h3 class="text-base sm:text-lg font-semibold mb-2 line-clamp-2 transition-colors" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-900 dark:text-white hover:text-primary-500 dark:hover:text-primary-400'">
                {{ item.isDeleted ? '已删除文章' : item.articleTitle }}
              </h3>
              <p class="text-sm line-clamp-2 mb-2" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                {{ item.isDeleted ? '该文章已被删除，无法查看' : (item.summary || '暂无摘要') }}
              </p>
              <div class="flex items-center gap-3 text-xs text-gray-400 dark:text-gray-500">
                <div v-if="!item.isDeleted" class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  <span>{{ item.viewCount || 0 }}</span>
                </div>
                <div v-if="!item.isDeleted" class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  <span>{{ item.likeCount || 0 }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>{{ formatAbsoluteDate(item.browseTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="flex justify-center pt-4">
          <button 
            @click="loadMore"
            :disabled="loading"
            class="px-6 py-2 text-sm text-primary-500 hover:text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20 rounded-lg transition-colors disabled:opacity-50"
          >
            <span v-if="loading" class="flex items-center gap-2">
              <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              加载中...
            </span>
            <span v-else>加载更多</span>
          </button>
        </div>

        <div v-else-if="historyList.length > 0" class="text-center py-4 text-sm text-gray-400 dark:text-gray-500">
          已加载全部记录
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
