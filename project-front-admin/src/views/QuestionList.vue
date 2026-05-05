<script setup>
import {onMounted, ref} from 'vue'
import AnswerList from './AnswerList.vue'
import {adminQuestionApi} from '../utils/request'

const searchQuery = ref('')
const statusFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const showAnswerModal = ref(false)
const selectedQuestion = ref(null)
const questions = ref([])

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待回答', value: 'pending' },
  { label: '已解决', value: 'solved' }
]

const fetchQuestions = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) params.keyword = searchQuery.value
    if (statusFilter.value !== 'all') params.status = statusFilter.value

    const res = await adminQuestionApi.getPage(params)
    questions.value = res.records || []
    total.value = res.total || 0
    totalPages.value = res.pages || 0
  } catch (e) {
    console.error('获取问题列表失败:', e)
  } finally {
    loading.value = false
  }
}

let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchQuestions()
  }, 300)
}

const onStatusChange = (status) => {
  statusFilter.value = status
  currentPage.value = 1
  fetchQuestions()
}

const getStatusText = (status) => {
  const map = { pending: '待回答', solved: '已解决' }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    pending: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    solved: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400'
  }
  return map[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const viewAnswers = async (question) => {
  try {
    const detail = await adminQuestionApi.getDetail(question.id)
    selectedQuestion.value = { ...question, answers: detail.answers || [] }
    showAnswerModal.value = true
  } catch (e) {
    console.error('获取问题详情失败:', e)
  }
}

const deleteQuestion = async (question) => {
  if (confirm(`确定要删除问题 "${question.title}" 吗？`)) {
    try {
      await adminQuestionApi.delete(question.id)
      fetchQuestions()
    } catch (e) {
      console.error('删除失败:', e)
      alert(e.message || '删除失败')
    }
  }
}

const handlePageChange = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchQuestions()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchQuestions()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchQuestions()
  }
}

const closeAnswerModal = () => {
  showAnswerModal.value = false
  selectedQuestion.value = null
}

onMounted(() => {
  fetchQuestions()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-7xl mx-auto">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">问答管理</h1>
          </div>
        </div>

        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row gap-4">
            <div class="flex-1">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  @input="onSearch"
                  type="text"
                  placeholder="搜索问题标题..."
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
                />
              </div>
            </div>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                @click="onStatusChange(option.value)"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="statusFilter === option.value
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">ID</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">问题标题</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">提问者</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">回答数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">浏览量</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">创建时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="question in questions"
                :key="question.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">{{ question.id }}</td>
                <td class="px-4 py-4 text-sm text-gray-900 dark:text-white max-w-xs">
                  <div class="truncate" :title="question.title">{{ question.title }}</div>
                </td>
                <td class="px-4 py-4 whitespace-nowrap hidden sm:table-cell">
                  <div class="flex items-center gap-2">
                    <img
                      :src="question.authorAvatar"
                      :alt="question.authorName"
                      class="w-8 h-8 rounded-full object-cover"
                    />
                    <span class="text-sm text-gray-600 dark:text-gray-300">{{ question.authorName }}</span>
                  </div>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden md:table-cell">
                  <span class="inline-flex items-center gap-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    {{ question.answerCount }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">
                  <span class="inline-flex items-center gap-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ question.viewCount }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getStatusClass(question.status)"
                  >
                    {{ getStatusText(question.status) }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden xl:table-cell">{{ formatDate(question.createdAt) }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm">
                  <div class="flex items-center gap-2">
                    <button
                      @click="viewAnswers(question)"
                      class="text-primary-500 hover:text-primary-600 transition-colors"
                      title="查看回答"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                      </svg>
                    </button>
                    <button
                      @click="deleteQuestion(question)"
                      class="text-red-500 hover:text-red-600 transition-colors"
                      title="删除"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!loading && questions.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无问题数据</p>
        </div>

        <div v-if="total > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-600 dark:text-gray-400">
              共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="prevPage"
                :disabled="currentPage === 1"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                上一页
              </button>
              <div class="flex items-center gap-1">
                <button
                  v-for="page in Math.min(totalPages, 7)"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 text-sm rounded-lg transition-colors"
                  :class="currentPage === page
                    ? 'bg-primary-500 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
                >
                  {{ page }}
                </button>
              </div>
              <button
                @click="nextPage"
                :disabled="currentPage === totalPages"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AnswerList
      v-if="showAnswerModal"
      :question="selectedQuestion"
      @close="closeAnswerModal"
      @refresh="fetchQuestions"
    />
  </div>
</template>
