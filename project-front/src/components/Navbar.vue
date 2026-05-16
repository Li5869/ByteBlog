<script setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {RouterLink, useRoute, useRouter} from 'vue-router'
import {useUserStore} from '../stores/user.js'
import {toast} from '@/utils/toast'
import {interactionApi, searchApi} from '@/utils/request'
import {Events, off, on} from '@/utils/eventBus'
import OnlineIndicator from './OnlineIndicator.vue'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import sseManager from '@/utils/sse'

defineProps({
  isDark: Boolean
})

const emit = defineEmits(['toggleDark'])
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isMenuOpen = ref(false)
const searchQuery = ref('')
const showUserCard = ref(false)

// ========== 搜索建议相关 ==========
const suggestions = ref({ articles: [], questions: [], authors: [], columns: [] })
const showSuggestions = ref(false)
const selectedIndex = ref(-1)  // 键盘导航选中索引（-1 表示未选中）
const searchInputRef = ref(null)
let debounceTimer = null

// 防抖获取搜索建议
const fetchSuggestions = async (keyword) => {
  if (!keyword || keyword.trim().length < 1) {
    suggestions.value = { articles: [], questions: [], authors: [], columns: [] }
    showSuggestions.value = false
    return
  }
  try {
    const data = await searchApi.suggest(keyword.trim(), 12)
    suggestions.value = data || { articles: [], questions: [], authors: [], columns: [] }
    const hasSuggestions = suggestions.value.articles?.length > 0
        || suggestions.value.questions?.length > 0
        || suggestions.value.authors?.length > 0
        || suggestions.value.columns?.length > 0
    showSuggestions.value = hasSuggestions
    selectedIndex.value = -1
  } catch (e) {
    console.error('获取搜索建议失败:', e)
    showSuggestions.value = false
  }
}

// 防抖 wrapper（150ms）
const debouncedFetchSuggestions = (keyword) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchSuggestions(keyword)
  }, 150)
}

// 关闭建议面板
const closeSuggestions = () => {
  showSuggestions.value = false
  selectedIndex.value = -1
}

// 点击建议项
const handleSuggestionClick = (item) => {
  closeSuggestions()
  if (item.type === 'author') {
    router.push(`/user/${item.id}`)
  } else if (item.type === 'column') {
    router.push(`/column/${item.id}`)
  } else {
    router.push({ path: '/search', query: { q: item.title, type: item.type === 'article' ? 'article' : 'question' } })
  }
}

// 键盘导航
const handleKeydown = (e) => {
  if (!showSuggestions.value) return
  const total = (suggestions.value.articles?.length || 0)
      + (suggestions.value.questions?.length || 0)
      + (suggestions.value.authors?.length || 0)
      + (suggestions.value.columns?.length || 0)
  if (total === 0) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % total
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = selectedIndex.value <= 0 ? total - 1 : selectedIndex.value - 1
  } else if (e.key === 'Enter' && selectedIndex.value >= 0) {
    e.preventDefault()
    const allItems = [
      ...(suggestions.value.articles || []),
      ...(suggestions.value.questions || []),
      ...(suggestions.value.authors || []),
      ...(suggestions.value.columns || [])
    ]
    if (allItems[selectedIndex.value]) {
      handleSuggestionClick(allItems[selectedIndex.value])
    }
  } else if (e.key === 'Escape') {
    closeSuggestions()
  }
}

// 监听搜索词变化
watch(searchQuery, (newVal) => {
  debouncedFetchSuggestions(newVal)
})

// 点击外部关闭建议面板
const handleClickOutside = (e) => {
  if (searchInputRef.value && !searchInputRef.value.contains(e.target)) {
    closeSuggestions()
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  clearTimeout(debounceTimer)
})
// ========== 搜索建议 END ==========

const navLinks = [
  { name: '首页', path: '/' },
  { name: '博客', path: '/blog' },
  { name: '专栏', path: '/columns' },
  { name: '问答', path: '/qa' },
  { name: 'AI助手', path: '/ai-chat' },
  { name: '我的', path: '/mine' }
]

const mobileNavLinks = [
  { name: '首页', path: '/' },
  { name: '博客', path: '/blog' },
  { name: '专栏', path: '/columns' },
  { name: '问答', path: '/qa' },
  { name: '我的', path: '/mine' }
]

const handleNormalSearch = () => {
  if (!searchQuery.value.trim()) return
  router.push({ path: '/search', query: { q: searchQuery.value } })
}

const handleAISearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/ai-chat', query: { q: searchQuery.value } })
  } else {
    router.push('/ai-chat')
  }
}

const userInfo = computed(() => userStore.state.userInfo)

// 未读消息总数
const unreadCount = ref(0)

// 刷新未读消息数
const fetchUnreadCount = async () => {
  if (!userStore.state.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  
  try {
    const [bizRes, systemRes] = await Promise.all([
      interactionApi.getBizUnreadCount(),
      interactionApi.getSystemUnreadCount()
    ])
    
    // 累加两部分未读数（request已返回data.data）
    const bizCount = bizRes?.count || 0
    const systemCount = systemRes?.count || 0
    
    unreadCount.value = bizCount + systemCount
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

// 定时器
let refreshInterval = null

// 登录成功后调用
watch(() => userStore.state.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    fetchUnreadCount()
    // 每30秒刷新一次
    if (!refreshInterval) {
      refreshInterval = setInterval(fetchUnreadCount, 30000)
    }
  } else {
    unreadCount.value = 0
    if (refreshInterval) {
      clearInterval(refreshInterval)
      refreshInterval = null
    }
  }
}, { immediate: true })

// 组件卸载时清理
onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
  // 移除事件监听
  off(Events.REFRESH_UNREAD_COUNT, handleRefreshUnreadCount)
  off(Events.NOTIFICATION_READ, handleNotificationRead)
  
  // 移除 SSE 未读数监听
  sseManager.off('unread_update', handleUnreadUpdate)
  
  // 移除 SSE 监听
  sseManager.off('notification', handleSseNotification)
})

// 处理刷新未读数事件
const handleRefreshUnreadCount = () => {
  fetchUnreadCount()
}

// 处理通知已读事件
const handleNotificationRead = (count = 1) => {
  // 直接重新从后端获取，确保数据一致
  fetchUnreadCount()
}

// 处理 WebSocket 未读数更新
const handleUnreadUpdate = (data) => {
  if (data && data.delta !== undefined) {
    unreadCount.value = Math.max(0, unreadCount.value + data.delta)
  }
}

// 处理 SSE 通知
const handleSseNotification = (notification) => {
  // 收到新通知，未读数 +1
  unreadCount.value++
}

// 监听事件（需要在onMounted中绑定）
onMounted(() => {
  on(Events.REFRESH_UNREAD_COUNT, handleRefreshUnreadCount)
  on(Events.NOTIFICATION_READ, handleNotificationRead)
  
  // 监听 SSE 未读数更新
  sseManager.on('unread_update', handleUnreadUpdate)
  
  // 监听 SSE 通知
  sseManager.on('notification', handleSseNotification)
})

const showLoginModal = ref(false)
const loginForm = ref({
  username: '',
  password: '',
  remember: false
})
const loginError = ref('')
const isLoggingIn = ref(false)
const showLoginPassword = ref(false)

const showRegisterModal = ref(false)
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: ''
})
const registerError = ref('')
const isRegistering = ref(false)
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)

const activeTab = ref('login')

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const openLoginModal = () => {
  showLoginModal.value = true
  activeTab.value = 'login'
  loginForm.value = { username: '', password: '', remember: false }
  loginError.value = ''
}

const closeLoginModal = () => {
  showLoginModal.value = false
  loginForm.value = { username: '', password: '', remember: false }
  loginError.value = ''
}

const openRegisterModal = () => {
  showRegisterModal.value = true
  activeTab.value = 'register'
  registerForm.value = { username: '', password: '', confirmPassword: '', email: '', nickname: '' }
  registerError.value = ''
}

const closeRegisterModal = () => {
  showRegisterModal.value = false
  registerForm.value = { username: '', password: '', confirmPassword: '', email: '', nickname: '' }
  registerError.value = ''
}

const switchToRegister = () => {
  closeLoginModal()
  setTimeout(() => openRegisterModal(), 150)
}

const switchToLogin = () => {
  closeRegisterModal()
  setTimeout(() => openLoginModal(), 150)
}

const handleLogin = async () => {
  loginError.value = ''
  
  if (!loginForm.value.username || !loginForm.value.password) {
    loginError.value = '请输入用户名和密码'
    return
  }
  
  isLoggingIn.value = true
  
  try {
    await userStore.login(loginForm.value.username, loginForm.value.password)
    toast.success('登录成功')
    closeLoginModal()
    if (route.query.redirect) {
      router.push(route.query.redirect)
    }
  } catch (error) {
    loginError.value = error.message || '登录失败，请重试'
  } finally {
    isLoggingIn.value = false
  }
}

