<script setup>
import {reactive, ref} from 'vue'

const securityForm = reactive({
  commentReview: true,
  userRegister: true,
  anonymousComment: false,
  sensitiveFilter: true,
  sensitiveWords: '广告\n推广\n代写\n刷单\n兼职'
})

const securityLoading = ref(false)

const saveSecuritySettings = async () => {
  securityLoading.value = true
  await new Promise(resolve => setTimeout(resolve, 1000))
  securityLoading.value = false
  alert('安全设置保存成功！')
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">系统设置</h1>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">安全设置</h2>
      
      <div class="space-y-4">
        <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <div>
            <p class="text-sm font-medium text-gray-900 dark:text-white">评论审核</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">开启后评论需要管理员审核才能显示</p>
          </div>
          <button
            @click="securityForm.commentReview = !securityForm.commentReview"
            :class="[
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              securityForm.commentReview ? 'bg-red-500' : 'bg-gray-300 dark:bg-gray-600'
            ]"
          >
            <span
              :class="[
                'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                securityForm.commentReview ? 'translate-x-6' : 'translate-x-1'
              ]"
            />
          </button>
        </div>

        <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <div>
            <p class="text-sm font-medium text-gray-900 dark:text-white">用户注册</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">开启后允许新用户注册账号</p>
          </div>
          <button
            @click="securityForm.userRegister = !securityForm.userRegister"
            :class="[
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              securityForm.userRegister ? 'bg-red-500' : 'bg-gray-300 dark:bg-gray-600'
            ]"
          >
            <span
              :class="[
                'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                securityForm.userRegister ? 'translate-x-6' : 'translate-x-1'
              ]"
            />
          </button>
        </div>

        <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <div>
            <p class="text-sm font-medium text-gray-900 dark:text-white">匿名评论</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">开启后允许游客匿名发表评论</p>
          </div>
          <button
            @click="securityForm.anonymousComment = !securityForm.anonymousComment"
            :class="[
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              securityForm.anonymousComment ? 'bg-red-500' : 'bg-gray-300 dark:bg-gray-600'
            ]"
          >
            <span
              :class="[
                'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                securityForm.anonymousComment ? 'translate-x-6' : 'translate-x-1'
              ]"
            />
          </button>
        </div>

        <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <div>
            <p class="text-sm font-medium text-gray-900 dark:text-white">敏感词过滤</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">开启后自动过滤评论中的敏感词</p>
          </div>
          <button
            @click="securityForm.sensitiveFilter = !securityForm.sensitiveFilter"
            :class="[
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              securityForm.sensitiveFilter ? 'bg-red-500' : 'bg-gray-300 dark:bg-gray-600'
            ]"
          >
            <span
              :class="[
                'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
                securityForm.sensitiveFilter ? 'translate-x-6' : 'translate-x-1'
              ]"
            />
          </button>
        </div>
      </div>

      <div v-if="securityForm.sensitiveFilter" class="mt-6">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">敏感词列表</label>
        <textarea
          v-model="securityForm.sensitiveWords"
          rows="6"
          class="w-full px-4 py-2.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 text-gray-900 dark:text-white placeholder-gray-500 transition-colors resize-none font-mono"
          placeholder="每行输入一个敏感词"
        ></textarea>
        <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">每行输入一个敏感词，保存后生效</p>
      </div>

      <div class="flex justify-end pt-6 mt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          @click="saveSecuritySettings"
          :disabled="securityLoading"
          :class="[
            'px-6 py-2.5 bg-red-500 text-white rounded-lg font-medium text-sm transition-colors',
            securityLoading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-600'
          ]"
        >
          <span v-if="securityLoading" class="flex items-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            保存中...
          </span>
          <span v-else>保存设置</span>
        </button>
      </div>
    </div>
  </div>
</template>
