<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {NPagination} from 'naive-ui'
import {interactionApi, searchApi} from '@/utils/request'
import {useUserStore} from '@/stores/user'
import {toast} from '@/utils/toast'
import {DEFAULT_AVATAR, getAvatar} from '@/utils/defaults'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ========== 搜索状态 ==========
const searchQuery = ref('')
const activeTab = ref('all')
const sortBy = ref('relevance')
const loading = ref(false)
const current = ref(1)
const size = ref(10)

const sortOptions = [
  { label: '相关度', value: 'relevance' },
  { label: '最新', value: 'time' },
  { label: '最热', value: 'views' }
]

// ========== 数据 ==========
const allArticles = ref([])
const allQuestions = ref([])
const allAuthors = ref([])
const allColumns = ref([])

const searchResult = ref({
  articleTotal: 0,
  questionTotal: 0,
  authorTotal: 0,
  columnTotal: 0
})

// ========== 计算属性 ==========
const totalCount = computed(() => {
  const at = Number(searchResult.value.articleTotal) || 0
  const qt = Number(searchResult.value.questionTotal) || 0
  const aut = Number(searchResult.value.authorTotal) || 0
  const ct = Number(searchResult.value.columnTotal) || 0

  if (activeTab.value === 'all') return at + qt + aut + ct
  if (activeTab.value === 'article') return at
  if (activeTab.value === 'question') return qt
  if (activeTab.value === 'author') return aut
  if (activeTab.value === 'column') return ct
  return 0
})

const paginationTotal = computed(() => {
  const at = Number(searchResult.value.articleTotal) || 0
  const qt = Number(searchResult.value.questionTotal) || 0
  const aut = Number(searchResult.value.authorTotal) || 0
  const ct = Number(searchResult.value.columnTotal) || 0

  if (activeTab.value === 'all') return Math.max(at, qt, aut, ct)
  return totalCount.value
})

