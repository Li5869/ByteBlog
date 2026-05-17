<script setup>
import {onMounted, onUnmounted, ref, watch} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {articleApi, columnApi, interactionApi, isLoggedIn, userApi} from '@/utils/request'
import {useUserStore} from '@/stores/user'
import {DEFAULT_AVATAR, DEFAULT_COVER, getAvatar} from '@/utils/defaults'
import {toast} from '@/utils/toast'
import {formatNumber} from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const goToUserHome = (userId) => {
  const currentUserId = userStore.state.userInfo?.id
  if (currentUserId && String(userId) === String(currentUserId)) {
    router.push('/mine')
  } else {
    router.push(`/user/${userId}`)
  }
}

const currentSlide = ref(0)
const slideInterval = ref(null)

const banners = ref([])
const articles = ref([])
const hotArticles = ref([])
const activeUsers = ref([])
const hotColumns = ref([])

const loading = ref({
  banners: false,
  articles: false,
  hotArticles: false,
  activeUsers: false,
  hotColumns: false
})

const isRefreshing = ref(false)

const fetchBanners = async () => {
  loading.value.banners = true
  try {
    banners.value = await articleApi.getBanners(3)
  } catch (error) {
    console.error('获取轮播图失败:', error)
  } finally {
    loading.value.banners = false
  }
}

const fetchRandomArticles = async () => {
  loading.value.articles = true
  try {
    articles.value = await articleApi.getRandomArticles(6)
  } catch (error) {
    console.error('获取随机文章失败:', error)
  } finally {
    loading.value.articles = false
  }
}

const refreshArticles = async () => {
  isRefreshing.value = true
  try {
    articles.value = await articleApi.getRandomArticles(6)
  } catch (error) {
    console.error('刷新文章失败:', error)
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 300)
  }
}

const fetchHotArticles = async () => {
  loading.value.hotArticles = true
  try {
    hotArticles.value = await articleApi.getHotArticles(5)
  } catch (error) {
    console.error('获取热门文章失败:', error)
  } finally {
    loading.value.hotArticles = false
  }
}

const fetchActiveUsers = async () => {
  loading.value.activeUsers = true
  try {
    activeUsers.value = await userApi.getActiveUsers(4)
  } catch (error) {
    console.error('获取活跃博主失败:', error)
  } finally {
    loading.value.activeUsers = false
  }
}

const fetchHotColumns = async () => {
  loading.value.hotColumns = true
  try {
    hotColumns.value = await columnApi.getHotColumns()
  } catch (error) {
    console.error('获取热门专栏失败:', error)
  } finally {
    loading.value.hotColumns = false
  }
}

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % banners.value.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + banners.value.length) % banners.value.length
}

const goToSlide = (index) => {
  currentSlide.value = index
}


const toggleFollow = async (user) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  try {
    const currentFollowState = user.isFollowing
    await interactionApi.toggleFollow(user.id, currentFollowState)
    user.isFollowing = !currentFollowState
    if (!currentFollowState) {
      user.followers++
      toast.success('关注成功')
    } else {
      user.followers--
      toast.success('已取消关注')
    }
  } catch (error) {
    console.error('关注操作失败:', error)
    toast.error(error.message || '操作失败')
  }
}

watch(banners, (newBanners) => {
  if (newBanners.length > 0 && !slideInterval.value) {
    slideInterval.value = setInterval(nextSlide, 5000)
  }
})

onMounted(() => {
  fetchBanners()
  fetchRandomArticles()
  fetchHotArticles()
  fetchActiveUsers()
  fetchHotColumns()
})

