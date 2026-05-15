<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {NPagination} from 'naive-ui'
import {isLoggedIn, questionApi, tagApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {useUserStore} from '@/stores/user'
import {getAvatar} from '@/utils/defaults'
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

const handleAskQuestion = () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  router.push('/create-question')
}

const tabs = [
  { key: 'all', name: '全部' },
  { key: 'unanswered', name: '待回答' },
  { key: 'solved', name: '已解决' }
]

const questions = ref([])
const hotQuestions = ref([])

const activeTab = ref('all')
const sortBy = ref('newest')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

// 标签筛选
const tags = ref([])
const activeTagId = ref(null)
const showTagPopup = ref(false)
const tagPopupRef = ref(null)
const tagLoading = ref(false)

const fetchQuestions = async () => {
  if (loading.value) return
  
  loading.value = true
  try {
    const params = {
      current: current.value,
      size: size.value,
      status: activeTab.value,
      sortBy: sortBy.value
    }
    
    if (activeTagId.value) {
      params.tagId = activeTagId.value
    }
    
    const data = await questionApi.getQuestionPage(params)
    questions.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取问题列表失败:', error)
    toast.error('获取问题列表失败')
  } finally {
    loading.value = false
  }
}

const fetchHotQuestions = async () => {
  try {
    const data = await questionApi.getHotQuestions(5)
    hotQuestions.value = data || []
  } catch (error) {
    console.error('获取热门问题失败:', error)
  }
}

const selectTab = (tabKey) => {
  activeTab.value = tabKey
  activeTagId.value = null
  current.value = 1
  fetchQuestions()
}

const selectTag = (tagId) => {
  activeTagId.value = activeTagId.value === tagId ? null : tagId
  current.value = 1
  fetchQuestions()
}

const fetchTags = async () => {
  tagLoading.value = true
  try {
    const data = await tagApi.getTags()
    tags.value = data || []
  } catch (e) {
    console.error('获取标签失败:', e)
  } finally {
    tagLoading.value = false
  }
}

let preventCloseOnce = false

const closeTagPopup = (e) => {
  // 如果标记为跳过本次关闭，则重置并返回
  if (preventCloseOnce) {
    preventCloseOnce = false
    return
  }

  // 检查点击是否在弹窗内或"更多"按钮内
  const inPopup = tagPopupRef.value && tagPopupRef.value.contains(e.target)
  const moreBtn = e.target.closest('[data-tag-more]')
  if (!inPopup && !moreBtn) {
    showTagPopup.value = false
  }
}

const toggleTagPopup = (e) => {
  e.stopPropagation()
  // 标记下一次 document click 应该忽略（防止弹窗刚开就关）
  preventCloseOnce = true

  // 使用 setTimeout 确保在事件冒泡完成后再切换状态
  setTimeout(() => {
    showTagPopup.value = !showTagPopup.value
  }, 0)
}

const handleSortChange = (sort) => {
  sortBy.value = sort
  current.value = 1
  fetchQuestions()
}

const handlePageChange = (page) => {
  current.value = page
  fetchQuestions()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}


onMounted(() => {
  fetchQuestions()
  fetchHotQuestions()
  fetchTags()
  document.addEventListener('click', closeTagPopup)
})

