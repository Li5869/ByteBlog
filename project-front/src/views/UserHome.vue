<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {articleApi, columnApi, interactionApi, isLoggedIn, userApi} from '@/utils/request'
import {NPagination} from 'naive-ui'
import {toast} from '@/utils/toast'
import OnlineIndicator from '@/components/OnlineIndicator.vue'
import {AI_USER_ID, DEFAULT_AVATAR} from '@/utils/defaults'
import {formatAbsoluteDate, formatNumber, formatRelativeDate} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userId = route.params.id

const loading = ref(true)
const userInfo = ref(null)
const activeTab = ref('articles')

const articles = ref([])
const articlesPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })
const collections = ref([])
const collectionsPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })
const columns = ref([])

const articlesLoading = ref(false)
const collectionsLoading = ref(false)
const columnsLoading = ref(false)

const articleOrderBy = ref('created_at')

/** 当前查看的是否为 AI 助手 */
const isAIUser = computed(() => String(userId) === String(AI_USER_ID))

const orderByOptions = [
  { label: '最新发布', value: 'created_at' },
  { label: '最多点赞', value: 'likes' },
  { label: '最多浏览', value: 'views' }
]

const fetchUserInfo = async () => {
  try {
    loading.value = true
    const data = await userApi.getAuthorInfo(userId)
    userInfo.value = {
      ...data,
      isFollowing: data.isFollowed || false
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchUserArticles = async (page = 1) => {
  try {
    articlesLoading.value = true
    const data = await articleApi.getArticlePage({
      current: page,
      size: articlesPagination.value.size,
      authorId: userId,
      orderBy: articleOrderBy.value
    })
    articles.value = data.records || []
    articlesPagination.value = {
      current: data.current,
      size: data.size,
      total: data.total,
      pages: data.pages
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    articlesLoading.value = false
  }
}

const fetchUserCollections = async (page = 1) => {
  try {
    collectionsLoading.value = true
    const data = await interactionApi.getUserCollections(
      userId,
      page,
      collectionsPagination.value.size
    )
    collections.value = data.records || []
    collectionsPagination.value = {
      current: data.current,
      size: data.size,
      total: data.total,
      pages: data.pages
    }
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    collectionsLoading.value = false
  }
}

const fetchUserColumns = async () => {
  if (columnsLoading.value) return
  
  columnsLoading.value = true
  try {
    const result = await columnApi.getColumnPage({
      userId: userId,
      status: 1,
      current: 1,
      size: 100
    })
    columns.value = result.records || []
  } catch (error) {
    console.error('获取用户专栏失败:', error)
  } finally {
    columnsLoading.value = false
  }
}

const handleArticlePageChange = (page) => {
  fetchUserArticles(page)
}

const handleArticlePageSizeChange = (size) => {
  articlesPagination.value.size = size
  fetchUserArticles(1)
}

const handleCollectionPageChange = (page) => {
  fetchUserCollections(page)
}

const handleCollectionPageSizeChange = (size) => {
  collectionsPagination.value.size = size
  fetchUserCollections(1)
}

watch(articleOrderBy, () => {
  fetchUserArticles(1)
})

watch(activeTab, (newTab) => {
  if (newTab === 'collections') {
    fetchUserCollections(1)
  } else if (newTab === 'columns') {
    fetchUserColumns()
  }
})


const getGenderIcon = (gender) => {
  if (gender === 1) return { icon: '♂', color: 'text-blue-500', bg: 'bg-blue-100 dark:bg-blue-900/30' }
  if (gender === 2) return { icon: '♀', color: 'text-pink-500', bg: 'bg-pink-100 dark:bg-pink-900/30' }
  return null
}

const toggleFollow = async () => {
  if (!userInfo.value || isAIUser.value) return
  toast.error('无法关注 AI 助手账号')
  try {
    const currentFollowState = userInfo.value.isFollowing
    await interactionApi.toggleFollow(userInfo.value.id, currentFollowState)
    userInfo.value.isFollowing = !currentFollowState
    if (!currentFollowState) {
      userInfo.value.fansCount = Number(userInfo.value.fansCount || 0) + 1
    } else {
      userInfo.value.fansCount = Math.max(0, Number(userInfo.value.fansCount || 0) - 1)
    }
  } catch (error) {
    console.error('关注操作失败:', error)
  }
}

const handleSendMessage = () => {
  if (isAIUser.value) {
    toast.info('AI 助手无需私信，可以尝试 AI 润色、AI 摘要等功能')
    return
  }
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  if (!userInfo.value?.id) {
    return
  }
  router.push({
    path: '/notifications',
    query: { userId: userInfo.value.id }
  })
}

const goToArticle = (articleId) => {
  router.push({ name: 'Article', params: { id: articleId } })
}

onMounted(() => {
  fetchUserInfo()
  fetchUserArticles()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800 pb-20 md:pb-8">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center py-32">
      <div class="flex flex-col items-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-500 rounded-full animate-spin"></div>
        <span class="text-gray-500 dark:text-gray-400">加载中...</span>
      </div>
    </div>

    <template v-else-if="userInfo">
      <!-- AI 助手专属头部 -->
      <div v-if="isAIUser" class="bg-gradient-to-r from-violet-500 via-purple-500 to-fuchsia-500 relative overflow-hidden">
        <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
        <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
        <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
        
        <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative">
          <div class="flex flex-col md:flex-row items-center md:items-start gap-6">
            <!-- 头像 -->
            <div class="w-28 h-28 sm:w-32 sm:h-32 rounded-2xl overflow-hidden ring-4 ring-white/30 shadow-2xl ring-fuchsia-300">
              <img :src="userInfo.avatar || DEFAULT_AVATAR" alt="AI 助手" class="w-full h-full object-cover" />
            </div>

            <!-- 用户信息 -->
            <div class="flex-1 text-center md:text-left">
              <div class="flex items-center justify-center md:justify-start gap-3 mb-3">
                <h1 class="text-2xl sm:text-3xl font-bold text-white">AI 助手</h1>
                <span class="px-3 py-1 text-xs font-semibold bg-fuchsia-400/30 backdrop-blur-sm text-white rounded-full border border-fuchsia-300/50 flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                  </svg>
                  智能助手
                </span>
              </div>

              <p class="text-white/80 text-base sm:text-lg mb-4 max-w-xl">
                智能博客助手，基于大语言模型，为你的文章提供润色、摘要、标题生成等 AI 能力支持。
              </p>

              <!-- 统计数据（AI 用户展示 AI 相关数据） -->
              <div class="flex items-center justify-center md:justify-start gap-4 sm:gap-6">
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ userInfo.articleCount || 0 }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">创作文章</div>
                </div>
                <div class="w-px h-8 sm:h-10 bg-white/20"></div>
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ formatNumber(userInfo.fansCount) }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">互动用户</div>
                </div>
              </div>
            </div>

            <!-- AI 用户提示 -->
            <div class="px-5 py-3 bg-white/10 backdrop-blur-sm rounded-xl border border-white/20 text-center max-w-xs">
              <p class="text-white/80 text-sm">这是智能助手账号，用于平台 AI 功能支持。</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 普通用户头部 -->
      <div v-else class="bg-gradient-to-r from-primary-500 via-primary-600 to-primary-700 relative overflow-hidden">
        <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
        <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
        <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
        
        <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative">
          <div class="flex flex-col md:flex-row items-center md:items-start gap-6">
            <!-- 头像 -->
            <div class="relative group">
              <div class="w-28 h-28 sm:w-32 sm:h-32 rounded-2xl overflow-hidden ring-4 ring-white/30 shadow-2xl">
                <img 
                  :src="userInfo.avatar || DEFAULT_AVATAR" 
                  :alt="userInfo.nickname || userInfo.username"
                  class="w-full h-full object-cover"
                />
              </div>

            </div>

            <!-- 用户信息 -->
            <div class="flex-1 text-center md:text-left">
              <div class="flex items-center justify-center md:justify-start gap-3 mb-3">
                <h1 class="text-2xl sm:text-3xl font-bold text-white">
                  {{ userInfo.nickname || userInfo.username }}
                </h1>
                <OnlineIndicator v-if="userId" :userId="userId" size="md" show-text />
                <span 
                  v-if="userInfo.isAdmin"
                  class="px-3 py-1 text-xs font-semibold bg-white/20 backdrop-blur-sm text-white rounded-full border border-white/30"
                >
                  管理员
                </span>
                <span 
                  v-if="getGenderIcon(userInfo.gender)"
                  :class="[getGenderIcon(userInfo.gender).bg, getGenderIcon(userInfo.gender).color]"
                  class="px-2 py-1 text-sm font-bold rounded-lg"
                >
                  {{ getGenderIcon(userInfo.gender).icon }}
                </span>
              </div>

              <p v-if="userInfo.bio" class="text-white/80 text-base sm:text-lg mb-4 max-w-xl">
                {{ userInfo.bio }}
              </p>

              <div class="flex items-center justify-center md:justify-start gap-4 text-white/70 text-sm mb-5">
                <div class="flex items-center gap-1.5">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span>{{ formatAbsoluteDate(userInfo.createdAt) }} 加入</span>
                </div>
              </div>

              <!-- 统计数据 -->
              <div class="flex items-center justify-center md:justify-start gap-4 sm:gap-6">
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ userInfo.articleCount || 0 }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">文章</div>
                </div>
                <div class="w-px h-8 sm:h-10 bg-white/20"></div>
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ formatNumber(userInfo.followCount) }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">关注</div>
                </div>
                <div class="w-px h-8 sm:h-10 bg-white/20"></div>
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ formatNumber(userInfo.fansCount) }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">粉丝</div>
                </div>
                <div class="w-px h-8 sm:h-10 bg-white/20"></div>
                <div class="text-center">
                  <div class="text-xl sm:text-2xl font-bold text-white">{{ formatNumber(userInfo.likeCount) }}</div>
                  <div class="text-white/60 text-xs sm:text-sm mt-1">获赞</div>
                </div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex gap-3">
              <button 
                @click="toggleFollow"
                class="px-6 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-lg"
                :class="userInfo.isFollowing 
                  ? 'bg-white/20 backdrop-blur-sm text-white border border-white/30 hover:bg-white/30' 
                  : 'bg-white text-primary-600 hover:bg-white/90 shadow-white/25'"
              >
                <span class="flex items-center gap-2">
                  <svg v-if="userInfo.isFollowing" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  {{ userInfo.isFollowing ? '已关注' : '关注' }}
                </span>
              </button>
              <button 
                @click="handleSendMessage"
                class="px-5 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/30 rounded-xl text-sm font-medium hover:bg-white/30 transition-all"
              >
                <span class="flex items-center gap-2">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  私信
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 -mt-4">
        <!-- 标签页 -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <div class="flex items-center border-b border-gray-100 dark:border-gray-700">
            <button 
              @click="activeTab = 'articles'"
              class="flex-1 sm:flex-none px-6 sm:px-8 py-4 text-sm font-medium transition-all relative"
              :class="activeTab === 'articles' 
                ? 'text-primary-600 dark:text-primary-400' 
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <span class="flex items-center justify-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                文章
                <span class="px-2 py-0.5 text-xs rounded-full" :class="activeTab === 'articles' ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400' : 'bg-gray-100 dark:bg-gray-700 text-gray-500'">
                  {{ userInfo.articleCount || 0 }}
                </span>
              </span>
              <div 
                v-if="activeTab === 'articles'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-500 to-primary-600"
              ></div>
            </button>
            <button 
              @click="activeTab = 'columns'"
              class="flex-1 sm:flex-none px-6 sm:px-8 py-4 text-sm font-medium transition-all relative"
              :class="activeTab === 'columns' 
                ? 'text-primary-600 dark:text-primary-400' 
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <span class="flex items-center justify-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
                专栏
                <span class="px-2 py-0.5 text-xs rounded-full" :class="activeTab === 'columns' ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400' : 'bg-gray-100 dark:bg-gray-700 text-gray-500'">
                  {{ columns.length || 0 }}
                </span>
              </span>
              <div 
                v-if="activeTab === 'columns'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-500 to-primary-600"
              ></div>
            </button>
            <button 
              @click="activeTab = 'collections'"
              class="flex-1 sm:flex-none px-6 sm:px-8 py-4 text-sm font-medium transition-all relative"
              :class="activeTab === 'collections' 
                ? 'text-primary-600 dark:text-primary-400' 
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <span class="flex items-center justify-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
                收藏
                <span class="px-2 py-0.5 text-xs rounded-full" :class="activeTab === 'collections' ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400' : 'bg-gray-100 dark:bg-gray-700 text-gray-500'">
                  {{ userInfo.collectionCount || 0 }}
                </span>
              </span>
              <div 
                v-if="activeTab === 'collections'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-500 to-primary-600"
              ></div>
            </button>
          </div>

          <!-- 文章列表 -->
          <div v-if="activeTab === 'articles'" class="p-4 sm:p-6">
            <!-- 排序选择器 -->
            <div class="flex items-center mb-4">
              <div class="flex items-center gap-2">
                <span class="text-sm text-gray-500 dark:text-gray-400">排序：</span>
                <select 
                  v-model="articleOrderBy"
                  class="w-32 px-3 py-2 text-sm border border-gray-300 rounded-lg bg-white dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option v-for="option in orderByOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>

            <div v-if="articlesLoading && articles.length === 0" class="space-y-4">
              <div v-for="i in 3" :key="i" class="animate-pulse flex gap-4 p-4">
                <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl"></div>
                <div class="flex-1 space-y-3">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
                </div>
              </div>
            </div>

            <div v-else-if="articles.length === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-700 rounded-2xl flex items-center justify-center">
                <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="text-gray-500 dark:text-gray-400 text-lg font-medium">暂无文章</p>
              <p class="text-gray-400 dark:text-gray-500 text-sm mt-2">该用户还没有发布任何文章</p>
            </div>

            <div v-else>
              <div class="space-y-4">
                <div 
                  v-for="article in articles" 
                  :key="article.id"
                  @click="goToArticle(article.id)"
                  class="group flex gap-4 p-4 rounded-xl border-2 border-gray-100 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-all cursor-pointer relative"
                >
                  <div 
                    v-if="article.cover"
                    class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl overflow-hidden shadow-md group-hover:shadow-lg transition-shadow relative"
                  >
                    <img :src="article.cover" :alt="article.title" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                    <div 
                      v-if="article.isTop"
                      class="absolute top-0 left-0 px-2 py-0.5 bg-gradient-to-r from-red-500 to-orange-500 text-white text-xs font-medium rounded-br-lg shadow-sm"
                    >
                      置顶
                    </div>
                  </div>
                  <div 
                    v-else
                    class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl bg-gradient-to-br from-primary-400 to-orange-400 flex items-center justify-center shadow-md relative"
                  >
                    <svg class="w-10 h-10 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <div 
                      v-if="article.isTop"
                      class="absolute top-0 left-0 px-2 py-0.5 bg-gradient-to-r from-red-500 to-orange-500 text-white text-xs font-medium rounded-tl-xl rounded-br-lg shadow-sm"
                    >
                      置顶
                    </div>
                  </div>
                  <div class="flex-1 min-w-0 flex flex-col justify-between py-1">
                    <div>
                      <h3 class="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-1">
                        {{ article.title }}
                      </h3>
                      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2">{{ article.summary || '暂无摘要' }}</p>
                    </div>
                    <div class="flex items-center gap-4 text-xs text-gray-400 dark:text-gray-500 mt-2">
                      <span class="flex items-center gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        {{ article.views || 0 }}
                      </span>
                      <span class="flex items-center gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                        </svg>
                        {{ article.likes || 0 }}
                      </span>
                      <span class="flex items-center gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                        {{ article.comments || 0 }}
                      </span>
                      <span class="ml-auto">{{ formatRelativeDate(article.createdAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="articlesPagination.pages > 1" class="flex justify-center pt-6">
                <n-pagination
                  v-model:page="articlesPagination.current"
                  :page-count="articlesPagination.pages"
                  @update:page="handleArticlePageChange"
                  @update:page-size="handleArticlePageSizeChange"
                  show-size-picker
                  :page-sizes="[10, 20, 30]"
                  :page-slot="5"
                />
              </div>
            </div>
          </div>

          <!-- 专栏列表 -->
          <div v-if="activeTab === 'columns'" class="p-4 sm:p-6">
            <div v-if="columnsLoading && columns.length === 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div v-for="i in 2" :key="i" class="p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                <div class="flex gap-4">
                  <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                  <div class="flex-1 space-y-3">
                    <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                    <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="columns.length === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-700 rounded-2xl flex items-center justify-center">
                <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
              <p class="text-gray-500 dark:text-gray-400 text-lg font-medium">暂无专栏</p>
              <p class="text-gray-400 dark:text-gray-500 text-sm mt-2">该用户还没有创建任何专栏</p>
            </div>

            <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div
                v-for="column in columns"
                :key="column.id"
                @click="router.push({ name: 'ColumnDetail', params: { id: column.id } })"
                class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-purple-200 dark:hover:border-purple-800 transition-all cursor-pointer"
              >
                <div
                  v-if="column.cover"
                  class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                >
                  <img :src="column.cover" :alt="column.title" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                </div>
                <div
                  v-else
                  class="flex-shrink-0 w-24 h-24 rounded-xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center"
                >
                  <svg class="w-10 h-10 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2">
                    <span
                      v-if="column.status === 0"
                      class="px-2 py-0.5 text-xs font-medium rounded-lg bg-gradient-to-r from-yellow-400 to-amber-400 text-white"
                    >
                      草稿
                    </span>
                  </div>
                  <h3 class="font-semibold text-gray-900 dark:text-white mb-1 group-hover:text-purple-500 transition-colors line-clamp-1">
                    {{ column.title }}
                  </h3>
                  <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1 mb-2">
                    {{ column.description || '暂无描述' }}
                  </p>
                  <div class="flex items-center gap-3 text-xs text-gray-400">
                    <span class="flex items-center gap-1">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      {{ column.articlesCount }}篇
                    </span>
                    <span class="flex items-center gap-1">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      {{ formatNumber(column.views) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 收藏列表 -->
          <div v-else class="p-4 sm:p-6">
            <div v-if="collectionsLoading && collections.length === 0" class="space-y-4">
              <div v-for="i in 3" :key="i" class="animate-pulse flex gap-4 p-4">
                <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl"></div>
                <div class="flex-1 space-y-3">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
                </div>
              </div>
            </div>

            <div v-else-if="collections.length === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-700 rounded-2xl flex items-center justify-center">
                <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </div>
              <p class="text-gray-500 dark:text-gray-400 text-lg font-medium">暂无收藏</p>
              <p class="text-gray-400 dark:text-gray-500 text-sm mt-2">该用户还没有收藏任何文章</p>
            </div>

            <div v-else>
              <div class="space-y-4">
                <div 
                  v-for="item in collections" 
                  :key="item.id"
                  @click="goToArticle(item.articleId)"
                  class="group flex gap-4 p-4 rounded-xl border-2 border-gray-100 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-all cursor-pointer"
                >
                  <div 
                    v-if="item.cover"
                    class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl overflow-hidden shadow-md group-hover:shadow-lg transition-shadow"
                  >
                    <img :src="item.cover" :alt="item.title" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  </div>
                  <div 
                    v-else
                    class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl bg-gradient-to-br from-amber-400 to-orange-400 flex items-center justify-center shadow-md"
                  >
                    <svg class="w-10 h-10 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0 flex flex-col justify-between py-1">
                    <div>
                      <h3 class="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-1">
                        {{ item.title }}
                      </h3>
                      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2">{{ item.summary || '暂无摘要' }}</p>
                    </div>
                    <div class="flex items-center justify-between mt-2">
                      <div class="flex items-center gap-2">
                        <img :src="item.authorAvatar || DEFAULT_AVATAR" :alt="item.authorName" class="w-5 h-5 rounded-full" />
                        <span class="text-xs text-gray-500 dark:text-gray-400">{{ item.authorName }}</span>
                      </div>
                      <span class="text-xs text-gray-400 dark:text-gray-500">
                        收藏于 {{ formatRelativeDate(item.collectedAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="collectionsPagination.pages > 1" class="flex justify-center pt-6">
                <n-pagination
                  v-model:page="collectionsPagination.current"
                  :page-count="collectionsPagination.pages"
                  @update:page="handleCollectionPageChange"
                  @update:page-size="handleCollectionPageSizeChange"
                  show-size-picker
                  :page-sizes="[10, 20, 30]"
                  :page-slot="5"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 用户不存在 -->
    <div v-else class="flex flex-col items-center justify-center py-32">
      <div class="w-24 h-24 bg-gray-100 dark:bg-gray-800 rounded-2xl flex items-center justify-center mb-6">
        <svg class="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-2">用户不存在</h2>
      <p class="text-gray-500 dark:text-gray-400 mb-6">该用户可能已被删除或不存在</p>
      <button 
        @click="router.push('/')"
        class="px-6 py-2.5 bg-primary-500 text-white rounded-xl hover:bg-primary-600 transition-colors"
      >
        返回首页
      </button>
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
