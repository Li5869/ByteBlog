<script setup>
defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  tags: {
    type: Array,
    default: () => []
  },
  hotArticles: {
    type: Array,
    default: () => []
  }
})
</script>

<template>
  <div class="space-y-4">
    <div class="bg-white dark:bg-gray-800 rounded-lg overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
        <h3 class="font-bold text-gray-900 dark:text-white text-sm">个人成就</h3>
      </div>
      <div class="p-4">
        <div class="grid grid-cols-3 gap-4 text-center">
          <div>
            <div class="text-xl font-bold text-primary-500">128</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">原创</div>
          </div>
          <div>
            <div class="text-xl font-bold text-orange-500">12.5k</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">粉丝</div>
          </div>
          <div>
            <div class="text-xl font-bold text-green-500">8.9k</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">点赞</div>
          </div>
        </div>
        <div class="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
          <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
            <svg class="w-4 h-4 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
            </svg>
            <span>获得 <strong class="text-gray-900 dark:text-white">256</strong> 次评论</span>
          </div>
          <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mt-2">
            <svg class="w-4 h-4 text-blue-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.214.33-.403.713-.57 1.116-.334.804-.614 1.768-.84 2.734a31.365 31.365 0 00-.613 3.58 2.64 2.64 0 01-.945-1.067c-.328-.68-.398-1.534-.398-2.654A1 1 0 005.05 6.05 6.981 6.981 0 003 11a7 7 0 1011.95-4.95c-.592-.591-.98-.985-1.348-1.467-.363-.476-.724-1.063-1.207-2.03zM12.12 15.12A3 3 0 017 13s.879.5 2.5.5c0-1 .5-4 1.25-4.5.5 1 .786 1.293 1.371 1.879A2.99 2.99 0 0113 13a2.99 2.99 0 01-.879 2.121z" clip-rule="evenodd" />
            </svg>
            <span>获得 <strong class="text-gray-900 dark:text-white">89</strong> 次收藏</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-lg overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <h3 class="font-bold text-gray-900 dark:text-white text-sm">热门文章</h3>
      </div>
      <div class="divide-y divide-gray-100 dark:divide-gray-700">
        <router-link 
          v-for="(article, index) in hotArticles" 
          :key="article.id"
          :to="`/article/${article.id}`"
          class="flex items-start gap-3 p-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors group"
        >
          <span 
            class="flex-shrink-0 w-5 h-5 flex items-center justify-center rounded text-xs font-bold"
            :class="{
              'bg-red-500 text-white': index === 0,
              'bg-orange-500 text-white': index === 1,
              'bg-yellow-500 text-white': index === 2,
              'bg-gray-200 dark:bg-gray-600 text-gray-600 dark:text-gray-300': index > 2
            }"
          >
            {{ index + 1 }}
          </span>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-primary-500 line-clamp-2">
              {{ article.title }}
            </p>
            <p class="text-xs text-gray-400 mt-1">
              {{ article.views }} 阅读
            </p>
          </div>
        </router-link>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-lg overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <h3 class="font-bold text-gray-900 dark:text-white text-sm">热门标签</h3>
      </div>
      <div class="p-4">
        <div class="flex flex-wrap gap-2">
          <a 
            v-for="tag in tags" 
            :key="tag"
            href="#"
            class="px-2 py-1 text-xs text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 rounded hover:bg-primary-100 hover:text-primary-600 dark:hover:bg-primary-900/30 dark:hover:text-primary-400 transition-colors"
          >
            #{{ tag }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