onUnmounted(() => {
  document.removeEventListener('click', closeTagPopup)
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 lg:py-8">
      <div class="flex flex-col lg:flex-row gap-6">
        <div class="lg:w-60 flex-shrink-0 hidden lg:block">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden sticky top-20">
            <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-red-500 to-orange-500 flex items-center justify-center">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 class="font-bold text-gray-900 dark:text-white">问题分类</h3>
            </div>
            <div class="p-2">
              <button 
                v-for="tab in tabs" 
                :key="tab.key"
                @click="selectTab(tab.key)"
                class="w-full flex items-center justify-between px-3 py-2.5 text-sm rounded-xl transition-all duration-200"
                :class="activeTab === tab.key 
                  ? 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-lg shadow-red-500/30' 
                  : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
              >
                <span class="font-medium">{{ tab.name }}</span>
              </button>
            </div>
          </div>

          <!-- 标签筛选 -->
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 sticky top-20 relative">
            <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-500 flex items-center justify-center">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
              </div>
              <h3 class="font-bold text-gray-900 dark:text-white">标签筛选</h3>
            </div>
            <div class="p-2">
              <!-- 骨架屏 -->
              <div v-if="tagLoading" class="p-2 space-y-2">
                <div v-for="i in 4" :key="i" class="h-7 bg-gray-100 dark:bg-gray-700 rounded-lg animate-pulse"></div>
              </div>
              <template v-else>
                <!-- 全部标签按钮 -->
                <button
                  @click="selectTag(null)"
                  class="w-full flex items-center justify-between px-3 py-2 text-sm rounded-xl transition-all duration-200"
                  :class="activeTagId === null
                    ? 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-lg shadow-red-500/30'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
                >
                  <span class="font-medium">全部</span>
                </button>
                <!-- 标签列表（最多显示5个） -->
                <button
                  v-for="tag in tags.slice(0, 5)"
                  :key="tag.id"
                  @click="selectTag(tag.id)"
                  class="w-full flex items-center justify-between px-3 py-2 text-sm rounded-xl transition-all duration-200"
                  :class="activeTagId === tag.id
                    ? 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-lg shadow-red-500/30'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
                >
                  <span class="font-medium truncate">{{ tag.name }}</span>
                </button>

                <!-- 更多标签按钮 -->
                <button
                  v-if="tags.length > 5"
                  data-tag-more
                  @click="toggleTagPopup"
                  class="w-full flex items-center justify-center gap-1 px-3 py-2 text-xs text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                  <span>+{{ tags.length - 5 }} 更多</span>
                </button>
              </template>
            </div>

            <!-- 弹窗浮层 - 出现在盒子右侧 -->
            <div
              v-if="showTagPopup && tags.length > 5"
              ref="tagPopupRef"
              class="absolute left-full top-0 ml-2 bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-100 dark:border-gray-700 z-50 overflow-hidden min-w-[160px]"
            >
              <div class="px-3 py-2 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
                <span class="text-xs font-semibold text-gray-500 dark:text-gray-400">选择标签</span>
                <button @click="showTagPopup = false" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div class="p-1 max-h-64 overflow-y-auto">
                <button
                  v-for="tag in tags.slice(5)"
                  :key="tag.id"
                  @click="selectTag(tag.id); showTagPopup = false"
                  class="w-full text-left flex items-center justify-between px-3 py-2 text-sm rounded-lg transition-all duration-200"
                  :class="activeTagId === tag.id
                    ? 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-md'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-500 dark:hover:text-red-400'"
                >
                  <span class="font-medium truncate">{{ tag.name }}</span>
                  <svg v-if="activeTagId === tag.id" class="w-3.5 h-3.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 min-w-0">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 mb-4 overflow-hidden">
            <div class="flex items-center justify-between p-3 sm:p-4 gap-2 flex-wrap">
              <div class="flex items-center gap-1 sm:gap-2">
                <button 
                  v-for="item in [
                    { key: 'newest', label: '最新', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
                    { key: 'hot', label: '最热', icon: 'M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z' }
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
                <div class="w-14 h-14 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse"></div>
                <div class="flex-1 space-y-3">
                  <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                  <div class="flex gap-4">
                    <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    <div class="h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="space-y-4">
            <RouterLink 
              v-for="question in questions" 
              :key="question.id"
              :to="`/question/${question.id}`"
              class="block bg-white dark:bg-gray-800 rounded-2xl shadow-sm hover:shadow-xl border border-gray-100 dark:border-gray-700/50 overflow-hidden transition-all duration-300 hover:-translate-y-1 group"
            >
              <div class="flex gap-4 p-4 sm:p-5">
                <div 
                  class="flex flex-col items-center justify-center min-w-[56px] h-14 rounded-xl text-center transition-all"
                  :class="!question.answers || question.answers === 0 
                    ? 'bg-gray-100 dark:bg-gray-700' 
                    : question.answers > 0 && question.answers < 5 
                      ? 'bg-blue-50 dark:bg-blue-900/20' 
                      : 'bg-green-50 dark:bg-green-900/20'"
                >
                  <span 
                    class="text-lg font-bold"
                    :class="!question.answers || question.answers === 0 
                      ? 'text-gray-400' 
                      : question.answers > 0 && question.answers < 5 
                        ? 'text-blue-500' 
                        : 'text-green-500'"
                  >{{ question.answers || 0 }}</span>
                  <span class="text-xs text-gray-400">回答</span>
                </div>

                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-3 mb-2">
                    <h3 class="text-base sm:text-lg font-semibold text-gray-900 dark:text-white group-hover:text-primary-500 transition-colors line-clamp-2 leading-relaxed flex-1">
                      {{ question.title }}
                    </h3>
                    <span 
                      v-if="question.isSolved"
                      class="flex-shrink-0 flex items-center gap-1 px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-xs font-medium rounded-lg"
                    >
                      <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                      </svg>
                      已解决
                    </span>
                  </div>

                  <p class="text-sm text-gray-500 dark:text-gray-400 mb-3 line-clamp-2 leading-relaxed">
                    {{ question.excerpt }}
                  </p>

                  <div class="flex items-center justify-between flex-wrap gap-2">
                    <div class="flex items-center gap-3">
                      <img 
                        v-if="question.author?.avatar"
                        :src="getAvatar(question.author?.id, question.author?.avatar)" 
                        :alt="question.author?.name"
                        class="w-6 h-6 rounded-full cursor-pointer hover:opacity-80 transition-opacity"
                        @click.stop="goToUserHome(question.author?.id)"
                      />
                      <div 
                        v-else
                        class="w-6 h-6 rounded-full bg-gradient-to-br from-red-400 to-orange-400 flex items-center justify-center text-white text-xs font-bold cursor-pointer"
                        @click.stop="goToUserHome(question.author?.id)"
                      >{{ (question.author?.name || '?').charAt(0) }}</div>
                      <span 
                        class="text-sm text-gray-600 dark:text-gray-400 font-medium cursor-pointer hover:text-primary-500 transition-colors"
                        @click.stop="goToUserHome(question.author?.id)"
                      >{{ question.author?.name || '匿名用户' }}</span>
                      <span class="text-xs text-gray-400">{{ formatTime(question.createdAt) }}</span>
                    </div>
                    
                    <div class="flex items-center gap-4 text-xs text-gray-400">
                      <span class="flex items-center gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        {{ formatNumber(question.views) }}
                      </span>
                      <span class="flex items-center gap-1">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                        </svg>
                        {{ formatNumber(question.likes) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </RouterLink>
          </div>

          <div v-if="!loading && questions.length > 0" class="flex justify-center pt-6">
            <n-pagination
              v-model:page="current"
              :page-size="size"
              :item-count="total"
              @update:page="handlePageChange"
            />
          </div>

          <div v-if="!loading && questions.length === 0" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-12 text-center">
            <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
              <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p class="text-gray-500 dark:text-gray-400 mb-4">暂无问题</p>
            <button 
              @click="handleAskQuestion"
              class="px-6 py-2.5 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl font-semibold hover:from-red-600 hover:to-orange-600 transition-all shadow-lg shadow-red-500/20"
            >
              去提问
            </button>
          </div>
        </div>

        <div class="lg:w-80 flex-shrink-0 hidden lg:block">
          <div class="sticky top-20 space-y-6">
            <div class="bg-gradient-to-br from-red-500 via-orange-500 to-yellow-500 rounded-2xl p-5 text-white shadow-xl shadow-red-500/30 overflow-hidden relative">
              <div class="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
              <div class="absolute bottom-0 left-0 w-24 h-24 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
              <div class="relative">
                <div class="flex items-center gap-2 mb-2">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <h3 class="font-bold text-lg">问答社区</h3>
                </div>
                <p class="text-sm text-white/90 mb-4 leading-relaxed">有问题？在这里提问，让技术大牛帮你解答</p>
                <button 
                  @click="handleAskQuestion"
                  class="w-full px-4 py-2.5 bg-white text-red-500 rounded-xl font-semibold hover:bg-white/90 transition-all duration-200 shadow-lg hover:shadow-xl text-sm"
                >
                  提出问题
                </button>
              </div>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-orange-500 flex items-center justify-center shadow-lg shadow-red-500/20">
                  <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                  </svg>
                </div>
                <div>
                  <h3 class="font-bold text-gray-900 dark:text-white">热门问题</h3>
                  <p class="text-xs text-gray-400">最受关注的问题</p>
                </div>
              </div>
              <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
                <RouterLink 
                  v-for="(question, index) in hotQuestions" 
                  :key="question.id"
                  :to="`/question/${question.id}`"
                  class="flex items-start gap-4 p-4 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-all group"
                >
                  <span 
                    class="flex-shrink-0 w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold transition-transform group-hover:scale-110"
                    :class="{
                      'bg-gradient-to-br from-red-500 to-orange-500 text-white shadow-lg shadow-red-500/30': index === 0,
                      'bg-gradient-to-br from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-500/30': index === 1,
                      'bg-gradient-to-br from-yellow-500 to-orange-400 text-white shadow-lg shadow-yellow-500/30': index === 2,
                      'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400': index > 2
                    }"
                  >
                    {{ index + 1 }}
                  </span>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-primary-500 line-clamp-2 transition-colors font-medium leading-relaxed">
                      {{ question.title }}
                    </p>
                    <p class="text-xs text-gray-400 mt-1.5 flex items-center gap-3">
                      <span class="flex items-center gap-1">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                        {{ question.answers || 0 }} 回答
                      </span>
                      <span class="flex items-center gap-1">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        {{ formatNumber(question.views) }} 浏览
                      </span>
                    </p>
                  </div>
                </RouterLink>
                
                <div v-if="hotQuestions.length === 0" class="p-8 text-center text-gray-400 text-sm">
                  暂无热门问题
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <RouterLink 
      to="/create-question"
      class="fixed bottom-20 right-4 md:bottom-8 md:right-8 w-14 h-14 rounded-2xl bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-xl shadow-red-500/30 hover:shadow-2xl hover:shadow-red-500/40 hover:scale-110 transition-all duration-300 flex items-center justify-center z-40 group"
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
