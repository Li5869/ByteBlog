<script setup>
import {useRoute, useRouter} from 'vue-router'
import {computed, onMounted, ref, watch} from 'vue'
import {interactionApi, isLoggedIn, questionApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'
import {useUserStore} from '@/stores/user'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import {formatAbsoluteDate, formatNumber, formatRelativeTime} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const questionId = computed(() => route.params.id)

const isAuthor = computed(() => {
  if (!question.value || !userStore.state.userInfo) return false
  return question.value.author?.id === userStore.state.userInfo.id
})

const loading = ref(true)
const error = ref(null)
const question = ref(null)
const newAnswer = ref('')
const isLiking = ref(false)

// 相关问题
const relatedQuestions = ref([])
const relatedLoading = ref(false)

const answers = ref([])
const answerCurrent = ref(1)
const answerSize = ref(10)
const answerTotal = ref(0)
const answerSortBy = ref('best')
const answerLoading = ref(false)
const submittingAnswer = ref(false)
const showAnswers = ref(false)

const fetchQuestionDetail = async () => {
  try {
    loading.value = true
    error.value = null
    
    const data = await questionApi.getQuestionDetail(questionId.value)
    question.value = data
    
    // 加载相关问题（热门问题）
    fetchRelatedQuestions()
  } catch (e) {
    error.value = e.message || '加载问题失败'
    console.error('加载问题失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchRelatedQuestions = async () => {
  const tags = question.value?.tags
  if (!tags || tags.length === 0) {
    relatedLoading.value = false
    return
  }

  relatedLoading.value = true
  try {
    // 取第一个标签进行筛选
    const data = await questionApi.getRelatedQuestions(tags[0].id, questionId.value, 6)
    const list = Array.isArray(data) ? data : (data.records || [])
    // 排除当前问题（后端没做前置过滤，前端兜底）
    relatedQuestions.value = list.filter(q => String(q.id) !== String(questionId.value)).slice(0, 5)
  } catch (e) {
    console.error('获取相关问题失败:', e)
  } finally {
    relatedLoading.value = false
  }
}

const fetchAnswers = async () => {
  if (answerLoading.value) return
  
  answerLoading.value = true
  try {
    const data = await questionApi.getAnswerList(questionId.value, {
      current: answerCurrent.value,
      size: answerSize.value,
      sortBy: answerSortBy.value
    })
    answers.value = data.records || []
    answerTotal.value = data.total || 0
  } catch (e) {
    console.error('获取回答列表失败:', e)
  } finally {
    answerLoading.value = false
  }
}

const toggleAnswers = async () => {
  showAnswers.value = !showAnswers.value
  if (showAnswers.value && answers.value.length === 0) {
    await fetchAnswers()
  }
}

const handleSortChange = (sort) => {
  answerSortBy.value = sort
  answerCurrent.value = 1
  fetchAnswers()
}

const handleAnswerPageChange = (page) => {
  answerCurrent.value = page
  fetchAnswers()
}

const handleDelete = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  const confirmed = await modal.confirm('确定要删除这个问题吗？此操作不可恢复。', {
    title: '删除问题',
    confirmText: '删除',
    icon: 'error'
  })
  if (!confirmed) return
  
  try {
    await questionApi.deleteQuestion(questionId.value)
    toast.success('删除成功')
    router.push('/qa')
  } catch (e) {
    console.error('删除问题失败:', e)
    toast.error(e.message || '删除问题失败')
  }
}

const handleLike = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  if (isLiking.value) return
  
  isLiking.value = true
  try {
    const newIsLike = !question.value.isLiked
    const data = await interactionApi.toggleLike(
      question.value.id, 
      'question', 
      newIsLike, 
      question.value.author?.id,
      question.value.title
    )
    question.value.isLiked = newIsLike
    question.value.likes = data.likes
    toast.success(newIsLike ? '点赞成功' : '已取消点赞')
  } catch (e) {
    console.error('点赞失败:', e)
    toast.error(e.message || '操作失败')
  } finally {
    isLiking.value = false
  }
}

const submitAnswer = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  if (!newAnswer.value.trim()) {
    toast.error('回答内容不能为空')
    return
  }
  
  if (newAnswer.value.trim().length > 5000) {
    toast.error('回答内容不能超过5000字符')
    return
  }
  
  submittingAnswer.value = true
  try {
    await questionApi.createAnswer(questionId.value, { content: newAnswer.value.trim() })
    toast.success('回答提交成功')
    newAnswer.value = ''
    question.value.answers = Number(question.value.answers || 0) + 1
    answerSortBy.value = 'newest'
    answerCurrent.value = 1
    await fetchAnswers()
  } catch (e) {
    console.error('提交回答失败:', e)
    toast.error(e.message || '提交回答失败')
  } finally {
    submittingAnswer.value = false
  }
}