const handleRegister = async () => {
  registerError.value = ''
  
  if (!registerForm.value.username || !registerForm.value.password) {
    registerError.value = '请输入用户名和密码'
    return
  }
  
  if (registerForm.value.password.length < 6) {
    registerError.value = '密码长度至少6位'
    return
  }
  
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }
  
  isRegistering.value = true
  
  try {
    await userStore.register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      email: registerForm.value.email || null,
      nickname: registerForm.value.nickname || null
    })
    toast.success('注册成功，请登录')
    closeRegisterModal()
    showLoginModal.value = true
    loginForm.value.username = registerForm.value.username
  } catch (error) {
    registerError.value = error.message || '注册失败，请重试'
  } finally {
    isRegistering.value = false
  }
}

const handleLogout = async () => {
  await userStore.logout()
  showUserCard.value = false
  router.push('/')
}

onMounted(() => {
  userStore.checkAuth()
  if (userStore.state.isLoggedIn) {
    userStore.fetchUserInfo()
  }
})

watch(() => route.query.login, (newVal) => {
  if (newVal === 'true' && !userStore.state.isLoggedIn) {
    openLoginModal()
    if (route.query.redirect) {
      router.replace({ query: { ...route.query, login: undefined } })
    }
  }
}, { immediate: true })
</script>

