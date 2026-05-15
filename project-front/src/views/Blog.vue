<script setup>
import {computed, onMounted, ref} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {NPagination} from 'naive-ui'
import {articleApi, categoryApi, interactionApi, isLoggedIn, userApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {DEFAULT_COVER, getAvatar} from '@/utils/defaults'
import {useUserStore} from '@/stores/user'
import {formatNumber, formatRelativeDate} from '@/utils/format'

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

const handleStartWriting = () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  router.push('/create-article')
}

const categories = ref([])
const articles = ref([])
const hotArticles = ref([])
const activeUsers = ref([])
const followingUsers = ref([])

const activeCategory = ref(null)
const sortBy = ref('new')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const pagination = computed(() => ({
  current: current.value,
  size: size.value,
  total: total.value
}))

const fetchCategories = async () => {
  try {
    const data = await categoryApi.getCategories()
    categories.value = [
      { id: null, name: '全部', articlesCount: data.reduce((sum, cat) => sum + (Number(cat.articlesCount) || 0), 0) },
      ...data.map(cat => ({
        id: cat.id,
        name: cat.name,
        articlesCount: Number(cat.articlesCount) || 0
      }))
    ]
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchArticles = async () => {
  if (loading.value) return

  loading.value = true
  try {
    // 关注 Tab：展示关注博主的最新文章
    if (sortBy.value === 'follow') {
      await fetchFollowArticles()
      return
    }

    if (sortBy.value === 'recommend') {
      const data = await articleApi.getRandomArticles(size.value)
      articles.value = data
      total.value = data.length
    } else {
      const params = {
        current: current.value,
        size: size.value,
        orderBy: sortBy.value === 'new' ? 'created_at' : 'views'
      }

      if (activeCategory.value) {
        params.categoryId = activeCategory.value
      }

      const data = await articleApi.getArticlePage(params)
      articles.value = data.records
      total.value = data.total
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchFollowArticles = async () => {
  if (!isLoggedIn()) {
    articles.value = []
    total.value = 0
    return
  }
  
  const params = {
    current: current.value,
    size: size.value,
    orderBy: 'created_at',
    Follow: true
  }

  const data = await articleApi.getArticlePage(params)
  articles.value = data.records
  total.value = data.total
}

const fetchFollowingUsers = async () => {
  if (!isLoggedIn()) {
    followingUsers.value = []
    return
  }
  try {
    followingUsers.value = await interactionApi.getFollowingUsers()
  } catch (error) {
    console.error('获取关注用户列表失败:', error)
    followingUsers.value = []
  }
}

const fetchHotArticles = async () => {
  try {
    const data = await articleApi.getHotArticles(5)
    hotArticles.value = data.map(article => ({
      id: article.id,
      title: article.title,
      views: article.views
    }))
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}

const fetchActiveUsers = async () => {
  try {
    const data = await userApi.getActiveUsers(4)
    activeUsers.value = data.map(user => ({
      id: user.id,
      name: user.name,
      avatar: user.avatar,
      articles: user.articles,
      followers: user.followers
    }))
  } catch (error) {
    console.error('获取活跃博主失败:', error)
  }
}

const selectCategory = (category) => {
  activeCategory.value = category.id
  current.value = 1
  fetchArticles()
}

const handleSortChange = (sort) => {
  sortBy.value = sort
  current.value = 1
  fetchArticles()
}

const handlePageChange = (page) => {
  current.value = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}


onMounted(() => {
  fetchCategories()
  fetchArticles()
  fetchHotArticles()
  fetchActiveUsers()
  fetchFollowingUsers()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 lg:py-8">
      <div class="flex flex-col lg:flex-row gap-6">
        <div class="lg:w-60 flex-shrink-0 hidden lg:block">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden sticky top-20">
            <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
              </div>
              <h3 class="font-bold text-gray-900 dark:text-white">文章分类</h3>
            </div>
            <div class="p-2">
              <button 
                v-for="cat in categories" 
                :key="cat.id"
                @click="selectCategory(cat)"
                class="w-full flex items-center justify-between px-3 py-2.5 text-sm rounded-xl transition-all duration-200"
                :class="activeCategory === cat.id 
                  ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/30' 
                  : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
              >
                <span class="font-medium">{{ cat.name }}</span>
                <span 
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="activeCategory === cat.id 
                    ? 'bg-white/20 text-white' 
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'"
                >{{ cat.articlesCount }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="flex-1 min-w-0">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 mb-4 overflow-hidden">
            <div class="flex items-center justify-between p-3 sm:p-4">
              <div class="flex items-center gap-1 sm:gap-2">
                <button
                  v-for="item in [
                    { key: 'new', label: '最新', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
                    { key: 'hot', label: '最热', icon: 'M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z' },
                    { key: 'recommend', label: '推荐', icon: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z' },
                    { key: 'follow', label: '关注', icon: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z' }
                  ]"
                  :key="item.key"
                  @click="handleSortChange(item.key)"
                  class="flex items-center gap-1.5 px-3 sm:px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200"
                  :class="sortBy === item.key
                    ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/30'
                    : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon" />
                  </svg>
                  <span class="hidden sm:inline">{{ item.label }}</span>
                </button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="space-y-4">
            <div v-for="i in 5" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-4">
              <div class="flex gap-4">
                <div class="flex-1 space-y-3">
                  <div class="flex gap-2">
                    <div class="h-5 w-12 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    <div class="h-5 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                  </div>
                  <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                  <div class="flex gap-4">
                    <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    <div class="h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                  </div>
                </div>
                <div class="w-32 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse hidden sm:block"></div>
              </div>
            </div>
          </div>

          <div v-else class="space-y-4">
            <RouterLink 
              v-for="article in articles" 
              :key="article.id"
              :to="`/article/${article.id}`"
              class="block bg-white dark:bg-gray-800 rounded-2xl shadow-sm hover:shadow-xl border border-gray-100 dark:border-gray-700/50 overflow-hidden transition-all duration-300 hover:-translate-y-1 group"
            >
              <div class="flex gap-4 p-4 sm:p-5">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2 flex-wrap">
                    <span 
                      v-if="article.isTop" 
                      class="px-2.5 py-1 bg-gradient-to-r from-red-500 to-rose-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-red-500/30"
                    >置顶</span>
                    <span 
                      v-if="article.isHot" 
                      class="px-2.5 py-1 bg-gradient-to-r from-orange-500 to-amber-500 text-white text-xs font-semibold rounded-lg shadow-lg shadow-orange-500/30"
                    >热门</span>
                    <span 
                      v-if="article.isOriginal" 
                      class="px-2.5 py-1 bg-gradient-to-r from-primary-500 to-primary-600 text-white text-xs font-semibold rounded-lg shadow-lg shadow-primary-500/30"
                    >原创</span>
                    <span class="px-2.5 py-1 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 text-xs font-medium rounded-lg">
                      {{ article.category?.name || article.category }}
                    </span>
                  </div>
                  
                  <h3 class="text-base sm:text-lg font-bold text-gray-900 dark:text-white mb-2 group-hover:text-primary-500 transition-colors line-clamp-2">
                    {{ article.title }}
                  </h3>
                  
                  <p class="text-sm text-gray-500 dark:text-gray-400 mb-3 line-clamp-2 leading-relaxed">
                    {{ article.summary }}
                  </p>
                  
                  <div class="flex items-center justify-between flex-wrap gap-2">
                    <div class="flex items-center gap-3">
                      <div class="flex items-center gap-2">
                        <img 
                          :src="getAvatar(article.authorId || article.author?.id, article.author?.avatar)" 
                          :alt="article.author.name"
                          class="w-6 h-6 rounded-full"
                        />
                        <span class="text-sm text-gray-600 dark:text-gray-400 font-medium">{{ article.author.name }}</span>
                      </div>
                      <span class="text-xs text-gray-400">{{ formatRelativeDate(article.createdAt) }}</span>
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
                
                <div class="flex-shrink-0 hidden sm:block">
                  <div class="w-36 h-28 rounded-xl overflow-hidden">
                    <img 
                      :src="article.cover || DEFAULT_COVER" 
                      :alt="article.title"
                      class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                    />
                  </div>
                </div>
              </div>
            </RouterLink>
          </div>

          <div v-if="!loading && articles.length > 0 && sortBy !== 'recommend'" class="flex justify-center pt-6">
            <n-pagination
              v-model:page="current"
              :page-size="size"
              :item-count="total"
              @update:page="handlePageChange"
            />
          </div>

          <div v-if="!loading && articles.length === 0" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-12 text-center">
            <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <!-- 关注 Tab 未登录提示 -->
            <template v-if="sortBy === 'follow' && !isLoggedIn()">
              <div class="flex flex-col items-center justify-center py-16">
                <svg class="w-16 h-16 text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                </svg>
                <p class="text-gray-500 dark:text-gray-400 text-base mb-2">关注博主后，这里将展示他们的最新文章</p>
                <button @click="$router.push('/login')" class="mt-2 px-5 py-2 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-full text-sm hover:shadow-lg transition-all">去登录</button>
              </div>
            </template>
            <template v-else>
              <p class="text-gray-500 dark:text-gray-400">暂无文章</p>
            </template>
          </div>
        </div>

        <div class="lg:w-80 flex-shrink-0 hidden lg:block">
          <div class="sticky top-20 space-y-6">
            <div class="bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700 rounded-2xl p-5 text-white shadow-xl shadow-primary-500/30 overflow-hidden relative">
              <div class="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
              <div class="absolute bottom-0 left-0 w-24 h-24 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
              <div class="relative">
                <div class="flex items-center gap-2 mb-2">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                  </svg>
                  <h3 class="font-bold text-lg">开始创作</h3>
                </div>
                <p class="text-sm text-white/80 mb-4 leading-relaxed">分享你的技术见解，与开发者一起成长</p>
                <button 
                  @click="handleStartWriting"
                  class="w-full px-4 py-2.5 bg-white text-primary-600 rounded-xl font-semibold hover:bg-white/90 transition-all duration-200 shadow-lg hover:shadow-xl text-sm"
                >
                  写篇文章
                </button>
              </div>
            </div>

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
              <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
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
              <div class="p-4 space-y-3">
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
                  <button class="px-3 py-1.5 text-xs text-primary-500 border border-primary-200 dark:border-primary-800 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors font-medium">
                    关注
                  </button>
                </div>
              </div>
            </div>

            <!-- 关注作者列表（仅在关注 Tab 显示） -->
            <div v-if="sortBy === 'follow' && isLoggedIn()" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-pink-500 to-rose-500 flex items-center justify-center shadow-lg shadow-pink-500/20">
                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                  </svg>
                </div>
                <div>
                  <h3 class="font-bold text-gray-900 dark:text-white">关注的博主</h3>
                  <p class="text-xs text-gray-400">{{ followingUsers.length }} 位博主</p>
                </div>
              </div>
              <div class="divide-y divide-gray-50 dark:divide-gray-700/50 max-h-80 overflow-y-auto">
                <div
                  v-for="author in followingUsers"
                  :key="author.id"
                  class="flex items-center gap-3 p-4 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors"
                >
                  <img 
                    :src="getAvatar(author.id, author.avatar)" 
                    :alt="author.nickname"
                    class="w-11 h-11 rounded-full cursor-pointer hover:opacity-80 transition-opacity"
                    @click="goToUserHome(author.id)"
                  />
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 dark:text-white truncate cursor-pointer hover:text-primary-500" @click="goToUserHome(author.id)">{{ author.nickname }}</p>
                    <p class="text-xs text-gray-400 truncate">{{ author.articles || 0 }} 篇文章</p>
                  </div>
                  <button class="px-3 py-1.5 text-xs bg-red-50 dark:bg-red-900/20 text-red-500 border border-red-200 dark:border-red-800 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors font-medium">
                    已关注
                  </button>
                </div>
              </div>
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