const handleDeleteAnswer = async (answerId) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  const confirmed = await modal.confirm('确定要删除这个回答吗？此操作不可恢复。', {
    title: '删除回答',
    confirmText: '删除',
    icon: 'error'
  })
  if (!confirmed) return
  
  try {
    await questionApi.deleteAnswer(answerId)
    toast.success('删除成功')
    question.value.answers = Math.max(0, (question.value.answers || 1) - 1)
    await fetchAnswers()
  } catch (e) {
    console.error('删除回答失败:', e)
    toast.error(e.message || '删除回答失败')
  }
}

const handleAnswerLike = async (answer) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  try {
    const newIsLike = !answer.isLiked
    const data = await interactionApi.toggleLike(
      answer.id, 
      'answer', 
      newIsLike, 
      answer.author?.id,
      question.value.title,
      answer.content
    )
    answer.isLiked = newIsLike
    answer.likes = data.likes
    toast.success(newIsLike ? '点赞成功' : '已取消点赞')
  } catch (e) {
    console.error('点赞失败:', e)
    toast.error(e.message || '操作失败')
  }
}

// 采纳最佳答案
const handleAcceptAnswer = async (answer) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  const confirmed = await modal.confirm('确定要采纳这个回答为最佳答案吗？采纳后将标记问题为已解决。', {
    title: '采纳最佳答案',
    icon: 'success'
  })
  if (!confirmed) return
  
  try {
    const result = await questionApi.acceptBestAnswer(answer.id)
    toast.success('已采纳为最佳答案')
    
    // 更新当前问题的解决状态
    question.value.isSolved = result.isSolved
    
    // 更新回答列表中的最佳答案状态
    answers.value.forEach(a => {
      a.isBest = a.id === result.answerId
    })
  } catch (e) {
    console.error('采纳失败:', e)
    toast.error(e.message || '采纳失败')
  }
}

const goToUserHome = (userId) => {
  const currentUserId = userStore.state.userInfo?.id
  if (currentUserId && String(userId) === String(currentUserId)) {
    router.push('/mine')
  } else {
    router.push(`/user/${userId}`)
  }
}


watch(questionId, () => {
  if (questionId.value) {
    showAnswers.value = false
    fetchQuestionDetail()
  }
})

