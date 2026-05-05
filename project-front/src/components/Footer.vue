<script setup>
const currentYear = new Date().getFullYear()

const links = [
  { name: '关于我们', url: '/about' },
  { name: '联系我们', url: '#' },
  { name: '隐私政策', url: '#' },
  { name: '服务条款', url: '#' }
]
</script>

<template>
  <footer class="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="flex flex-col md:flex-row items-center justify-between gap-4">
        <div class="flex items-center space-x-4 text-sm text-gray-500 dark:text-gray-400">
          <a 
            v-for="link in links" 
            :key="link.name"
            :href="link.url"
            class="hover:text-primary-500 transition-colors"
          >
            {{ link.name }}
          </a>
        </div>
        
        <div class="text-sm text-gray-500 dark:text-gray-400">
          © {{ currentYear }} 个人博客 All Rights Reserved
        </div>
      </div>
    </div>
  </footer>
</template>
