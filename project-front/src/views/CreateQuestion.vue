<script setup>
import {onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useMessage} from 'naive-ui'
import {questionApi, tagApi} from '@/utils/request'

const router = useRouter()
const message = useMessage()

const question = reactive({
  title: '',
  content: '',
  tagIds: []
})

const errors = reactive({
  title: '',
  content: ''
})

const isSubmitting = ref(false)
const tags = ref([])
const selectedTags = ref([])
const loadingTags = ref(false)

const fetchTags = async () => {
  loadingTags.value = true
  try {
    const data = await tagApi.getTags()
    tags.value = data || []
  } catch (error) {
    console.error('获取标签列表失败:', error)
  } finally {
    loadingTags.value = false
  }
}

const toggleTag = (tagId) => {
  const index = selectedTags.value.indexOf(tagId)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    if (selectedTags.value.length >= 5) {
      message.warning('最多选择5个标签')
      return
    }
    selectedTags.value.push(tagId)
  }
  question.tagIds = [...selectedTags.value]
}

const validateForm = () => {
  let isValid = true
  errors.title = ''
  errors.content = ''

  if (!question.title.trim()) {
    errors.title = '请输入问题标题'
    isValid = false
  } else if (question.title.length > 100) {
    errors.title = '问题标题不能超过100个字符'
    isValid = false
  }

  if (!question.content.trim()) {
    errors.content = '请输入问题内容'
    isValid = false
  }

  return isValid
}

const handleSubmit = async () => {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    await questionApi.createQuestion(question)
    message.success('问题发布成功')
    router.push('/qa')
  } catch (error) {
    console.error('提交失败:', error)
    message.error(error.message || '发布问题失败')
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => {
  router.back()
}

const handleTitleInput = () => {
  if (question.title.length > 100) {
    question.title = question.title.slice(0, 100)
  }
  if (errors.title && question.title.trim()) {
    errors.title = ''
  }
}

const handleContentInput = () => {
  if (errors.content && question.content.trim()) {
    errors.content = ''
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 ">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
      <button 
        @click="goBack"
        class="flex items-center gap-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 mb-4 transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        <span>返回</span>
      </button>

      <div class="bg-gradient-to-r from-red-500 to-orange-500 rounded-lg p-4 sm:p-6 mb-4 sm:mb-6 text-white">
        <h1 class="text-xl sm:text-2xl font-bold mb-1 sm:mb-2">提出问题</h1>
        <p class="text-sm sm:text-base text-white/80">有问题？在这里提问，让技术大牛帮你解答</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-lg p-4 sm:p-6">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              问题标题 <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="question.title"
              @input="handleTitleInput"
              type="text"
              placeholder="请输入问题标题，简洁明了地描述你的问题"
              class="w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-colors"
              :class="errors.title ? 'border-red-500 dark:border-red-500' : 'border-gray-300 dark:border-gray-600'"
            />
            <div class="flex justify-between mt-1">
              <span v-if="errors.title" class="text-sm text-red-500">{{ errors.title }}</span>
              <span v-else class="text-sm text-transparent">-</span>
              <span class="text-sm text-gray-400">{{ question.title.length }}/100</span>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              问题内容 <span class="text-red-500">*</span>
            </label>
            <textarea 
              v-model="question.content"
              @input="handleContentInput"
              placeholder="详细描述你的问题，包括你尝试过的方法、期望的结果等"
              rows="10"
              class="w-full px-4 py-3 border rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-red-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-colors"
              :class="errors.content ? 'border-red-500 dark:border-red-500' : 'border-gray-300 dark:border-gray-600'"
            ></textarea>
            <span v-if="errors.content" class="text-sm text-red-500">{{ errors.content }}</span>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              选择标签（最多5个）
            </label>
            <div v-if="loadingTags" class="flex items-center justify-center py-4">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500"></div>
            </div>
            <div v-else-if="tags.length > 0" class="flex flex-wrap gap-2">
              <button
                v-for="tag in tags"
                :key="tag.id"
                type="button"
                @click="toggleTag(tag.id)"
                class="px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
                :class="selectedTags.includes(tag.id)
                  ? 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-md'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-600'"
              >
                {{ tag.name }}
              </button>
            </div>
            <p v-else class="text-sm text-gray-400">暂无可用标签</p>
          </div>

          <div class="flex items-center justify-end gap-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button 
              type="button"
              @click="goBack"
              class="px-6 py-2.5 text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors font-medium"
            >
              取消
            </button>
            <button 
              type="submit"
              :disabled="isSubmitting"
              class="px-6 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium flex items-center gap-2"
            >
              <svg v-if="isSubmitting" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ isSubmitting ? '发布中...' : '发布问题' }}
            </button>
          </div>
        </form>
      </div>

      <div class="mt-6 bg-white dark:bg-gray-800 rounded-lg p-4 sm:p-6">
        <h3 class="text-base font-bold text-gray-900 dark:text-white mb-3">提问技巧</h3>
        <ul class="space-y-2 text-sm text-gray-600 dark:text-gray-400">
          <li class="flex items-start gap-2">
            <svg class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>标题要简洁明了，准确描述问题的核心</span>
          </li>
          <li class="flex items-start gap-2">
            <svg class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>详细描述问题背景、你尝试过的方法以及期望的结果</span>
          </li>
          <li class="flex items-start gap-2">
            <svg class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>如果有错误信息或代码片段，请一并附上</span>
          </li>
          <li class="flex items-start gap-2">
            <svg class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>使用代码块格式化代码，提高可读性</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