<template>
  <!-- 桌面端顶部导航栏 -->
  <header class="sticky top-0 z-50 bg-white/95 dark:bg-gray-800/95 border-b border-gray-200 dark:border-gray-700 shadow-sm backdrop-blur md:block hidden">
    <nav class="max-w-7xl mx-auto px-5 lg:px-8">
      <div class="flex items-center h-14">
        <RouterLink to="/" class="flex items-center space-x-2 shrink-0">
          <div class="w-8 h-8 bg-gradient-to-br from-red-500 to-orange-500 rounded-md flex items-center justify-center">
            <span class="text-white font-bold text-sm">B</span>
          </div>
          <span class="text-lg font-bold tracking-tight text-gray-900 dark:text-white">博客</span>
        </RouterLink>

        <div class="flex flex-1 items-center gap-3 mx-8">
          <RouterLink 
            v-for="link in navLinks" 
            :key="link.path" 
            :to="link.path"
            class="flex-1 text-center px-4 py-2.5 rounded-lg text-gray-600 dark:text-gray-300 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50/80 dark:hover:bg-red-900/30 transition-all text-sm font-medium bg-gray-100/80 dark:bg-gray-700/60 border border-transparent min-h-[44px] flex items-center justify-center"
            active-class="text-red-500 dark:text-red-400 border-red-200 dark:border-red-700 bg-red-50/90 dark:bg-red-900/30"
          >
            <span class="whitespace-nowrap">{{ link.name }}</span>
          </RouterLink>
        </div>

        <div class="flex items-center gap-3 shrink-0">
          <!-- 搜索框 + 建议下拉面板 -->
          <div class="relative" ref="searchInputRef">
            <div class="flex items-center bg-gray-100/90 dark:bg-gray-700 rounded-full">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索问题、文章..."
                class="pl-4 pr-2 py-2 text-sm bg-transparent border-none rounded-full focus:outline-none text-gray-900 dark:text-white placeholder-gray-500 w-48 lg:w-64"
                @keyup.enter="handleKeydown($event) || handleNormalSearch"
                @keydown="handleKeydown"
                @focus="searchQuery && fetchSuggestions(searchQuery)"
              />
            </div>
            <!-- 搜索建议下拉面板 -->
            <transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="opacity-0 scale-95"
              enter-to-class="opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="opacity-100 scale-100"
              leave-to-class="opacity-0 scale-95"
            >
              <div v-if="showSuggestions"
                   class="absolute top-full left-0 right-0 mt-2 bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden z-50 max-h-96 overflow-y-auto">
                <!-- 文章建议 -->
                <template v-if="suggestions.articles?.length">
                  <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">文章</div>
                  <div v-for="(item, idx) in suggestions.articles" :key="'a'+item.id"
                       :class="['px-4 py-2.5 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20', selectedIndex === idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                       @click="handleSuggestionClick(item)">
                    <svg class="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                    <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
                  </div>
                </template>
                <!-- 问答建议 -->
                <template v-if="suggestions.questions?.length">
                  <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">问答</div>
                  <div v-for="(item, idx) in suggestions.questions" :key="'q'+item.id"
                       :class="['px-4 py-2.5 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20', selectedIndex === (suggestions.articles?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                       @click="handleSuggestionClick(item)">
                    <svg class="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
                  </div>
                </template>
                <!-- 博主建议 -->
                <template v-if="suggestions.authors?.length">
                  <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">博主</div>
                  <div v-for="(item, idx) in suggestions.authors" :key="'u'+item.id"
                       :class="['px-4 py-2.5 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20', selectedIndex === (suggestions.articles?.length || 0) + (suggestions.questions?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                       @click="handleSuggestionClick(item)">
                    <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                    <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
                  </div>
                </template>
                <!-- 专栏建议 -->
                <template v-if="suggestions.columns?.length">
                  <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">专栏</div>
                  <div v-for="(item, idx) in suggestions.columns" :key="'c'+item.id"
                       :class="['px-4 py-2.5 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20', selectedIndex === (suggestions.articles?.length || 0) + (suggestions.questions?.length || 0) + (suggestions.authors?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                       @click="handleSuggestionClick(item)">
                    <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                    <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
                  </div>
                </template>
              </div>
            </transition>
          </div>
          
          <button 
            @click="handleNormalSearch"
            class="flex items-center gap-2 px-4 py-2 text-sm bg-red-500 text-white rounded-full hover:bg-red-600 transition-all"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <span>搜索</span>
          </button>

          <button 
            @click="handleAISearch"
            class="flex items-center gap-2 px-4 py-2 text-sm bg-gradient-to-r from-purple-500 to-indigo-500 text-white rounded-full hover:from-purple-600 hover:to-indigo-600 transition-all"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span>AI搜索</span>
          </button>

          <button 
            @click="emit('toggleDark')"
            class="p-2 rounded-full text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
          >
            <svg v-if="isDark" class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <svg v-else class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          </button>

          <RouterLink 
            to="/notifications"
            class="relative p-2 rounded-full text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
          >
            <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span 
              v-if="unreadCount > 0 && userStore.state.isLoggedIn"
              class="absolute -top-1 -right-1 min-w-4 h-4 px-1 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center"
            >
              {{ unreadCount > 9 ? '9+' : unreadCount }}
            </span>
          </RouterLink>

          <template v-if="userStore.state.isLoggedIn && userInfo">
            <div class="relative" @mouseenter="showUserCard = true" @mouseleave="showUserCard = false">
              <img 
                :src="userInfo.avatar || DEFAULT_AVATAR" 
                alt="用户头像" 
                class="w-8 h-8 rounded-full cursor-pointer ring-2 ring-transparent hover:ring-red-500 transition-all"
              />
              <!-- 在线状态指示器 -->
              <div class="absolute -bottom-0.5 -right-0.5">
                <OnlineIndicator :userId="userInfo.id" size="sm" />
              </div>
              <transition
                enter-active-class="transition duration-200 ease-out"
                enter-from-class="opacity-0 scale-95"
                enter-to-class="opacity-100 scale-100"
                leave-active-class="transition duration-150 ease-in"
                leave-from-class="opacity-100 scale-100"
                leave-to-class="opacity-0 scale-95"
              >
                <div 
                  v-if="showUserCard"
                  class="absolute right-0 top-full mt-2 w-64 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 p-4 z-50"
                >
                  <div class="flex items-center gap-3 mb-3">
                    <img :src="userInfo.avatar || DEFAULT_AVATAR" class="w-12 h-12 rounded-full" />
                    <div class="flex-1">
                      <div class="flex items-center gap-2">
                        <span class="font-bold text-gray-900 dark:text-white">{{ userInfo.nickname || userInfo.username }}</span>
                        <span v-if="userInfo.isAdmin" class="px-1.5 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 text-xs rounded">管理员</span>
                      </div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ userInfo.bio || '这个人很懒，什么都没写' }}</p>
                    </div>
                  </div>
                  <div class="space-y-1">
                    <RouterLink to="/mine" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                      个人主页
                    </RouterLink>
                    <RouterLink to="/mine" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                      </svg>
                      我的收藏
                    </RouterLink>
                    <RouterLink to="/browse-history" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      浏览历史
                    </RouterLink>
                    <button @click="handleLogout" class="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                      退出登录
                    </button>
                  </div>
                </div>
              </transition>
            </div>
          </template>
          <template v-else>
            <div class="flex items-center gap-2">
              <button @click="openLoginModal" class="px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors">
                登录
              </button>
              <button @click="openRegisterModal" class="px-3 py-1.5 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors">
                注册
              </button>
            </div>
          </template>
        </div>
      </div>
    </nav>
  </header>

  <!-- 移动端顶部导航栏 -->
  <header class="sticky top-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm md:hidden">
    <div class="flex items-center justify-between h-14 px-4">
      <RouterLink to="/" class="flex items-center space-x-2">
        <div class="w-8 h-8 bg-gradient-to-br from-red-500 to-orange-500 rounded flex items-center justify-center">
          <span class="text-white font-bold text-sm">B</span>
        </div>
      </RouterLink>
      
      <div class="flex items-center gap-2 flex-1 mx-4 relative" ref="searchInputRef">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索..."
          class="flex-1 px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 border border-transparent rounded-full focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white placeholder-gray-500"
          @keyup.enter="handleKeydown($event) || handleNormalSearch"
          @keydown="handleKeydown"
          @focus="searchQuery && fetchSuggestions(searchQuery)"
        />

        <button
          @click="handleNormalSearch"
          class="px-3 py-1.5 text-xs bg-red-500 text-white rounded-full hover:bg-red-600 transition-colors"
        >
          搜索
        </button>
        
        <button
          @click="handleAISearch"
          class="px-3 py-1.5 text-xs bg-gradient-to-r from-purple-500 to-indigo-500 text-white rounded-full hover:from-purple-600 hover:to-indigo-600 transition-all"
        >
          AI
        </button>

        <!-- 移动端搜索建议下拉面板（fixed定位） -->
        <transition
          enter-active-class="transition ease-out duration-100"
          enter-from-class="opacity-0"
          enter-to-class="opacity-100"
          leave-active-class="transition ease-in duration-75"
          leave-from-class="opacity-100"
          leave-to-class="opacity-0"
        >
          <div v-if="showSuggestions"
               class="fixed left-0 right-0 bg-white dark:bg-gray-800 shadow-2xl border-t border-gray-200 dark:border-gray-700 z-[9999]"
               :style="{ top: '56px', maxHeight: '60vh', overflowY: 'auto' }">
            <!-- 文章建议 -->
            <template v-if="suggestions.articles?.length">
              <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">文章</div>
              <div v-for="(item, idx) in suggestions.articles" :key="'ma'+item.id"
                   :class="['px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20 active:bg-red-100', selectedIndex === idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                   @click="handleSuggestionClick(item)">
                <svg class="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
              </div>
            </template>
            <!-- 问答建议 -->
            <template v-if="suggestions.questions?.length">
              <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">问答</div>
              <div v-for="(item, idx) in suggestions.questions" :key="'mq'+item.id"
                   :class="['px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20 active:bg-red-100', selectedIndex === (suggestions.articles?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                   @click="handleSuggestionClick(item)">
                <svg class="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
              </div>
            </template>
            <!-- 博主建议 -->
            <template v-if="suggestions.authors?.length">
              <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">博主</div>
              <div v-for="(item, idx) in suggestions.authors" :key="'mu'+item.id"
                   :class="['px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20 active:bg-red-100', selectedIndex === (suggestions.articles?.length || 0) + (suggestions.questions?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                   @click="handleSuggestionClick(item)">
                <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
              </div>
            </template>
            <!-- 专栏建议 -->
            <template v-if="suggestions.columns?.length">
              <div class="px-3 py-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-700/50">专栏</div>
              <div v-for="(item, idx) in suggestions.columns" :key="'mc'+item.id"
                   :class="['px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-red-50 dark:hover:bg-red-900/20 active:bg-red-100', selectedIndex === (suggestions.articles?.length || 0) + (suggestions.questions?.length || 0) + (suggestions.authors?.length || 0) + idx ? 'bg-red-50 dark:bg-red-900/20' : '']"
                   @click="handleSuggestionClick(item)">
                <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                <span class="text-sm text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</span>
              </div>
            </template>
          </div>
        </transition>
      </div>
      
      <div class="flex items-center space-x-3">
        <button 
          @click="emit('toggleDark')"
          class="p-1.5 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
        >
          <svg v-if="isDark" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        </button>
        
        <RouterLink 
          to="/notifications"
          class="relative p-1.5 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          <span 
            v-if="unreadCount > 0 && userStore.state.isLoggedIn"
            class="absolute -top-1 -right-1 w-3 h-3 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center"
          >
            {{ unreadCount > 9 ? '9+' : unreadCount }}
          </span>
        </RouterLink>
        
        <template v-if="userStore.state.isLoggedIn && userInfo">
          <div class="relative">
            <img 
              :src="userInfo.avatar || DEFAULT_AVATAR" 
              alt="用户头像" 
              class="w-7 h-7 rounded-full cursor-pointer"
              @click="showUserCard = !showUserCard"
            />
            <!-- 在线状态指示器 -->
            <div class="absolute -bottom-0.5 -right-0.5">
              <OnlineIndicator :userId="userInfo.id" size="sm" />
            </div>
          </div>
        </template>
        <template v-else>
          <button 
            @click="openLoginModal"
            class="px-3 py-1 text-xs bg-red-500 text-white rounded-lg"
          >
            登录
          </button>
        </template>
      </div>
    </div>
  </header>

  <!-- 移动端用户卡片 -->
  <transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0 scale-95"
    enter-to-class="opacity-100 scale-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100 scale-100"
    leave-to-class="opacity-0 scale-95"
  >
    <div 
      v-if="showUserCard && userStore.state.isLoggedIn && userInfo"
      class="fixed top-14 right-4 w-64 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 p-4 z-50 md:hidden"
    >
      <div class="flex items-center gap-3 mb-3">
        <img :src="userInfo.avatar || DEFAULT_AVATAR" class="w-12 h-12 rounded-full" />
        <div class="flex-1">
          <div class="flex items-center gap-2">
            <span class="font-bold text-gray-900 dark:text-white">{{ userInfo.nickname || userInfo.username }}</span>
          </div>
          <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ userInfo.bio || '这个人很懒，什么都没写' }}</p>
        </div>
      </div>
      <div class="space-y-1">
        <RouterLink to="/mine" @click="showUserCard = false" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          个人主页
        </RouterLink>
        <RouterLink to="/browse-history" @click="showUserCard = false" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          浏览历史
        </RouterLink>
        <button @click="handleLogout(); showUserCard = false" class="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          退出登录
        </button>
      </div>
    </div>
  </transition>

  <!-- 移动端底部导航栏 -->
  <footer class="fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 z-40 md:hidden">
    <div class="flex items-center justify-around h-14">
      <RouterLink 
        v-for="link in mobileNavLinks" 
        :key="link.path" 
        :to="link.path"
        class="flex flex-col items-center justify-center px-4 py-2 text-gray-500 dark:text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors"
        active-class="text-red-500 dark:text-red-400"
      >
        <svg v-if="link.name === '首页'" class="w-5 h-5 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <svg v-else-if="link.name === '博客'" class="w-5 h-5 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <svg v-else-if="link.name === '专栏'" class="w-5 h-5 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <svg v-else-if="link.name === '问答'" class="w-5 h-5 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <svg v-else-if="link.name === '我的'" class="w-5 h-5 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span class="text-xs">{{ link.name }}</span>
      </RouterLink>
    </div>
  </footer>

  <!-- 登录弹窗 -->
  <Teleport to="body">
    <transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div 
        v-if="showLoginModal" 
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      >
        <div class="absolute inset-0 bg-black/60 backdrop-blur-md" @click="closeLoginModal"></div>
        
        <transition
          enter-active-class="transition duration-300 ease-out"
          enter-from-class="opacity-0 scale-90 translate-y-4"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition duration-200 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-90 translate-y-4"
        >
          <div 
            v-if="showLoginModal"
            class="relative w-full max-w-4xl bg-white dark:bg-gray-800 rounded-3xl shadow-2xl overflow-hidden flex min-h-[520px]"
          >
            <!-- 左侧装饰区域 -->
            <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-red-500 via-orange-500 to-pink-500 relative overflow-hidden">
              <div class="absolute inset-0 opacity-20" style="background-image: radial-gradient(circle at 30% 30%, white 1px, transparent 1px); background-size: 40px 40px;"></div>
              <div class="absolute top-20 right-20 w-64 h-64 bg-white/20 rounded-full blur-3xl"></div>
              <div class="absolute bottom-20 left-20 w-48 h-48 bg-white/10 rounded-full blur-2xl"></div>
              
              <div class="relative z-10 flex flex-col justify-center items-center p-10 text-white">
                <div class="w-20 h-20 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center mb-6 shadow-lg">
                  <span class="text-4xl font-bold">B</span>
                </div>
                <h2 class="text-3xl font-bold mb-3">欢迎回来</h2>
                <p class="text-white/80 text-center text-lg leading-relaxed">
                  登录您的账号<br/>继续探索精彩内容
                </p>
                
                <div class="mt-10 space-y-4 w-full max-w-xs">
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <span>发现优质技术文章</span>
                  </div>
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <span>与开发者交流互动</span>
                  </div>
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <span>分享您的技术见解</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 右侧表单区域 -->
            <div class="w-full lg:w-1/2 p-8 lg:p-10 flex flex-col justify-center">
              <button 
                @click="closeLoginModal"
                class="absolute top-4 right-4 p-2 rounded-full text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              
              <div class="lg:hidden flex items-center gap-3 mb-6">
                <div class="w-10 h-10 bg-gradient-to-br from-red-500 to-orange-500 rounded-xl flex items-center justify-center">
                  <span class="text-white font-bold text-lg">B</span>
                </div>
                <span class="text-xl font-bold text-gray-900 dark:text-white">博客</span>
              </div>
              
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">登录账号</h2>
              <p class="text-gray-500 dark:text-gray-400 mb-8">欢迎回来，请输入您的登录信息</p>
              
              <form @submit.prevent="handleLogin" class="space-y-5">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    用户名
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                    </div>
                    <input 
                      v-model="loginForm.username"
                      type="text" 
                      placeholder="请输入用户名"
                      class="w-full pl-12 pr-4 py-3.5 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                  </div>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    密码
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                      </svg>
                    </div>
                    <input 
                      v-model="loginForm.password"
                      :type="showLoginPassword ? 'text' : 'password'" 
                      placeholder="请输入密码"
                      class="w-full pl-12 pr-12 py-3.5 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-red-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                    <button 
                      type="button"
                      @click="showLoginPassword = !showLoginPassword"
                      class="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                    >
                      <svg v-if="!showLoginPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                      </svg>
                    </button>
                  </div>
                </div>
                
                <div class="flex items-center justify-between">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input 
                      v-model="loginForm.remember"
                      type="checkbox" 
                      class="w-4 h-4 text-red-500 border-gray-300 rounded focus:ring-red-500"
                    />
                    <span class="text-sm text-gray-600 dark:text-gray-400">记住我</span>
                  </label>
                  <a href="#" class="text-sm text-red-500 hover:text-red-600 transition-colors">忘记密码？</a>
                </div>
                
                <div v-if="loginError" class="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-xl text-sm border border-red-100 dark:border-red-800">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  {{ loginError }}
                </div>
                
                <button 
                  type="submit"
                  :disabled="isLoggingIn"
                  class="w-full py-3.5 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl font-semibold hover:from-red-600 hover:to-orange-600 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-red-500/25 hover:shadow-xl hover:shadow-red-500/30"
                >
                  <span v-if="isLoggingIn" class="flex items-center justify-center gap-2">
                    <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    登录中...
                  </span>
                  <span v-else>登 录</span>
                </button>
              </form>
              
              <div class="mt-6 text-center">
                <span class="text-gray-500 dark:text-gray-400">还没有账号？</span>
                <button @click="switchToRegister" class="text-red-500 hover:text-red-600 font-medium ml-1 transition-colors">
                  立即注册
                </button>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>

  <!-- 注册弹窗 -->
  <Teleport to="body">
    <transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div 
        v-if="showRegisterModal" 
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      >
        <div class="absolute inset-0 bg-black/60 backdrop-blur-md" @click="closeRegisterModal"></div>
        
        <transition
          enter-active-class="transition duration-300 ease-out"
          enter-from-class="opacity-0 scale-90 translate-y-4"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition duration-200 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-90 translate-y-4"
        >
          <div 
            v-if="showRegisterModal"
            class="relative w-full max-w-4xl bg-white dark:bg-gray-800 rounded-3xl shadow-2xl overflow-hidden flex min-h-[600px]"
          >
            <!-- 左侧装饰区域 -->
            <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-orange-500 via-pink-500 to-purple-500 relative overflow-hidden">
              <div class="absolute inset-0 opacity-20" style="background-image: radial-gradient(circle at 30% 30%, white 1px, transparent 1px); background-size: 40px 40px;"></div>
              <div class="absolute top-20 right-20 w-64 h-64 bg-white/20 rounded-full blur-3xl"></div>
              <div class="absolute bottom-20 left-20 w-48 h-48 bg-white/10 rounded-full blur-2xl"></div>
              
              <div class="relative z-10 flex flex-col justify-center items-center p-10 text-white">
                <div class="w-20 h-20 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center mb-6 shadow-lg">
                  <span class="text-4xl font-bold">B</span>
                </div>
                <h2 class="text-3xl font-bold mb-3">加入我们</h2>
                <p class="text-white/80 text-center text-lg leading-relaxed">
                  创建账号<br/>开启您的创作之旅
                </p>
                
                <div class="mt-10 space-y-4 w-full max-w-xs">
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                    </div>
                    <span>发布您的技术文章</span>
                  </div>
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                      </svg>
                    </div>
                    <span>建立您的技术圈子</span>
                  </div>
                  <div class="flex items-center gap-3 text-white/90">
                    <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                      </svg>
                    </div>
                    <span>获得更多成长机会</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 右侧表单区域 -->
            <div class="w-full lg:w-1/2 p-8 lg:p-10 flex flex-col justify-center">
              <button 
                @click="closeRegisterModal"
                class="absolute top-4 right-4 p-2 rounded-full text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              
              <div class="lg:hidden flex items-center gap-3 mb-6">
                <div class="w-10 h-10 bg-gradient-to-br from-orange-500 to-pink-500 rounded-xl flex items-center justify-center">
                  <span class="text-white font-bold text-lg">B</span>
                </div>
                <span class="text-xl font-bold text-gray-900 dark:text-white">博客</span>
              </div>
              
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">创建账号</h2>
              <p class="text-gray-500 dark:text-gray-400 mb-8">填写以下信息完成注册</p>
              
              <form @submit.prevent="handleRegister" class="space-y-5">
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      用户名 <span class="text-red-500">*</span>
                    </label>
                    <input 
                      v-model="registerForm.username"
                      type="text" 
                      placeholder="3-20位字符"
                      class="w-full px-4 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                  </div>
                  
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      昵称
                    </label>
                    <input 
                      v-model="registerForm.nickname"
                      type="text" 
                      placeholder="您的昵称"
                      class="w-full px-4 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                  </div>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    邮箱
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </div>
                    <input 
                      v-model="registerForm.email"
                      type="email" 
                      placeholder="请输入邮箱（可选）"
                      class="w-full pl-12 pr-4 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                  </div>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    密码 <span class="text-red-500">*</span>
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                      </svg>
                    </div>
                    <input 
                      v-model="registerForm.password"
                      :type="showRegisterPassword ? 'text' : 'password'" 
                      placeholder="至少6位字符"
                      class="w-full pl-12 pr-12 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                    <button 
                      type="button"
                      @click="showRegisterPassword = !showRegisterPassword"
                      class="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                    >
                      <svg v-if="!showRegisterPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                      </svg>
                    </button>
                  </div>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    确认密码 <span class="text-red-500">*</span>
                  </label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                    </div>
                    <input 
                      v-model="registerForm.confirmPassword"
                      :type="showConfirmPassword ? 'text' : 'password'" 
                      placeholder="请再次输入密码"
                      class="w-full pl-12 pr-12 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                    <button 
                      type="button"
                      @click="showConfirmPassword = !showConfirmPassword"
                      class="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                    >
                      <svg v-if="!showConfirmPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                      </svg>
                    </button>
                  </div>
                </div>
                
                <div v-if="registerError" class="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-xl text-sm border border-red-100 dark:border-red-800">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  {{ registerError }}
                </div>
                
                <button 
                  type="submit"
                  :disabled="isRegistering"
                  class="w-full py-3.5 bg-gradient-to-r from-orange-500 to-pink-500 text-white rounded-xl font-semibold hover:from-orange-600 hover:to-pink-600 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-orange-500/25 hover:shadow-xl hover:shadow-orange-500/30"
                >
                  <span v-if="isRegistering" class="flex items-center justify-center gap-2">
                    <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    注册中...
                  </span>
                  <span v-else>立即注册</span>
                </button>
              </form>
              
              <div class="mt-6 text-center">
                <span class="text-gray-500 dark:text-gray-400">已有账号？</span>
                <button @click="switchToLogin" class="text-orange-500 hover:text-orange-600 font-medium ml-1 transition-colors">
                  立即登录
                </button>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>