onUnmounted(() => {
  if (slideInterval.value) {
    clearInterval(slideInterval.value)
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div v-if="banners.length > 0" class="relative h-[50vh] sm:h-[60vh] lg:h-[70vh] overflow-hidden">
      <RouterLink 
        v-for="(banner, index) in banners" 
        :key="banner.id"
        :to="`/article/${banner.articleId}`"
        class="absolute inset-0 transition-all duration-700 ease-out cursor-pointer"
        :class="index === currentSlide ? 'opacity-100 scale-100 pointer-events-auto' : 'opacity-0 scale-105 pointer-events-none'"
      >
        <img :src="banner.cover" :alt="banner.title" class="w-full h-full object-cover" />
        <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent"></div>
        <div class="absolute inset-0 bg-gradient-to-r from-black/30 to-transparent"></div>
        
        <div class="absolute bottom-0 left-0 right-0 p-6 sm:p-10 lg:p-16">
          <div class="max-w-7xl mx-auto">
            <div class="flex items-center gap-3 mb-4">
              <span class="px-4 py-1.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white text-xs font-semibold rounded-full shadow-lg shadow-primary-500/30">
                {{ banner.categoryName || '热门推荐' }}
              </span>
              <span class="px-3 py-1 bg-white/10 backdrop-blur-md text-white text-xs rounded-full border border-white/20">
                精选内容
              </span>
            </div>
            <h2 class="text-2xl sm:text-3xl lg:text-5xl font-bold text-white mb-3 leading-tight max-w-3xl">
              {{ banner.title }}
            </h2>
            <p class="text-sm sm:text-base text-white/70 hidden sm:block line-clamp-2 max-w-2xl">
              {{ banner.summary }}
            </p>
          </div>
        </div>
      </RouterLink>

      <div class="absolute top-1/2 -translate-y-1/2 left-4 lg:left-8 z-10">
        <button 
          @click="prevSlide"
          class="w-12 h-12 lg:w-14 lg:h-14 bg-white/10 backdrop-blur-xl hover:bg-white/20 text-white rounded-2xl flex items-center justify-center transition-all duration-300 border border-white/20 shadow-xl hover:scale-110"
        >
          <svg class="w-5 h-5 lg:w-6 lg:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      </div>
      <div class="absolute top-1/2 -translate-y-1/2 right-4 lg:right-8 z-10">
        <button 
          @click="nextSlide"
          class="w-12 h-12 lg:w-14 lg:h-14 bg-white/10 backdrop-blur-xl hover:bg-white/20 text-white rounded-2xl flex items-center justify-center transition-all duration-300 border border-white/20 shadow-xl hover:scale-110"
        >
          <svg class="w-5 h-5 lg:w-6 lg:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      <div class="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-2 z-10">
        <button 
          v-for="(_, index) in banners" 
          :key="index"
          @click="goToSlide(index)"
          class="h-1.5 rounded-full transition-all duration-300"
          :class="index === currentSlide ? 'bg-white w-8 shadow-lg shadow-white/30' : 'bg-white/40 hover:bg-white/60 w-1.5'"
        ></button>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-12">
      <div class="flex flex-col lg:flex-row gap-8">
        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h2 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">发现好文</h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">精选优质内容，为你推荐</p>
            </div>
            <button 
              @click="refreshArticles"
              :disabled="isRefreshing"
              class="flex items-center gap-2 px-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-all text-sm font-medium shadow-sm hover:shadow disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg 
                class="w-4 h-4 transition-transform" 
                :class="{ 'animate-spin': isRefreshing }"
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              换一批
            </button>
          </div>

          <div v-if="loading.articles" class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div v-for="i in 6" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm">
              <div class="h-44 bg-gray-200 dark:bg-gray-700 animate-pulse"></div>
              <div class="p-5 space-y-3">
                <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-1/2"></div>
              </div>
            </div>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <RouterLink 
              v-for="article in articles" 
              :key="article.id"
              :to="`/article/${article.id}`"
              class="group bg-white dark:bg-gray-800 rounded-2xl shadow-sm hover:shadow-xl border border-gray-100 dark:border-gray-700/50 overflow-hidden transition-all duration-300 hover:-translate-y-1"
            >
              <div class="relative h-44 overflow-hidden">
                <img 
                  :src="article.cover || DEFAULT_COVER" 
                  :alt="article.title"
                  class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                />
                
                <div class="absolute top-3 left-3 flex gap-2">
                  <span 
                    v-if="article.isTop" 
                    class="px-2.5 py-1 bg-gradient-to-r from-red-500 to-rose-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-red-500/30"
                  >置顶</span>
                  <span 
                    v-if="article.isHot" 
                    class="px-2.5 py-1 bg-gradient-to-r from-orange-500 to-amber-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-orange-500/30"
                  >热门</span>
                </div>
                
                <div class="absolute top-3 right-3">
                  <span class="px-3 py-1.5 bg-black/40 backdrop-blur-md text-white text-xs font-medium rounded-lg border border-white/10">
                    {{ article.category }}
                  </span>
                </div>
              </div>
              
              <div class="p-5">
                <h3 class="font-bold text-gray-900 dark:text-white mb-2 line-clamp-2 group-hover:text-primary-500 transition-colors text-lg">
                  {{ article.title }}
                </h3>
                <p class="text-sm text-gray-500 dark:text-gray-400 mb-4 line-clamp-2 leading-relaxed">
                  {{ article.summary }}
                </p>
                
                <div class="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-gray-700">
                  <div class="flex items-center gap-2">
                    <img 
                      :src="getAvatar(article.authorId || article.author?.id, article.author?.avatar)" 
                      :alt="article.author.name"
                      class="w-6 h-6 rounded-full"
                    />
                    <span class="text-sm text-gray-600 dark:text-gray-400 font-medium">{{ article.author.name }}</span>
                  </div>
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
                      {{ article.likes }}
                    </span>
                  </div>
                </div>
              </div>
            </RouterLink>
          </div>
        </div>

        <div class="lg:w-80 xl:w-96 flex-shrink-0 space-y-6">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
            <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-rose-500 flex items-center justify-center shadow-lg shadow-red-500/20">
                <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <div>
                <h3 class="font-bold text-gray-900 dark:text-white">热门文章</h3>
                <p class="text-xs text-gray-400">最受欢迎的内容</p>
              </div>
            </div>
            
            <div v-if="loading.hotArticles" class="p-6 space-y-4">
              <div v-for="i in 5" :key="i" class="flex items-center gap-3">
                <div class="w-6 h-6 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-full"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-1/2"></div>
                </div>
              </div>
            </div>
            
            <div v-else-if="hotArticles.length > 0" class="divide-y divide-gray-50 dark:divide-gray-700/50">
              <RouterLink 
                v-for="(article, index) in hotArticles" 
                :key="article.id"
                :to="`/article/${article.id}`"
                class="flex items-start gap-4 p-4 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-all group"
              >
                <span 
                  class="flex-shrink-0 w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold transition-transform group-hover:scale-110"
                  :class="{
                    'bg-gradient-to-br from-red-500 to-rose-500 text-white shadow-lg shadow-red-500/30': index === 0,
                    'bg-gradient-to-br from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-500/30': index === 1,
                    'bg-gradient-to-br from-yellow-500 to-orange-400 text-white shadow-lg shadow-yellow-500/30': index === 2,
                    'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400': index > 2
                  }"
                >
                  {{ index + 1 }}
                </span>
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-primary-500 line-clamp-2 transition-colors font-medium leading-relaxed">
                    {{ article.title }}
                  </p>
                  <p class="text-xs text-gray-400 mt-1.5 flex items-center gap-1">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ formatNumber(article.views) }} 阅读
                  </p>
                </div>
              </RouterLink>
            </div>
            <div v-else class="p-8 text-center text-gray-400 text-sm">
              暂无热门文章
            </div>
          </div>

          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
            <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/20">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <div>
                <h3 class="font-bold text-gray-900 dark:text-white">活跃博主</h3>
                <p class="text-xs text-gray-400">优质内容创作者</p>
              </div>
            </div>
            
            <div v-if="loading.activeUsers" class="p-6 space-y-4">
              <div v-for="i in 4" :key="i" class="flex items-center gap-3">
                <div class="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-24"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-32"></div>
                </div>
              </div>
            </div>
            
            <div v-else-if="activeUsers.length > 0" class="p-4 space-y-3">
              <div 
                v-for="user in activeUsers" 
                :key="user.id"
                class="flex items-center gap-3 p-2 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors"
              >
                <img 
                  :src="getAvatar(user.id, user.avatar)" 
                  :alt="user.name"
                  class="w-11 h-11 rounded-full cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-gray-100 dark:ring-gray-700"
                  @click="goToUserHome(user.id)"
                />
                <div class="flex-1 min-w-0">
                  <p 
                    class="text-sm font-medium text-gray-900 dark:text-white cursor-pointer hover:text-primary-500 transition-colors truncate"
                    @click="goToUserHome(user.id)"
                  >
                    {{ user.name }}
                  </p>
                  <p class="text-xs text-gray-400">{{ user.articles }} 篇文章 · {{ formatNumber(user.followers) }} 粉丝</p>
                </div>
                <button 
                  v-if="String(user.id) !== String(userStore.state.userInfo?.id)"
                  @click.stop="toggleFollow(user)"
                  :class="user.isFollowing 
                    ? 'px-3 py-1.5 text-xs text-gray-500 dark:text-gray-400 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors font-medium'
                    : 'px-3 py-1.5 text-xs text-primary-500 border border-primary-200 dark:border-primary-800 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors font-medium'"
                >
                  {{ user.isFollowing ? '已关注' : '关注' }}
                </button>
              </div>
            </div>
            <div v-else class="p-8 text-center text-gray-400 text-sm">
              暂无活跃博主
            </div>
          </div>

          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-5">
            <div class="flex items-center gap-3 mb-4">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center shadow-lg shadow-purple-500/20">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
              <div class="flex-1">
                <h3 class="font-bold text-gray-900 dark:text-white">热门专栏</h3>
                <p class="text-xs text-gray-400">发现优质专栏</p>
              </div>
              <RouterLink to="/columns" class="text-xs text-purple-500 hover:text-purple-600 dark:text-purple-400 dark:hover:text-purple-300 font-medium">
                查看全部 →
              </RouterLink>
            </div>
            
            <div v-if="loading.hotColumns" class="space-y-3">
              <div v-for="i in 3" :key="i" class="flex gap-3 p-3 bg-gray-50 dark:bg-gray-700/30 rounded-xl">
                <div class="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse flex-shrink-0"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-3/4"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-1/2"></div>
                </div>
              </div>
            </div>
            
            <div v-else-if="hotColumns.length > 0" class="space-y-3">
              <RouterLink
                v-for="column in hotColumns" 
                :key="column.id"
                :to="`/column/${column.id}`"
                class="group flex gap-3 p-3 bg-gray-50 dark:bg-gray-700/30 rounded-xl hover:bg-purple-50 dark:hover:bg-purple-900/20 transition-all cursor-pointer"
              >
                <div class="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0">
                  <img
                    v-if="column.cover"
                    :src="column.cover"
                    :alt="column.title"
                    class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                  />
                  <div v-else class="w-full h-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
                    <svg class="w-6 h-6 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-gray-900 dark:text-white text-sm mb-1 group-hover:text-purple-500 transition-colors line-clamp-1">
                    {{ column.title }}
                  </h4>
                  <p class="text-xs text-gray-500 dark:text-gray-400 line-clamp-1 mb-2">
                    {{ column.description }}
                  </p>
                  <div class="flex items-center gap-2 text-xs text-gray-400">
                    <img
                      :src="column.authorAvatar || DEFAULT_AVATAR"
                      :alt="column.authorName"
                      class="w-4 h-4 rounded-full object-cover"
                    />
                    <span>{{ column.authorName }}</span>
                    <span>·</span>
                    <span>{{ column.articlesCount }}篇</span>
                    <span>·</span>
                    <span>{{ formatNumber(column.views) }}阅读</span>
                  </div>
                </div>
              </RouterLink>
            </div>
            <div v-else class="text-center text-gray-400 text-sm py-8">
              <div class="w-16 h-16 mx-auto mb-3 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
              暂无热门专栏
            </div>
          </div>
        </div>
      </div>
    </div>

    <RouterLink 
      to="/create-article"
      class="fixed bottom-20 right-4 md:bottom-8 md:right-8 w-14 h-14 rounded-2xl bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-xl shadow-primary-500/30 hover:shadow-2xl hover:shadow-primary-500/40 hover:scale-110 transition-all duration-300 flex items-center justify-center z-40 group"
    >
      <svg class="w-6 h-6 group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
    </RouterLink>
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
