<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getAdminInfo} from '@/utils/request'
import {DEFAULT_AVATAR} from '@/utils/defaults'

const route = useRoute()
const router = useRouter()

const isDark = ref(false)
const showUserMenu = ref(false)
const userInfo = ref({
  name: '管理员',
  avatar: DEFAULT_AVATAR,
  role: '超级管理员'
})

const loadUserInfo = () => {
  const adminInfo = getAdminInfo()
  if (adminInfo) {
    userInfo.value = {
      name: adminInfo.nickname || adminInfo.username || '管理员',
      avatar: adminInfo.avatar || DEFAULT_AVATAR,
      role: adminInfo.isAdmin ? '超级管理员' : '管理员'
    }
  }
}

const breadcrumbs = computed(() => {
  const pathMap = {
    'dashboard': '首页',
    'articles': '文章管理',
    'users': '用户管理',
    'comments': '评论管理',
    'categories': '分类管理',
    'tags': '标签管理',
    'questions': '问答管理',
    'notifications': '通知管理',
    'profile': '账号设置',
    'edit': '编辑'
  }
  
  const paths = route.path.split('/').filter(Boolean)
  const crumbs = [{ name: '首页', path: '/dashboard' }]
  
  paths.forEach((path, index) => {
    if (path !== 'dashboard') {
      const name = pathMap[path] || path
      const fullPath = '/' + paths.slice(0, index + 1).join('/')
      crumbs.push({ name, path: fullPath })
    }
  })
  
  return crumbs
})

const toggleDark = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('admin-theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('admin-theme', 'light')
  }
}

const handleLogout = () => {
  showUserMenu.value = false
  localStorage.removeItem('admin-token')
  router.push('/login')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('admin-theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
  loadUserInfo()
  
  window.addEventListener('storage', (e) => {
    if (e.key === 'admin-info') {
      loadUserInfo()
    }
  })
  
  window.addEventListener('admin-info-updated', loadUserInfo)
})
</script>

<template>
  <header class="h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-20">
    <div class="h-full px-6 flex items-center justify-between">
      <nav class="flex items-center gap-2 text-sm">
        <template v-for="(crumb, index) in breadcrumbs" :key="crumb.path">
          <RouterLink
            v-if="index < breadcrumbs.length - 1"
            :to="crumb.path"
            class="text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
          >
            {{ crumb.name }}
          </RouterLink>
          <span v-else class="text-gray-900 dark:text-white font-medium">
            {{ crumb.name }}
          </span>
          <svg 
            v-if="index < breadcrumbs.length - 1" 
            class="w-4 h-4 text-gray-400" 
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </template>
      </nav>

      <div class="flex items-center gap-4">
        <button
          @click="toggleDark"
          class="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        >
          <svg v-if="isDark" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        </button>

        <div class="relative">
          <button
            @click="showUserMenu = !showUserMenu"
            class="flex items-center gap-3 p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <img
              :src="userInfo.avatar"
              alt="用户头像"
              class="w-8 h-8 rounded-full"
            />
            <div class="text-left hidden sm:block">
              <p class="text-sm font-medium text-gray-900 dark:text-white">{{ userInfo.name }}</p>
              <p class="text-xs text-gray-500 dark:text-gray-400">{{ userInfo.role }}</p>
            </div>
            <svg class="w-4 h-4 text-gray-400 hidden sm:block" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 scale-95"
            enter-to-class="opacity-100 scale-100"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 scale-100"
            leave-to-class="opacity-0 scale-95"
          >
            <div
              v-if="showUserMenu"
              class="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50"
            >
              <RouterLink
                to="/profile"
                class="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                @click="showUserMenu = false"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                账号设置
              </RouterLink>
              <div class="border-t border-gray-200 dark:border-gray-700 my-1"></div>
              <button
                @click="handleLogout"
                class="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                退出登录
              </button>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </header>
</template>