// ========== 搜索逻辑 ==========
const fetchFollowStatus = async (authors) => {
  if (!userStore.state.isLoggedIn || !authors || authors.length === 0) return
  
  const currentUserId = userStore.state.userInfo?.id
  const authorIds = authors
    .filter(author => String(author.id) !== String(currentUserId))
    .map(author => author.id)
  
  if (authorIds.length === 0) return
  
  try {
    const followedIds = await interactionApi.checkBatchFollowStatus(authorIds)
    const followedSet = new Set(followedIds.map(id => String(id)))
    authors.forEach(author => {
      author.isFollowing = followedSet.has(String(author.id))
    })
  } catch (error) {
    console.error('获取关注状态失败:', error)
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  loading.value = true
  current.value = 1
  allArticles.value = []
  allQuestions.value = []
  allAuthors.value = []
  allColumns.value = []
  searchResult.value = { articleTotal: 0, questionTotal: 0, authorTotal: 0, columnTotal: 0 }

  try {
    const params = {
      keyword: searchQuery.value,
      type: activeTab.value,
      orderBy: sortBy.value,
      current: current.value,
      size: size.value
    }

    const result = await searchApi.search(params)

    searchResult.value = {
      articleTotal: Number(result.articleTotal) || 0,
      questionTotal: Number(result.questionTotal) || 0,
      authorTotal: Number(result.authorTotal) || 0,
      columnTotal: Number(result.columnTotal) || 0
    }

    allArticles.value = result.articles || []
    allQuestions.value = result.questions || []
    allAuthors.value = (result.authors || []).map(author => ({
      ...author,
      isFollowing: false
    }))
    allColumns.value = result.columns || []
    
    await fetchFollowStatus(allAuthors.value)
  } catch (error) {
    console.error('搜索失败:', error)
    toast.error(error.message || '搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  current.value = page
  fetchPage()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const fetchPage = async () => {
  if (!searchQuery.value.trim()) return
  loading.value = true
  try {
    const params = {
      keyword: searchQuery.value,
      type: activeTab.value,
      orderBy: sortBy.value,
      current: current.value,
      size: size.value
    }
    const result = await searchApi.search(params)
    searchResult.value = {
      articleTotal: Number(result.articleTotal) || 0,
      questionTotal: Number(result.questionTotal) || 0,
      authorTotal: Number(result.authorTotal) || 0,
      columnTotal: Number(result.columnTotal) || 0
    }
    allArticles.value = result.articles || []
    allQuestions.value = result.questions || []
    allAuthors.value = (result.authors || []).map(author => ({
      ...author,
      isFollowing: false
    }))
    allColumns.value = result.columns || []
    
    await fetchFollowStatus(allAuthors.value)
  } catch (error) {
    console.error('加载失败:', error)
    toast.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ========== 交互 ==========
const handleTabChange = (tab) => {
  activeTab.value = tab
  current.value = 1
  if (searchQuery.value.trim()) handleSearch()
}

const handleSortChange = (val) => {
  sortBy.value = val
  if (searchQuery.value.trim()) handleSearch()
}

const goToUserHome = (userId) => {
  const currentUserId = userStore.state.userInfo?.id
  if (currentUserId && String(userId) === String(currentUserId)) {
    router.push('/mine')
  } else {
    router.push(`/user/${userId}`)
  }
}

const toggleFollow = async (user) => {
  if (!userStore.state.isLoggedIn) {
    toast.error('请先登录')
    return
  }
  try {
    const currentFollowState = user.isFollowing
    await interactionApi.toggleFollow(user.id, currentFollowState)
    user.isFollowing = !currentFollowState
    if (!currentFollowState) {
      user.fansCount++
      toast.success('关注成功')
    } else {
      user.fansCount--
      toast.success('已取消关注')
    }
  } catch (error) {
    console.error('关注操作失败:', error)
    toast.error(error.message || '操作失败')
  }
}

const formatNumber = (num) => {
  if (!num) return 0
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) return '昨天'
  else if (days < 7) return `${days}天前`
  else if (days < 30) return `${Math.floor(days / 7)}周前`
  else if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

const highlightText = (text, keyword) => {
  if (!text || !keyword) return text
  const escapeRegExp = (str) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

// ========== 生命周期 ==========
onMounted(() => {
  searchQuery.value = route.query.q || route.query.tag || ''
  if (searchQuery.value) handleSearch()
})

watch(() => route.query, (newQuery) => {
  const newQ = newQuery.q || newQuery.tag || ''
  if (newQ && newQ !== searchQuery.value) {
    searchQuery.value = newQ
    handleSearch()
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800 pb-20">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      <!-- ===== 搜索区 ===== -->
      <div class="mb-8">
        <!-- 标题 -->
        <div class="flex items-center gap-3 mb-6">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/20">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <div>
            <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">搜索</h1>
            <p class="text-sm text-gray-500 dark:text-gray-400">探索文章、问答、专栏和优质博主</p>
          </div>
        </div>


      </div>

      <!-- ===== 结果区 ===== -->
      <div v-if="searchQuery">
        <!-- 顶部信息栏 -->
        <div class="flex items-center justify-between mb-6 gap-4 flex-wrap">
          <div>
            <h2 class="text-lg font-bold text-gray-900 dark:text-white">
              "<span class="text-primary-500">{{ searchQuery }}</span>" 的搜索结果
            </h2>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
              共找到 <span class="font-semibold text-gray-700 dark:text-gray-300">{{ totalCount }}</span> 个结果
            </p>
          </div>
          <select
            v-model="sortBy"
            class="px-3 py-2 text-sm border border-gray-200 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-500 cursor-pointer"
            @change="handleSortChange(sortBy)"
          >
            <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>

        <!-- Tab 切换 - 始终显示（只要有搜索词） -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden mb-6">
          <div class="flex items-center border-b border-gray-100 dark:border-gray-700">
            <button
              v-for="tab in [
                { key: 'all', label: '全部', count: totalCount },
                { key: 'article', label: '文章', count: searchResult.articleTotal },
                { key: 'question', label: '问答', count: searchResult.questionTotal },
                { key: 'author', label: '博主', count: searchResult.authorTotal },
                { key: 'column', label: '专栏', count: searchResult.columnTotal }
              ]"
              :key="tab.key"
              @click="handleTabChange(tab.key)"
              class="flex-1 sm:flex-none px-5 sm:px-6 py-4 text-sm font-medium transition-all relative"
              :class="activeTab === tab.key
                ? 'text-primary-600 dark:text-primary-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <span class="flex items-center justify-center gap-2">
                <svg v-if="tab.key === 'all'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
                <svg v-else-if="tab.key === 'article'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <svg v-else-if="tab.key === 'question'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <svg v-else-if="tab.key === 'author'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <svg v-else-if="tab.key === 'column'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
                {{ tab.label }}
                <span class="px-2 py-0.5 text-xs rounded-full"
                  :class="activeTab === tab.key
                    ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'"
                >
                  {{ formatNumber(tab.count) }}
                </span>
              </span>
              <div
                v-if="activeTab === tab.key"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-500 to-primary-600"
              />
            </button>
          </div>

          <!-- 内容区 -->
          <div class="p-4 sm:p-6">

            <!-- 加载中 -->
            <div v-if="loading" class="space-y-4">
              <div v-for="i in 3" :key="i" class="bg-gray-50 dark:bg-gray-700/30 rounded-xl p-5 animate-pulse">
                <div class="flex gap-4">
                  <div class="w-24 h-24 bg-gray-200 dark:bg-gray-600 rounded-xl flex-shrink-0"></div>
                  <div class="flex-1 space-y-3">
                    <div class="h-5 bg-gray-200 dark:bg-gray-600 rounded w-3/4"></div>
                    <div class="h-4 bg-gray-200 dark:bg-gray-600 rounded w-full"></div>
                    <div class="h-4 bg-gray-200 dark:bg-gray-600 rounded w-1/2"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 无结果 -->
            <div v-else-if="totalCount === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-700/50 rounded-2xl flex items-center justify-center">
                <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">没有找到相关结果</h3>
              <p class="text-gray-500 dark:text-gray-400 text-sm">换个关键词试试，或浏览其他分类</p>
            </div>

            <!-- 有结果 -->
            <div v-else>
              <!-- ===== 博主列表 ===== -->
              <template v-if="activeTab === 'author' || (activeTab === 'all' && allAuthors.length > 0)">
                <div v-if="activeTab === 'all'" class="flex items-center gap-3 mb-4">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-yellow-400 to-orange-500 flex items-center justify-center shadow-lg shadow-orange-500/20">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                      <circle cx="9" cy="7" r="4" />
                      <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
                    </svg>
                  </div>
                  <div>
                    <h3 class="font-bold text-gray-900 dark:text-white">相关博主</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ allAuthors.length }} 位</p>
                  </div>
                </div>

                <div :class="activeTab === 'author' ? 'grid grid-cols-1 sm:grid-cols-2 gap-4' : 'flex flex-wrap gap-3'">
                  <div
                    v-for="author in (activeTab === 'all' ? allAuthors.slice(0, 4) : allAuthors)"
                    :key="author.id"
                    class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-700/30 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-all cursor-pointer group"
                    :class="activeTab === 'author' ? 'border border-gray-100 dark:border-gray-700' : 'border border-gray-100 dark:border-gray-700'"
                    @click="goToUserHome(author.id)"
                  >
                    <img
                      :src="getAvatar(author.id, author.avatar)"
                      :alt="author.nickname"
                      class="w-12 h-12 rounded-xl object-cover flex-shrink-0"
                    />
                    <div class="flex-1 min-w-0">
                      <h4 class="font-semibold text-gray-900 dark:text-white text-sm group-hover:text-primary-500 transition-colors truncate" v-html="highlightText(author.nickname, searchQuery)" />
                      <p class="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{{ author.bio || '这个人很懒，什么都没写' }}</p>
                      <div class="flex items-center gap-3 text-xs text-gray-400 mt-1">
                        <span><strong class="text-gray-600 dark:text-gray-300">{{ formatNumber(author.articlesCount) }}</strong> 文章</span>
                        <span><strong class="text-gray-600 dark:text-gray-300">{{ formatNumber(author.fansCount) }}</strong> 粉丝</span>
                      </div>
                    </div>
                    <button
                      v-if="userStore.state.isLoggedIn && String(userStore.state.userInfo?.id) !== String(author.id)"
                      class="px-3 py-1.5 text-xs font-medium rounded-lg border transition-all flex-shrink-0"
                      :class="author.isFollowing
                        ? 'border-gray-200 dark:border-gray-600 text-gray-500 dark:text-gray-400 hover:border-red-200 hover:text-red-500'
                        : 'border-primary-200 dark:border-primary-800 text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20'"
                      @click.stop="toggleFollow(author)"
                    >
                      {{ author.isFollowing ? '已关注' : '关注' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- ===== 文章列表 ===== -->
              <template v-if="activeTab !== 'author' && allArticles.length > 0">
                <div v-if="activeTab === 'all'" class="flex items-center gap-3 mb-4 mt-6 pt-6 border-t border-gray-100 dark:border-gray-700">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-pink-500 to-rose-500 flex items-center justify-center shadow-lg shadow-rose-500/20">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div>
                    <h3 class="font-bold text-gray-900 dark:text-white">文章</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ searchResult.articleTotal }} 篇</p>
                  </div>
                </div>

                <div class="space-y-4">
                  <RouterLink
                    v-for="article in allArticles"
                    :key="article.id"
                    :to="`/article/${article.id}`"
                    class="group flex gap-4 p-4 rounded-xl border-2 border-gray-100 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-all"
                  >
                    <div
                      v-if="article.cover"
                      class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl overflow-hidden shadow-md"
                    >
                      <img :src="article.cover" :alt="article.title" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                    </div>
                    <div
                      v-else
                      class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl bg-gradient-to-br from-primary-400 to-orange-400 flex items-center justify-center shadow-md"
                    >
                      <svg class="w-10 h-10 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0 flex flex-col justify-between py-1">
                      <div>
                        <div class="flex flex-wrap gap-1.5 mb-2">
                          <span class="px-2 py-0.5 bg-pink-50 dark:bg-pink-900/20 text-pink-600 dark:text-pink-400 text-xs rounded-md font-medium">文章</span>
                          <span v-if="article.isTop" class="px-2 py-0.5 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-xs rounded-md font-medium">置顶</span>
                          <span v-if="article.isHot" class="px-2 py-0.5 bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400 text-xs rounded-md font-medium">热门</span>
                          <span v-if="article.categoryName" class="px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 text-xs rounded-md">{{ article.categoryName }}</span>
                        </div>
                        <h3 class="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-1 text-base" v-html="highlightText(article.title, searchQuery)" />
                        <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2" v-html="highlightText(article.summary, searchQuery)" />
                      </div>
                      <div class="flex items-center gap-4 text-xs text-gray-400 dark:text-gray-500 mt-2">
                        <div class="flex items-center gap-1.5">
                          <img :src="article.authorAvatar || DEFAULT_AVATAR" class="w-5 h-5 rounded-full" />
                          <span class="text-gray-600 dark:text-gray-400">{{ article.authorName }}</span>
                        </div>
                        <span>{{ formatDate(article.createdAt) }}</span>
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
                      </div>
                    </div>
                  </RouterLink>
                </div>
              </template>

              <!-- ===== 问答列表 ===== -->
              <template v-if="activeTab !== 'author' && allQuestions.length > 0">
                <div v-if="activeTab === 'all'" class="flex items-center gap-3 mb-4 mt-6 pt-6 border-t border-gray-100 dark:border-gray-700">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <div>
                    <h3 class="font-bold text-gray-900 dark:text-white">问答</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ searchResult.questionTotal }} 个</p>
                  </div>
                </div>

                <div class="space-y-4">
                  <div
                    v-for="question in allQuestions"
                    :key="question.id"
                    class="group flex gap-4 p-4 rounded-xl border-2 border-gray-100 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-all cursor-pointer"
                    @click="router.push(`/question/${question.id}`)"
                  >
                    <div class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl bg-gradient-to-br from-blue-400 to-indigo-400 flex items-center justify-center shadow-md">
                      <svg class="w-10 h-10 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0 flex flex-col justify-between py-1">
                      <div>
                        <div class="flex flex-wrap gap-1.5 mb-2">
                          <span class="px-2 py-0.5 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 text-xs rounded-md font-medium">问答</span>
                          <span v-if="question.isSolved" class="px-2 py-0.5 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 text-xs rounded-md font-medium">已解决</span>
                          <span v-for="tag in (question.tags || []).slice(0, 2)" :key="tag" class="px-2 py-0.5 bg-violet-50 dark:bg-violet-900/20 text-violet-600 dark:text-violet-400 text-xs rounded-md">{{ tag }}</span>
                        </div>
                        <h3 class="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-1 text-base" v-html="highlightText(question.title, searchQuery)" />
                        <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2" v-html="highlightText(question.content, searchQuery)" />
                      </div>
                      <div class="flex items-center gap-4 text-xs text-gray-400 dark:text-gray-500 mt-2">
                        <div class="flex items-center gap-1.5">
                          <img :src="question.authorAvatar || DEFAULT_AVATAR" class="w-5 h-5 rounded-full" />
                          <span class="text-gray-600 dark:text-gray-400">{{ question.authorName }}</span>
                        </div>
                        <span>{{ formatDate(question.createdAt) }}</span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                          {{ formatNumber(question.views) }}
                        </span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                          </svg>
                          {{ formatNumber(question.answers) }} 回答
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </template>

              <!-- ===== 专栏列表 ===== -->
              <template v-if="activeTab !== 'author' && allColumns.length > 0">
                <div v-if="activeTab === 'all'" class="flex items-center gap-3 mb-4 mt-6 pt-6 border-t border-gray-100 dark:border-gray-700">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 flex items-center justify-center shadow-lg shadow-teal-500/20">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                  </div>
                  <div>
                    <h3 class="font-bold text-gray-900 dark:text-white">专栏</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ searchResult.columnTotal }} 个</p>
                  </div>
                </div>

                <div class="space-y-4">
                  <RouterLink
                    v-for="column in allColumns"
                    :key="column.id"
                    :to="`/column/${column.id}`"
                    class="group flex gap-4 p-4 rounded-xl border-2 border-gray-100 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 hover:bg-primary-50/50 dark:hover:bg-primary-900/10 transition-all"
                  >
                    <div
                      v-if="column.cover"
                      class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl overflow-hidden shadow-md"
                    >
                      <img :src="column.cover" :alt="column.title" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                    </div>
                    <div
                      v-else
                      class="flex-shrink-0 w-24 h-24 sm:w-28 sm:h-28 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-400 flex items-center justify-center shadow-md"
                    >
                      <svg class="w-10 h-10 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0 flex flex-col justify-between py-1">
                      <div>
                        <div class="flex flex-wrap gap-1.5 mb-2">
                          <span class="px-2 py-0.5 bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400 text-xs rounded-md font-medium">专栏</span>
                        </div>
                        <h3 class="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-1 text-base" v-html="highlightText(column.title, searchQuery)" />
                        <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2" v-html="highlightText(column.description, searchQuery)" />
                      </div>
                      <div class="flex items-center gap-4 text-xs text-gray-400 dark:text-gray-500 mt-2">
                        <div class="flex items-center gap-1.5">
                          <img :src="column.authorAvatar || DEFAULT_AVATAR" class="w-5 h-5 rounded-full" />
                          <span class="text-gray-600 dark:text-gray-400">{{ column.authorName }}</span>
                        </div>
                        <span>{{ formatDate(column.createdAt) }}</span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                          {{ formatNumber(column.views) }}
                        </span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                          </svg>
                          {{ formatNumber(column.articlesCount) }} 篇
                        </span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                          </svg>
                          {{ formatNumber(column.subscriptionCount) }} 订阅
                        </span>
                      </div>
                    </div>
                  </RouterLink>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="!loading && paginationTotal > size" class="flex justify-center pt-4">
          <n-pagination
            v-model:page="current"
            :page-size="size"
            :item-count="paginationTotal"
            @update:page="handlePageChange"
            :page-sizes="[10, 20, 50]"
            show-quick-jumper
          />
        </div>
      </div>

      <!-- ===== 未搜索时的空状态 ===== -->
      <div v-else class="text-center py-20">
        <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-2xl flex items-center justify-center">
          <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">开始搜索</h3>
        <p class="text-gray-500 dark:text-gray-400 text-sm">输入关键词，一键搜索海量文章、问答、专栏和优质博主</p>
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

/* 搜索高亮 */
:deep(.search-highlight) {
  color: #ef4444;
  background-color: #fef2f2;
  font-weight: 600;
  padding: 0 2px;
  border-radius: 3px;
  font-style: normal;
}

.dark :deep(.search-highlight) {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
</style>