onMounted(() => {
  fetchQuestionDetail()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div v-if="loading" class="flex items-center justify-center min-h-screen">
      <div class="flex flex-col items-center gap-4">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-500"></div>
        <div class="text-gray-500 dark:text-gray-400 font-medium">加载中...</div>
      </div>
    </div>
    
    <div v-else-if="error" class="flex items-center justify-center min-h-screen">
      <div class="text-center">
        <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
          <svg class="w-10 h-10 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div class="text-red-500 font-medium">{{ error }}</div>
      </div>
    </div>
    
    <div v-else-if="question" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6">
      <button 
        @click="router.push('/qa')"
        class="flex items-center gap-2 text-gray-500 dark:text-gray-400 hover:text-primary-500 transition-colors mb-4"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        <span class="font-medium">返回列表</span>
      </button>

      <div class="flex flex-col lg:flex-row gap-4 sm:gap-6">
        <div class="lg:w-64 flex-shrink-0 hidden lg:block">
          <div class="sticky top-20 space-y-4 sm:space-y-6">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="p-4 sm:p-5">
                <div class="flex items-center gap-3 mb-4">
                  <img 
                    :src="question.author?.avatar || DEFAULT_AVATAR" 
                    :alt="question.author?.name"
                    class="w-14 h-14 rounded-xl cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-gray-100 dark:ring-gray-700"
                    @click="goToUserHome(question.author?.id)"
                  />
                  <div class="flex-1 min-w-0">
                    <h4 
                      class="font-bold text-gray-900 dark:text-white cursor-pointer hover:text-primary-500 transition-colors truncate"
                      @click="goToUserHome(question.author?.id)"
                    >{{ question.author?.name || '匿名用户' }}</h4>
                    <p class="text-xs text-gray-400 mt-0.5">提问者</p>
                  </div>
                </div>

                <div class="grid grid-cols-3 gap-1 text-center mb-4">
                  <div class="py-2 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(question.views) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">浏览</div>
                  </div>
                  <div class="py-2 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(question.answers) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">回答</div>
                  </div>
                  <div class="py-2 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(question.likes) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">点赞</div>
                  </div>
                </div>

                <button 
                  v-if="isAuthor"
                  @click="handleDelete"
                  class="w-full px-3 py-2.5 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-xl text-sm hover:bg-red-100 dark:hover:bg-red-900/30 transition-all font-medium"
                >
                  删除问题
                </button>
              </div>
            </div>

            <!-- 相关问题模块 -->
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-4 pt-4 pb-1 flex items-center gap-2">
                <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-orange-400 to-rose-500 flex items-center justify-center shadow-sm shadow-orange-500/20 flex-shrink-0">
                  <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
                  </svg>
                </div>
                <h3 class="text-sm font-bold text-gray-900 dark:text-white">相关问题</h3>
              </div>

              <!-- 骨架屏 -->
              <div v-if="relatedLoading" class="px-4 pb-4 pt-3 space-y-3">
                <div v-for="i in 4" :key="i" class="flex items-start gap-2">
                  <div class="w-1 h-1 rounded-full bg-gray-200 dark:bg-gray-600 mt-2 flex-shrink-0 animate-pulse"></div>
                  <div class="flex-1 space-y-1.5">
                    <div class="h-3.5 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-full"></div>
                    <div class="h-3.5 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-3/4"></div>
                  </div>
                </div>
              </div>

              <!-- 问题列表 -->
              <div v-else-if="relatedQuestions.length > 0" class="px-4 pb-4 pt-3 space-y-1">
                <button
                  v-for="(rq, index) in relatedQuestions"
                  :key="rq.id"
                  @click="router.push(`/qa/${rq.id}`)"
                  class="w-full text-left group flex items-start gap-2.5 py-2 px-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-all duration-200"
                >
                  <span 
                    class="flex-shrink-0 w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold mt-0.5 transition-colors"
                    :class="index < 3 
                      ? 'bg-gradient-to-br from-orange-400 to-rose-500 text-white shadow-sm' 
                      : 'bg-gray-100 dark:bg-gray-700 text-gray-400 dark:text-gray-500'"
                  >{{ index + 1 }}</span>
                  <div class="flex-1 min-w-0">
                    <p class="text-xs text-gray-700 dark:text-gray-300 leading-snug group-hover:text-orange-500 dark:group-hover:text-orange-400 transition-colors line-clamp-2">
                      {{ rq.title }}
                    </p>
                    <div class="flex items-center gap-2.5 mt-1">
                      <span class="flex items-center gap-1 text-xs text-gray-400">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                        {{ formatNumber(rq.answers || 0) }}
                      </span>
                      <span class="flex items-center gap-1 text-xs text-gray-400">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        {{ formatNumber(rq.views || 0) }}
                      </span>
                      <span v-if="rq.isSolved" class="text-xs text-green-500 font-medium">已解决</span>
                    </div>
                  </div>
                </button>
              </div>

              <!-- 空状态 -->
              <div v-else class="px-4 pb-4 pt-3 text-center">
                <p class="text-xs text-gray-400">暂无相关问题</p>
              </div>

              <!-- 查看更多 -->
              <div class="px-4 pb-3 border-t border-gray-100 dark:border-gray-700/50 pt-2" v-if="relatedQuestions.length > 0">
                <button 
                  @click="router.push('/qa')"
                  class="w-full text-center text-xs text-gray-400 hover:text-orange-500 dark:hover:text-orange-400 transition-colors py-1 font-medium"
                >
                  查看全部问题 →
                </button>
              </div>
            </div>

          </div>
        </div>

        <div class="flex-1 min-w-0">
          <article class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
            <div class="p-4 sm:p-6 lg:p-8">
              <header class="mb-6 sm:mb-8">
                <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 mb-4 flex-wrap">
                  <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    {{ formatAbsoluteDate(question.createdAt) }}
                  </span>
                  <span 
                    v-if="question.isSolved"
                    class="flex items-center gap-1.5 px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-xs font-semibold rounded-lg"
                  >
                    <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    已解决
                  </span>
                </div>
                
                <h1 class="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6 leading-tight">
                  {{ question.title }}
                </h1>

                <div class="flex flex-wrap gap-2 mb-4 sm:mb-6">
                  <span 
                    v-for="tag in (question.tags || [])" 
                    :key="tag.id"
                    class="px-3 py-1.5 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm rounded-lg font-medium hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors"
                  >
                    #{{ tag.name }}
                  </span>
                </div>
              </header>

              <div class="prose prose-base sm:prose-lg dark:prose-invert max-w-none mb-6 sm:mb-8">
                <div class="text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-wrap">
                  {{ question.content }}
                </div>
              </div>

              <div class="flex items-center justify-center gap-6 sm:gap-10 py-6 sm:py-8 border-t border-gray-100 dark:border-gray-700">
                <button 
                  @click="handleLike"
                  :disabled="isLiking"
                  class="flex flex-col items-center gap-2 group"
                >
                  <div 
                    class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
                    :class="question.isLiked 
                      ? 'bg-gradient-to-br from-red-500 to-rose-500 shadow-lg shadow-red-500/30' 
                      : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-red-50 dark:group-hover:bg-red-900/20'"
                  >
                    <svg 
                      class="w-6 h-6 transition-colors" 
                      :class="question.isLiked ? 'text-white' : 'text-gray-500 dark:text-gray-400 group-hover:text-red-500'"
                      :fill="question.isLiked ? 'currentColor' : 'none'" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                  </div>
                  <span class="text-sm font-medium" :class="question.isLiked ? 'text-red-500' : 'text-gray-500 dark:text-gray-400'">{{ formatNumber(question.likes) }}</span>
                </button>

                <button 
                  @click="toggleAnswers"
                  class="flex flex-col items-center gap-2 group"
                >
                  <div 
                    class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
                    :class="showAnswers 
                      ? 'bg-gradient-to-br from-blue-500 to-cyan-500 shadow-lg shadow-blue-500/30' 
                      : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-blue-50 dark:group-hover:bg-blue-900/20'"
                  >
                    <svg 
                      class="w-6 h-6 transition-colors" 
                      :class="showAnswers ? 'text-white' : 'text-gray-500 dark:text-gray-400 group-hover:text-blue-500'"
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                  </div>
                  <span class="text-sm font-medium" :class="showAnswers ? 'text-blue-500' : 'text-gray-500 dark:text-gray-400'">{{ formatNumber(question.answers) }}</span>
                </button>
              </div>
            </div>
          </article>

          <transition
            enter-active-class="transition duration-300 ease-out"
            enter-from-class="opacity-0 max-h-0"
            enter-to-class="opacity-100 max-h-[3000px]"
            leave-active-class="transition duration-200 ease-in"
            leave-from-class="opacity-100 max-h-[3000px]"
            leave-to-class="opacity-0 max-h-0"
          >
            <div v-if="showAnswers" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-4 sm:p-6 mt-4 sm:mt-6 overflow-hidden">
              <div class="flex items-center justify-between mb-4 sm:mb-6">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                  </div>
                  <h3 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    回答 <span class="text-blue-500">({{ formatNumber(question.answers) }})</span>
                  </h3>
                </div>
                
                <div class="flex items-center gap-1 sm:gap-2">
                  <button 
                    v-for="item in [
                      { key: 'best', label: '最佳' },
                      { key: 'newest', label: '最新' },
                      { key: 'votes', label: '投票' }
                    ]" 
                    :key="item.key"
                    @click="handleSortChange(item.key)"
                    class="px-2 sm:px-3 py-1.5 text-xs sm:text-sm rounded-lg transition-all"
                    :class="answerSortBy === item.key 
                      ? 'bg-blue-500 text-white' 
                      : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'"
                  >
                    {{ item.label }}
                  </button>
                </div>
              </div>

              <div class="mb-6 sm:mb-8">
                <div class="flex gap-3">
                  <img 
                    :src="userStore.state.userInfo?.avatar || DEFAULT_AVATAR" 
                    alt="用户头像"
                    class="w-10 h-10 rounded-xl flex-shrink-0 ring-2 ring-gray-100 dark:ring-gray-700"
                  />
                  <div class="flex-1">
                    <textarea 
                      v-model="newAnswer"
                      placeholder="写下你的回答..."
                      class="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-gray-50 dark:bg-gray-700/50 focus:bg-white dark:focus:bg-gray-700 text-gray-900 dark:text-white text-sm transition-all"
                      rows="4"
                      :disabled="submittingAnswer"
                    ></textarea>
                    <div class="flex justify-between items-center mt-3">
                      <span class="text-xs text-gray-400">{{ newAnswer.length }}/5000</span>
                      <button 
                        @click="submitAnswer"
                        :disabled="!newAnswer.trim() || submittingAnswer"
                        class="px-5 py-2 bg-gradient-to-r from-blue-500 to-cyan-500 text-white rounded-xl hover:from-blue-600 hover:to-cyan-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm font-semibold shadow-lg shadow-blue-500/20"
                      >
                        {{ submittingAnswer ? '提交中...' : '提交回答' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="answerLoading" class="space-y-4">
                <div v-for="i in 3" :key="i" class="p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                  <div class="flex gap-3">
                    <div class="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse"></div>
                    <div class="flex-1 space-y-2">
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-24"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="answers.length > 0" class="space-y-4">
                <div 
                  v-for="answer in answers" 
                  :key="answer.id"
                  class="p-4 rounded-xl bg-gray-50 dark:bg-gray-700/30 hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors"
                  :class="{ 'ring-2 ring-green-500 dark:ring-green-400 bg-green-50/50 dark:bg-green-900/10': answer.isBest }"
                >
                  <div class="flex gap-3">
                    <img 
                      :src="answer.author?.avatar || DEFAULT_AVATAR" 
                      :alt="answer.author?.name"
                      class="w-10 h-10 rounded-xl flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-gray-100 dark:ring-gray-700"
                      @click="goToUserHome(answer.author?.id)"
                    />
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2 mb-2">
                        <div class="flex items-center gap-2">
                          <span 
                            class="font-semibold text-gray-900 dark:text-white text-sm cursor-pointer hover:text-blue-500 transition-colors"
                            @click="goToUserHome(answer.author?.id)"
                          >{{ answer.author?.name || '匿名用户' }}</span>
                          <span class="text-xs text-gray-400">{{ formatRelativeTime(answer.createdAt) }}</span>
                          <span 
                            v-if="answer.isBest"
                            class="flex items-center gap-1 px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-xs font-medium rounded-lg"
                          >
                            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                            </svg>
                            最佳答案
                          </span>
                        </div>
                        <button 
                          v-if="answer.author?.id === userStore.state.userInfo?.id"
                          @click="handleDeleteAnswer(answer.id)"
                          class="flex items-center gap-1.5 text-xs text-gray-500 hover:text-red-500 transition-colors"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                          <span class="font-medium">删除</span>
                        </button>
                        <!-- 采纳按钮：问题作者，且不是自己的回答，且未采纳 -->
                        <button 
                          v-if="isAuthor && answer.author?.id !== userStore.state.userInfo?.id && !answer.isBest"
                          @click="handleAcceptAnswer(answer)"
                          class="flex items-center gap-1.5 text-xs text-green-600 dark:text-green-400 hover:text-green-700 dark:hover:text-green-300 transition-colors"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                          </svg>
                          <span class="font-medium">采纳</span>
                        </button>
                      </div>
                      <p class="text-gray-700 dark:text-gray-300 mb-3 text-sm leading-relaxed whitespace-pre-wrap">{{ answer.content }}</p>
                      <div class="flex items-center gap-4">
                        <button 
                          @click="handleAnswerLike(answer)"
                          class="flex items-center gap-1.5 text-sm transition-colors"
                          :class="answer.isLiked ? 'text-red-500' : 'text-gray-500 hover:text-red-500'"
                        >
                          <svg 
                            class="w-4 h-4 transition-transform hover:scale-110" 
                            :fill="answer.isLiked ? 'currentColor' : 'none'" 
                            stroke="currentColor" 
                            viewBox="0 0 24 24"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                          </svg>
                          <span class="font-medium">{{ formatNumber(answer.likes) }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div v-if="answerTotal > answerSize" class="flex justify-center pt-4">
                  <button 
                    v-if="answerCurrent * answerSize < answerTotal"
                    @click="handleAnswerPageChange(answerCurrent + 1)"
                    class="px-4 py-2 text-sm text-gray-500 dark:text-gray-400 hover:text-blue-500 transition-colors"
                  >
                    加载更多回答...
                  </button>
                </div>
              </div>

              <div v-else class="text-center py-12">
                <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                  <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <p class="text-gray-400 font-medium">暂无回答</p>
                <p class="text-sm text-gray-400 dark:text-gray-500 mt-1">快来抢沙发吧！</p>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.prose {
  @apply text-gray-700 dark:text-gray-300;
}

.prose p {
  @apply mb-4 leading-relaxed;
}
</style>
