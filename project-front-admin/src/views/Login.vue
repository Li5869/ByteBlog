<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {adminAuthApi, isLoggedIn as checkIsLoggedIn, saveAuth} from '../utils/request.js'

const router = useRouter()

const loginForm = ref({
  username: '',
  password: ''
})

const loginError = ref('')
const isLoggingIn = ref(false)

if (checkIsLoggedIn()) {
  router.push('/dashboard')
}

const handleLogin = async () => {
  loginError.value = ''
  
  if (!loginForm.value.username || !loginForm.value.password) {
    loginError.value = '请输入用户名和密码'
    return
  }
  
  isLoggingIn.value = true
  
  try {
    const data = await adminAuthApi.login({
      username: loginForm.value.username,
      password: loginForm.value.password
    })

    // 保存 Access Token、Refresh Token 和用户信息
    saveAuth(data.token, data, data.refreshToken)
    router.push('/dashboard')
  } catch (error) {
    loginError.value = error.message || '用户名或密码错误'
  } finally {
    isLoggingIn.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-900 dark:to-gray-800 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl overflow-hidden">
        <div class="bg-gradient-to-r from-red-500 to-orange-500 px-8 py-6">
          <h1 class="text-2xl font-bold text-white text-center">管理后台登录</h1>
          <p class="text-white/80 text-center text-sm mt-1">个人博客管理系统</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="p-8 space-y-6">
          <div v-if="loginError" class="bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm px-4 py-3 rounded-lg">
            {{ loginError }}
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              用户名
            </label>
            <div class="relative">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="请输入用户名/邮箱/手机号"
                class="w-full pl-10 pr-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
              />
            </div>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              密码
            </label>
            <div class="relative">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                class="w-full pl-10 pr-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
              />
            </div>
          </div>
          
          <button
            type="submit"
            :disabled="isLoggingIn"
            class="w-full py-3 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-lg font-medium hover:from-red-600 hover:to-orange-600 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="isLoggingIn" class="flex items-center justify-center gap-2">
              <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              登录中...
            </span>
            <span v-else>登录</span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
