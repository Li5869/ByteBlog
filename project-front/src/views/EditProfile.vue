<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {isLoggedIn, uploadApi, userApi} from '@/utils/request'
import {useUserStore} from '@/stores/user'
import {toast} from '@/utils/toast'
import {DEFAULT_AVATAR} from '@/utils/defaults'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formData = ref({
  nickname: '',
  avatar: '',
  phone: '',
  gender: null,
  email: '',
  bio: ''
})

const avatarInput = ref(null)
const avatarPreview = ref('')

const genderOptions = [
  { label: '保密', value: 0, icon: 'M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  { label: '男', value: 1, icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
  { label: '女', value: 2, icon: 'M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z' }
]

const fetchProfile = async () => {
  if (!isLoggedIn()) {
    router.push('/')
    return
  }
  
  loading.value = true
  try {
    const data = await userApi.getProfileStats()
    formData.value = {
      nickname: data.nickname || '',
      avatar: data.avatar || '',
      phone: data.phone || '',
      gender: data.gender ?? null,
      email: data.email || '',
      bio: data.bio || ''
    }
    avatarPreview.value = data.avatar || ''
  } catch (error) {
    console.error('获取用户资料失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAvatarChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  if (file.size > 10 * 1024 * 1024) {
    toast.error('图片大小不能超过10MB')
    return
  }
  
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    toast.error('请上传 jpg、png、gif、bmp、webp 格式的图片')
    return
  }
  
  uploading.value = true
  try {
    const avatarUrl = await uploadApi.uploadFile(file)
    avatarPreview.value = avatarUrl
    formData.value.avatar = avatarUrl
  } catch (error) {
    console.error('上传头像失败:', error)
    toast.error(error.message || '上传头像失败，请重试')
  } finally {
    uploading.value = false
  }
  
  event.target.value = ''
}

const handleSubmit = async () => {
  if (saving.value) return
  
  saving.value = true
  try {
    await userApi.updateProfile(formData.value)
    userStore.updateUserInfo({
      nickname: formData.value.nickname,
      avatar: formData.value.avatar,
      bio: formData.value.bio
    })
    toast.success('资料更新成功')
    router.push('/mine')
  } catch (error) {
    console.error('更新资料失败:', error)
    toast.error(error.message || '更新失败，请重试')
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchProfile()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="bg-gradient-to-r from-primary-500 via-primary-600 to-primary-700 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 relative">
        <div class="flex items-center gap-4">
          <button 
            @click="goBack"
            class="p-2.5 bg-white/20 backdrop-blur-sm text-white rounded-xl hover:bg-white/30 transition-all border border-white/20"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <h1 class="text-xl sm:text-2xl font-bold text-white">编辑资料</h1>
            <p class="text-white/70 text-sm mt-0.5">完善你的个人信息</p>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 -mt-4">
      <div v-if="loading" class="bg-white dark:bg-gray-800 rounded-2xl shadow-lg border border-gray-100 dark:border-gray-700/50 p-12 flex justify-center">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-500"></div>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="space-y-6">
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <div class="p-6 sm:p-8">
            <div class="flex flex-col sm:flex-row items-center gap-6 sm:gap-8">
              <div class="flex flex-col items-center">
                <div class="relative group">
                  <img 
                    :src="avatarPreview || DEFAULT_AVATAR" 
                    alt="头像预览"
                    class="w-28 h-28 sm:w-32 sm:h-32 rounded-2xl object-cover border-4 border-white dark:border-gray-700 shadow-xl transition-all duration-300 group-hover:scale-105"
                    :class="{ 'opacity-50': uploading }"
                  />
                  <div 
                    v-if="uploading"
                    class="absolute inset-0 flex items-center justify-center bg-black/30 rounded-2xl"
                  >
                    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
                  </div>
                  <button
                    type="button"
                    @click="avatarInput?.click()"
                    :disabled="uploading"
                    class="absolute -bottom-2 -right-2 p-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl hover:from-primary-600 hover:to-primary-700 transition-all shadow-lg shadow-primary-500/30 hover:scale-110 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </button>
                  <input 
                    ref="avatarInput"
                    type="file" 
                    accept="image/*"
                    class="hidden"
                    @change="handleAvatarChange"
                  />
                </div>
                
                <button
                  type="button"
                  @click="avatarInput?.click()"
                  :disabled="uploading"
                  class="mt-4 text-sm text-primary-500 hover:text-primary-600 transition-colors font-medium disabled:opacity-50"
                >
                  {{ uploading ? '上传中...' : '更换头像' }}
                </button>
                <p class="mt-1 text-xs text-gray-400 dark:text-gray-500 text-center">支持 jpg、png、gif、webp，不超过 10MB</p>
              </div>

              <div class="flex-1 w-full">
                <div class="space-y-5">
                  <div>
                    <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                      昵称 <span class="text-red-500">*</span>
                    </label>
                    <input 
                      v-model="formData.nickname"
                      type="text"
                      maxlength="50"
                      placeholder="请输入昵称"
                      class="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white dark:focus:bg-gray-700 transition-all"
                    />
                    <p class="mt-1.5 text-xs text-gray-400 dark:text-gray-500">最多 50 个字符</p>
                  </div>

                  <div>
                    <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                      性别
                    </label>
                    <div class="flex gap-3">
                      <button
                        type="button"
                        v-for="option in genderOptions" 
                        :key="option.value"
                        @click="formData.gender = option.value"
                        class="flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-xl border-2 transition-all duration-200"
                        :class="formData.gender === option.value 
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400' 
                          : 'border-gray-200 dark:border-gray-600 hover:border-primary-300 dark:hover:border-primary-700 text-gray-600 dark:text-gray-400'"
                      >
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="option.icon" />
                        </svg>
                        <span class="font-medium">{{ option.label }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
              </svg>
            </div>
            <div>
              <h3 class="font-bold text-gray-900 dark:text-white">联系方式</h3>
              <p class="text-xs text-gray-400">用于接收通知和找回密码</p>
            </div>
          </div>
          
          <div class="p-6 sm:p-8">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                  手机号码
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                    </svg>
                  </div>
                  <input 
                    v-model="formData.phone"
                    type="tel"
                    maxlength="11"
                    placeholder="请输入手机号码"
                    class="w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white dark:focus:bg-gray-700 transition-all"
                  />
                </div>
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                  邮箱
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <input 
                    v-model="formData.email"
                    type="email"
                    maxlength="100"
                    placeholder="请输入邮箱地址"
                    class="w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white dark:focus:bg-gray-700 transition-all"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 flex items-center justify-center shadow-lg shadow-violet-500/20">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </div>
            <div>
              <h3 class="font-bold text-gray-900 dark:text-white">个人简介</h3>
              <p class="text-xs text-gray-400">让更多人了解你</p>
            </div>
          </div>
          
          <div class="p-6 sm:p-8">
            <textarea 
              v-model="formData.bio"
              rows="5"
              maxlength="500"
              placeholder="介绍一下自己吧，让更多人了解你..."
              class="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700/50 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white dark:focus:bg-gray-700 transition-all resize-none"
            />
            <div class="mt-2 flex justify-between text-xs text-gray-400 dark:text-gray-500">
              <span>用一段话介绍自己</span>
              <span :class="{ 'text-orange-500': (formData.bio?.length || 0) > 450 }">{{ formData.bio?.length || 0 }}/500</span>
            </div>
          </div>
        </div>

        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <button
            type="button"
            @click="goBack"
            class="px-6 py-3 border border-gray-200 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors text-sm font-medium order-2 sm:order-1"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="saving || uploading"
            class="px-8 py-3 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl hover:from-primary-600 hover:to-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all text-sm font-semibold shadow-lg shadow-primary-500/30 hover:shadow-xl hover:shadow-primary-500/40 order-1 sm:order-2 flex items-center justify-center gap-2"
          >
            <svg v-if="saving" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
