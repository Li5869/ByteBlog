<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import {columnApi, isLoggedIn} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatAbsoluteDate, formatNumber} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const columnId = route.params.id

const loading = ref(true)
const column = ref(null)
const articles = ref([])
const subscribing = ref(false)

const fetchColumnDetail = async () => {
  loading.value = true
  try {
    const result = await columnApi.getColumnDetail(columnId)
    column.value = result
    articles.value = result.articles || []
  } catch (error) {
    console.error('获取专栏详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSubscribe = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }

  subscribing.value = true
  try {
    if (column.value.isSubscribed) {
      await columnApi.unsubscribeColumn(columnId)
      column.value.isSubscribed = false
      column.value.subscriptionCount--
      toast.success('已取消订阅')
    } else {
      await columnApi.subscribeColumn(columnId)
      column.value.isSubscribed = true
      column.value.subscriptionCount++
      toast.success('订阅成功')
    }
  } catch (error) {
    console.error('订阅操作失败:', error)
    toast.error(error.message || '操作失败，请稍后重试')
  } finally {
    subscribing.value = false
  }
}

const goToArticle = (articleId) => {
  router.push({ name: 'Article', params: { id: articleId } })
}

const goToAuthor = () => {
  router.push({ name: 'UserHome', params: { id: column.value.authorId } })
}


onMounted(() => {
  fetchColumnDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div v-if="loading" class="flex justify-center items-center min-h-screen">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
    </div>

    <template v-else-if="column">
      <div class="relative">
        <div class="h-64 sm:h-80 relative overflow-hidden">
          <img
            v-if="column.cover"
            :src="column.cover"
            :alt="column.title"
            class="w-full h-full object-cover"
          />
          <div v-else class="w-full h-full bg-gradient-to-br from-purple-500 to-pink-500"></div>
          <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent"></div>
          
          <div class="absolute bottom-0 left-0 right-0 p-6 sm:p-8">
            <div class="max-w-6xl mx-auto">
              <div class="flex items-center justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-3">
                    <span class="px-3 py-1 bg-purple-500 text-white text-xs font-semibold rounded-full">
                      专栏
                    </span>
                    <span class="px-3 py-1 bg-white/20 backdrop-blur-sm text-white text-xs font-medium rounded-full">
                      {{ column.articlesCount }} 篇文章
                    </span>
                    <span class="px-3 py-1 bg-white/20 backdrop-blur-sm text-white text-xs font-medium rounded-full">
                      {{ formatNumber(column.subscriptionCount) }} 订阅
                    </span>
                  </div>
                  <h1 class="text-2xl sm:text-4xl font-bold text-white mb-3">{{ column.title }}</h1>
                  <p class="text-white/80 text-sm sm:text-base max-w-3xl line-clamp-2">
                    {{ column.description }}
                  </p>
                </div>
                
                <!-- 订阅按钮 - 只在不是自己的专栏时显示 -->
                <div v-if="!column.isAuthor" class="flex-shrink-0">
                  <button
                    @click="handleSubscribe"
                    :disabled="subscribing"
                    class="px-6 py-3 rounded-xl text-sm font-semibold transition-all flex items-center gap-2"
                    :class="column.isSubscribed
                      ? 'bg-white/20 backdrop-blur-sm text-white hover:bg-white/30'
                      : 'bg-white text-purple-600 hover:bg-white/90 shadow-lg'"
                  >
                    <svg v-if="subscribing" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <svg v-else-if="column.isSubscribed" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    {{ subscribing ? '处理中...' : (column.isSubscribed ? '已订阅' : '订阅') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div class="lg:col-span-8">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
              <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
                <h2 class="text-lg font-bold text-gray-900 dark:text-white flex items-center gap-2">
                  <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  专栏文章
                  <span class="text-sm font-normal text-gray-400">({{ column.articlesCount }}篇)</span>
                </h2>
              </div>

              <div v-if="articles.length === 0" class="text-center py-12">
                <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                  <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <p class="text-gray-500 dark:text-gray-400">暂无文章</p>
              </div>

              <div v-else class="divide-y divide-gray-100 dark:divide-gray-700">
                <div
                  v-for="(article, index) in articles"
                  :key="article.id"
                  @click="goToArticle(article.id)"
                  class="group p-5 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer"
                >
                  <div class="flex gap-4">
                    <div class="flex-shrink-0 w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold text-sm">
                      {{ index + 1 }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold text-gray-900 dark:text-white mb-2 group-hover:text-purple-500 transition-colors line-clamp-1">
                        {{ article.title }}
                      </h3>
                      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">
                        {{ article.summary }}
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
                    <div v-if="article.cover" class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden hidden sm:block">
                      <img :src="article.cover" :alt="article.title" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="lg:col-span-4">
            <div class="sticky top-20 space-y-6">
              <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
                <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700">
                  <h3 class="font-bold text-gray-900 dark:text-white">作者信息</h3>
                </div>
                <div class="p-5">
                  <div
                    class="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity"
                    @click="goToAuthor"
                  >
                    <img
                      :src="column.authorAvatar || DEFAULT_AVATAR"
                      :alt="column.authorName"
                      class="w-12 h-12 rounded-full object-cover border-2 border-purple-200 dark:border-purple-800"
                    />
                    <div>
                      <div class="font-semibold text-gray-900 dark:text-white">{{ column.authorName }}</div>
                      <div class="text-sm text-gray-500 dark:text-gray-400">{{ column.authorBio || '这个人很懒，什么都没写' }}</div>
                    </div>
                  </div>
                  
                  <div class="grid grid-cols-3 gap-4 mt-5 pt-5 border-t border-gray-100 dark:border-gray-700">
                    <div class="text-center">
                      <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ column.articlesCount }}</div>
                      <div class="text-xs text-gray-400">文章</div>
                    </div>
                    <div class="text-center">
                      <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ formatNumber(column.subscriptionCount) }}</div>
                      <div class="text-xs text-gray-400">订阅</div>
                    </div>
                    <div class="text-center">
                      <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ formatNumber(column.views) }}</div>
                      <div class="text-xs text-gray-400">浏览</div>
                    </div>
                  </div>
                  
                  <!-- 订阅按钮 - 只在不是自己的专栏时显示 -->
                  <div v-if="!column.isAuthor" class="mt-5 pt-5 border-t border-gray-100 dark:border-gray-700">
                    <button
                      @click="handleSubscribe"
                      :disabled="subscribing"
                      class="w-full py-3 rounded-xl text-sm font-semibold transition-all flex items-center justify-center gap-2"
                      :class="column.isSubscribed
                        ? 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                        : 'bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:from-purple-600 hover:to-pink-600 shadow-lg'"
                    >
                      <svg v-if="subscribing" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      <svg v-else-if="column.isSubscribed" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                      </svg>
                      {{ subscribing ? '处理中...' : (column.isSubscribed ? '已订阅' : '订阅专栏') }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
                <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700">
                  <h3 class="font-bold text-gray-900 dark:text-white">专栏信息</h3>
                </div>
                <div class="p-5 space-y-3">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-500 dark:text-gray-400">创建时间</span>
                    <span class="text-gray-900 dark:text-white">{{ formatAbsoluteDate(column.createdAt) }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-500 dark:text-gray-400">更新时间</span>
                    <span class="text-gray-900 dark:text-white">{{ formatAbsoluteDate(column.updatedAt) }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-500 dark:text-gray-400">文章数量</span>
                    <span class="text-gray-900 dark:text-white">{{ column.articlesCount }} 篇</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-500 dark:text-gray-400">订阅数量</span>
                    <span class="text-gray-900 dark:text-white">{{ formatNumber(column.subscriptionCount) }}</span>
                  </div>
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-500 dark:text-gray-400">浏览量</span>
                    <span class="text-gray-900 dark:text-white">{{ formatNumber(column.views) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
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
