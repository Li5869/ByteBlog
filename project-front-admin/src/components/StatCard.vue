<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  icon: {
    type: String,
    default: 'document'
  },
  color: {
    type: String,
    default: 'blue',
    validator: (value) => ['blue', 'green', 'orange', 'purple'].includes(value)
  }
})

const colorClasses = {
  blue: {
    bg: 'bg-blue-50 dark:bg-blue-900/20',
    iconBg: 'bg-blue-500',
    text: 'text-blue-600 dark:text-blue-400',
    ring: 'hover:ring-blue-200 dark:hover:ring-blue-800'
  },
  green: {
    bg: 'bg-green-50 dark:bg-green-900/20',
    iconBg: 'bg-green-500',
    text: 'text-green-600 dark:text-green-400',
    ring: 'hover:ring-green-200 dark:hover:ring-green-800'
  },
  orange: {
    bg: 'bg-orange-50 dark:bg-orange-900/20',
    iconBg: 'bg-orange-500',
    text: 'text-orange-600 dark:text-orange-400',
    ring: 'hover:ring-orange-200 dark:hover:ring-orange-800'
  },
  purple: {
    bg: 'bg-purple-50 dark:bg-purple-900/20',
    iconBg: 'bg-purple-500',
    text: 'text-purple-600 dark:text-purple-400',
    ring: 'hover:ring-purple-200 dark:hover:ring-purple-800'
  }
}
</script>

<template>
  <div 
    class="bg-white dark:bg-gray-800 rounded-lg p-5 shadow-sm border border-gray-100 dark:border-gray-700 
           hover:shadow-md transition-all duration-300 ring-2 ring-transparent cursor-pointer"
    :class="colorClasses[color].ring"
  >
    <div class="flex items-center gap-4">
      <div 
        class="w-12 h-12 rounded-lg flex items-center justify-center text-white"
        :class="colorClasses[color].iconBg"
      >
        <svg v-if="icon === 'document'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <svg v-else-if="icon === 'users'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
        <svg v-else-if="icon === 'chat'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <svg v-else-if="icon === 'eye'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
        </svg>
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">{{ title }}</p>
        <p class="text-2xl font-bold text-gray-900 dark:text-white truncate">{{ value }}</p>
      </div>
    </div>
  </div>
</template>
