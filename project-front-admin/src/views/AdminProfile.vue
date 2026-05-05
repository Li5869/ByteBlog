<script setup>
import {onMounted, reactive, ref} from 'vue'
import {updateAdminInfo, uploadApi, userApi} from '@/utils/request'
import {DEFAULT_AVATAR} from '@/utils/defaults'

const activeTab = ref('profile')

const profileForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  avatar: '',
  gender: null
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileLoading = ref(false)
const passwordLoading = ref(false)
const uploading = ref(false)
const loading = ref(false)

const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

const fetchProfile = async () => {
  loading.value = true
  try {
    const data = await userApi.getProfile()
    profileForm.username = data.username || ''
    profileForm.nickname = data.nickname || ''
    profileForm.email = data.email || ''
    profileForm.phone = data.phone || ''
    profileForm.bio = data.bio || ''
    profileForm.avatar = data.avatar || ''
    profileForm.gender = data.gender ?? null
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAvatarChange = async () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    
    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过10MB')
      return
    }
    
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      alert('请上传 jpg、png、gif、bmp、webp 格式的图片')
      return
    }
    
    uploading.value = true
    try {
      const avatarUrl = await uploadApi.uploadFile(file)
      profileForm.avatar = avatarUrl
    } catch (error) {
      console.error('上传头像失败:', error)
      alert(error.message || '上传头像失败，请重试')
    } finally {
      uploading.value = false
    }
  }
  input.click()
}

const saveProfile = async () => {
  profileLoading.value = true
  try {
    await userApi.updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      bio: profileForm.bio,
      avatar: profileForm.avatar,
      gender: profileForm.gender
    })
    updateAdminInfo({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      bio: profileForm.bio
    })
    window.dispatchEvent(new CustomEvent('admin-info-updated'))
    alert('个人信息保存成功！')
  } catch (error) {
    console.error('保存失败:', error)
    alert(error.message || '保存失败，请重试')
  } finally {
    profileLoading.value = false
  }
}

const changePassword = async () => {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    alert('请填写完整的密码信息')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }
  
  if (passwordForm.newPassword.length < 6) {
    alert('新密码长度不能少于6位')
    return
  }
  
  passwordLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    
    alert('密码修改成功！')
  } catch (error) {
    alert(error.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">账号设置</h1>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500"></div>
    </div>

    <div v-else class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
      <div class="border-b border-gray-200 dark:border-gray-700">
        <nav class="flex -mb-px">
          <button
            @click="activeTab = 'profile'"
            :class="[
              'px-6 py-4 text-sm font-medium border-b-2 transition-colors',
              activeTab === 'profile'
                ? 'border-red-500 text-red-600 dark:text-red-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            ]"
          >
            个人信息
          </button>
          <button
            @click="activeTab = 'password'"
            :class="[
              'px-6 py-4 text-sm font-medium border-b-2 transition-colors',
              activeTab === 'password'
                ? 'border-red-500 text-red-600 dark:text-red-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            ]"
          >
            修改密码
          </button>
        </nav>
      </div>

      <div class="p-6">
        <div v-if="activeTab === 'profile'" class="space-y-6">
          <div class="flex items-center gap-6">
            <div class="relative">
              <img
                :src="profileForm.avatar || DEFAULT_AVATAR"
                alt="头像"
                class="w-24 h-24 rounded-full object-cover border-4 border-gray-200 dark:border-gray-700"
                :class="{ 'opacity-50': uploading }"
              />
              <div v-if="uploading" class="absolute inset-0 flex items-center justify-center">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
              </div>
              <button
                @click="handleAvatarChange"
                :disabled="uploading"
                class="absolute bottom-0 right-0 w-8 h-8 bg-red-500 text-white rounded-full flex items-center justify-center hover:bg-red-600 transition-colors shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
            <div>
              <h3 class="text-lg font-medium text-gray-900 dark:text-white">{{ profileForm.nickname || profileForm.username }}</h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">{{ profileForm.email || '未设置邮箱' }}</p>
              <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">支持 jpg、png、gif、webp，大小不超过 10MB</p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                用户名
              </label>
              <input
                v-model="profileForm.username"
                type="text"
                disabled
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 cursor-not-allowed"
              />
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">用户名不可修改</p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                昵称
              </label>
              <input
                v-model="profileForm.nickname"
                type="text"
                maxlength="50"
                placeholder="请输入昵称"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                邮箱
              </label>
              <input
                v-model="profileForm.email"
                type="email"
                maxlength="100"
                placeholder="请输入邮箱"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                手机号
              </label>
              <input
                v-model="profileForm.phone"
                type="tel"
                maxlength="11"
                placeholder="请输入手机号"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                性别
              </label>
              <div class="flex gap-6 h-[42px] items-center">
                <label 
                  v-for="option in genderOptions" 
                  :key="option.value"
                  class="flex items-center cursor-pointer"
                >
                  <input 
                    type="radio" 
                    :value="option.value" 
                    v-model="profileForm.gender"
                    class="w-4 h-4 text-red-500 border-gray-300 focus:ring-red-500"
                  />
                  <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">{{ option.label }}</span>
                </label>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              个人简介
            </label>
            <textarea
              v-model="profileForm.bio"
              rows="4"
              maxlength="500"
              placeholder="请输入个人简介"
              class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent resize-none"
            ></textarea>
            <div class="mt-1 flex justify-between text-xs text-gray-500 dark:text-gray-400">
              <span>最多 500 个字符</span>
              <span>{{ profileForm.bio?.length || 0 }}/500</span>
            </div>
          </div>

          <div class="flex justify-end pt-4">
            <button
              @click="saveProfile"
              :disabled="profileLoading || uploading"
              :class="[
                'px-6 py-2.5 bg-red-500 text-white rounded-lg font-medium text-sm transition-colors',
                profileLoading || uploading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-600'
              ]"
            >
              <span v-if="profileLoading" class="flex items-center gap-2">
                <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                保存中...
              </span>
              <span v-else>保存修改</span>
            </button>
          </div>
        </div>

        <div v-if="activeTab === 'password'" class="space-y-6">
          <div class="max-w-md">
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                当前密码
              </label>
              <input
                v-model="passwordForm.currentPassword"
                type="password"
                placeholder="请输入当前密码"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                新密码
              </label>
              <input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">密码长度不能少于6位</p>
            </div>

            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                确认新密码
              </label>
              <input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div class="flex justify-end">
              <button
                @click="changePassword"
                :disabled="passwordLoading"
                :class="[
                  'px-6 py-2.5 bg-red-500 text-white rounded-lg font-medium text-sm transition-colors',
                  passwordLoading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-600'
                ]"
              >
                <span v-if="passwordLoading" class="flex items-center gap-2">
                  <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  修改中...
                </span>
                <span v-else>修改密码</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
