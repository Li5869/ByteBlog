import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {adminAuthApi, clearAuth, getAdminInfo, saveAuth} from '@/utils/request.js'

export const useAdminStore = defineStore('admin', () => {
  const adminInfo = ref(getAdminInfo())
  const loading = ref(false)

  const isLoggedIn = computed(() => !!adminInfo.value)

  const login = async (loginData) => {
    loading.value = true
    try {
      const data = await adminAuthApi.login(loginData)
      saveAuth(data.token, data, data.refreshToken)
      adminInfo.value = data
      return data
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    try {
      await adminAuthApi.logout()
    } finally {
      clearAuth()
      adminInfo.value = null
      window.location.href = '/login'
    }
  }

  const fetchAdminInfo = async () => {
    const data = await adminAuthApi.getUserInfo()
    adminInfo.value = { ...adminInfo.value, ...data }
    localStorage.setItem('admin-info', JSON.stringify(adminInfo.value))
    return data
  }

  return {
    adminInfo,
    loading,
    isLoggedIn,
    login,
    logout,
    fetchAdminInfo,
  }
})
