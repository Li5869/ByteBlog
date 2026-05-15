<script setup>
import {ref} from 'vue'
import {adminQuestionApi} from '../utils/request'
import {formatAbsoluteDate} from '@/utils/format'

const props = defineProps({
  question: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'refresh'])

const answers = ref(props.question.answers || [])

const deleteAnswer = async (answer) => {
  if (confirm('确定要删除该回答吗？')) {
    try {
      await adminQuestionApi.deleteAnswer(answer.id)
      // 从列表中移除
      const index = answers.value.findIndex(a => a.id === answer.id)
      if (index > -1) {
        answers.value.splice(index, 1)
      }
      emit('refresh')
    } catch (e) {
      console.error('删除回答失败:', e)
      alert(e.message || '删除失败')
    }
  }
}

const closeModal = () => {
  emit('close')
}
</script>

<template>
  <div class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex min-h-screen items-center justify-center p-4">
      <div
        class="fixed inset-0 bg-black/50 transition-opacity"
        @click="closeModal"
      ></div>

      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden flex flex-col">
        <div class="flex items-center justify-between p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex-1 pr-4">
            <h2 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white line-clamp-2">
              {{ question.title }}
            </h2>
            <div class="flex items-center gap-2 mt-2 text-sm text-gray-500 dark:text-gray-400">
              <img
                :src="question.authorAvatar"
                :alt="question.authorName"
                class="w-5 h-5 rounded-full object-cover"
              />
              <span>{{ question.authorName }}</span>
              <span>·</span>
              <span>{{ formatAbsoluteDate(question.createdAt) }}</span>
            </div>
          </div>
          <button
            @click="closeModal"
            class="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-4 sm:p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-semibold text-gray-900 dark:text-white">
              全部回答 ({{ answers.length }})
            </h3>
          </div>

          <div class="space-y-4">
            <div
              v-for="answer in answers"
              :key="answer.id"
              class="p-4 rounded-lg border transition-colors"
              :class="answer.isBest
                ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800'
                : 'bg-gray-50 dark:bg-gray-700/50 border-gray-200 dark:border-gray-600'"
            >
              <div class="flex items-start gap-3">
                <img
                  :src="answer.authorAvatar"
                  :alt="answer.authorName"
                  class="w-10 h-10 rounded-full object-cover flex-shrink-0"
                />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="font-medium text-gray-900 dark:text-white">{{ answer.authorName }}</span>
                    <span
                      v-if="answer.isBest"
                      class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400"
                    >
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                      </svg>
                      最佳答案
                    </span>
                    <span class="text-xs text-gray-500 dark:text-gray-400">{{ formatAbsoluteDate(answer.createdAt) }}</span>
                  </div>

                  <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed mb-3">
                    {{ answer.content }}
                  </p>

                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                      </svg>
                      <span>{{ answer.likeCount }} 赞</span>
                    </div>

                    <div class="flex items-center gap-2">
                      <button
                        @click="deleteAnswer(answer)"
                        class="inline-flex items-center gap-1 px-3 py-1 text-xs font-medium rounded-lg text-red-600 dark:text-red-400 bg-red-100 dark:bg-red-900/30 hover:bg-red-200 dark:hover:bg-red-900/50 transition-colors"
                      >
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                        删除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="answers.length === 0"
            class="text-center py-12"
          >
            <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <p class="text-gray-500 dark:text-gray-400">暂无回答</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
