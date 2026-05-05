<script setup>
import {ref, watch} from 'vue'

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'save'])

const formData = ref({
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  gender: '',
  status: 'normal'
})

const statusOptions = [
  { label: '正常', value: 'normal' },
  { label: '封禁', value: 'banned' }
]

const genderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' },
  { label: '保密', value: '保密' }
]

watch(() => props.user, (newUser) => {
  if (newUser) {
    formData.value = {
      nickname: newUser.nickname || '',
      email: newUser.email || '',
      phone: newUser.phone || '',
      bio: newUser.bio || '',
      gender: newUser.gender || '保密',
      status: newUser.status || 'normal'
    }
  }
}, { immediate: true })

const closeModal = () => {
  emit('close')
}

const handleSave = () => {
  emit('save', { ...formData.value, id: props.user.id })
  closeModal()
}
</script>

<template>
  <div class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex min-h-screen items-center justify-center p-4">
      <div
        class="fixed inset-0 bg-black/50 transition-opacity"
        @click="closeModal"
      ></div>

      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg overflow-hidden">
        <div class="flex items-center justify-between p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">编辑用户</h2>
          <button
            @click="closeModal"
            class="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="p-4 sm:p-6 space-y-4">
          <div v-if="user" class="flex items-center gap-4 mb-6">
            <img
              :src="user.avatar"
              :alt="user.nickname"
              class="w-16 h-16 rounded-full object-cover"
            />
            <div>
              <div class="text-lg font-medium text-gray-900 dark:text-white">{{ user.username }}</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">ID: {{ user.id }}</div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">昵称</label>
            <input
              v-model="formData.nickname"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              placeholder="请输入昵称"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">邮箱</label>
            <input
              v-model="formData.email"
              type="email"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              placeholder="请输入邮箱"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">手机号</label>
            <input
              v-model="formData.phone"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
              placeholder="请输入手机号"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">性别</label>
            <select
              v-model="formData.gender"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
            >
              <option v-for="option in genderOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">个人简介</label>
            <textarea
              v-model="formData.bio"
              rows="3"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm resize-none"
              placeholder="请输入个人简介"
            ></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">状态</label>
            <select
              v-model="formData.status"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
            >
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 p-4 sm:p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
          <button
            @click="closeModal"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-600 border border-gray-300 dark:border-gray-500 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-500 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSave"
            class="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-lg hover:bg-primary-600 transition-colors"
          >
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
