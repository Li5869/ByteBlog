<script setup>
import {RouterLink, useRoute} from 'vue-router'

const props = defineProps({
  isCollapsed: {
    type: Boolean,
    default: false
  },
  sidebarWidth: {
    type: String,
    default: 'w-64'
  }
})

const emit = defineEmits(['toggle'])

const route = useRoute()

const menuItems = [
  {
    name: '首页',
    path: '/dashboard',
    icon: 'dashboard'
  },
  {
    name: '文章管理',
    path: '/articles',
    icon: 'article'
  },
  {
    name: '用户管理',
    path: '/users',
    icon: 'user'
  },
  {
    name: '评论管理',
    path: '/comments',
    icon: 'comment'
  },
  {
    name: '分类管理',
    path: '/categories',
    icon: 'category'
  },
  {
    name: '标签管理',
    path: '/tags',
    icon: 'tag'
  },
  {
    name: '专栏管理',
    path: '/columns',
    icon: 'column'
  },
  {
    name: '问答管理',
    path: '/questions',
    icon: 'question'
  },
  {
    name: '通知管理',
    path: '/notifications',
    icon: 'notification'
  },
  {
    name: '操作日志',
    path: '/logs',
    icon: 'log'
  },
  {
    name: '知识库管理',
    path: '/knowledge',
    icon: 'knowledge'
  },
  {
    name: '技能管理',
    path: '/skill',
    icon: 'skill'
  }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const iconPaths = {
  dashboard: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
  article: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
  user: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
  comment: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
  category: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
  tag: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z',
  column: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
  question: 'M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0a9 9 0 0118 0z',
  notification: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9',
  settings: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
  log: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01',
  knowledge: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253',
  skill: 'M13 10V3L4 14h7v7l9-11h-7z'
}
</script>

<template>
  <aside 
    :class="[sidebarWidth]"
    class="fixed left-0 top-0 h-full bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 shadow-sm z-30 transition-all duration-300 flex flex-col"
  >
    <div class="h-16 flex items-center justify-between px-4 border-b border-gray-200 dark:border-gray-700">
      <RouterLink to="/dashboard" class="flex items-center gap-2">
        <div class="w-8 h-8 bg-gradient-to-br from-red-500 to-orange-500 rounded-lg flex items-center justify-center shrink-0">
          <span class="text-white font-bold text-sm">A</span>
        </div>
        <span 
          v-if="!isCollapsed" 
          class="text-lg font-bold text-gray-900 dark:text-white whitespace-nowrap"
        >
          博客管理
        </span>
      </RouterLink>
    </div>

    <nav class="flex-1 overflow-y-auto py-4">
      <ul class="space-y-1 px-3">
        <li v-for="item in menuItems" :key="item.path">
          <RouterLink
            :to="item.path"
            :class="[
              'flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 group',
              isActive(item.path)
                ? 'bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white'
            ]"
            :title="isCollapsed ? item.name : ''"
          >
            <svg 
              class="w-5 h-5 shrink-0" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                stroke-linecap="round" 
                stroke-linejoin="round" 
                stroke-width="2" 
                :d="iconPaths[item.icon]" 
              />
            </svg>
            <span 
              v-if="!isCollapsed" 
              class="text-sm font-medium whitespace-nowrap"
            >
              {{ item.name }}
            </span>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="p-3 border-t border-gray-200 dark:border-gray-700">
      <button
        @click="emit('toggle')"
        class="w-full flex items-center justify-center gap-2 px-3 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
      >
        <svg 
          class="w-5 h-5 transition-transform duration-300" 
          :class="{ 'rotate-180': isCollapsed }"
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
        </svg>
        <span v-if="!isCollapsed" class="text-sm">收起菜单</span>
      </button>
    </div>
  </aside>
</template>
