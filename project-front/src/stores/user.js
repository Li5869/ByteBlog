/**
 * 用户状态管理
 * 
 * 使用 Vue 3 的 reactive 实现简单的状态管理
 * 提供用户登录、登出、获取用户信息等功能
 */
import {reactive} from 'vue'
import {authApi, clearAuth, getUserInfo, isLoggedIn as checkIsLoggedIn, saveAuth} from '../utils/request.js'
import wsManager from '../utils/websocket.js'
import sseManager from '../utils/sse.js'

const state = reactive({
  isLoggedIn: checkIsLoggedIn(),
  userInfo: getUserInfo(),
  loading: false,
})

const login = async (username, password) => {
  state.loading = true
  try {
    const data = await authApi.login({ username, password })
    // 保存 Access Token、Refresh Token 和用户信息
    saveAuth(data.token, data, data.refreshToken)
    state.isLoggedIn = true
    state.userInfo = data
    // 登录成功后建立 WebSocket 连接
    wsManager.connect()
    // 登录成功后建立 SSE 连接
    sseManager.connect()
    return data
  } finally {
    state.loading = false
  }
}

const register = async (formData) => {
  state.loading = true
  try {
    await authApi.register(formData)
    return true
  } finally {
    state.loading = false
  }
}

const logout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    console.error('登出请求失败:', e)
  } finally {
    clearAuth()
    state.isLoggedIn = false
    state.userInfo = null
    // 登出后断开 WebSocket 连接
    wsManager.disconnect()
    // 登出后断开 SSE 连接
    sseManager.disconnect()
  }
}

const fetchUserInfo = async () => {
  if (!checkIsLoggedIn()) {
    return null
  }
  try {
    const data = await authApi.getUserInfo()
    state.userInfo = { ...state.userInfo, ...data }
    localStorage.setItem('userInfo', JSON.stringify(state.userInfo))
    return data
  } catch (e) {
    console.error('获取用户信息失败:', e)
    return null
  }
}

const checkAuth = () => {
  state.isLoggedIn = checkIsLoggedIn()
  state.userInfo = getUserInfo()
  return state.isLoggedIn
}

const updateUserInfo = (newInfo) => {
  state.userInfo = { ...state.userInfo, ...newInfo }
  localStorage.setItem('userInfo', JSON.stringify(state.userInfo))
}

export const useUserStore = () => {
  return {
    state,
    login,
    register,
    logout,
    fetchUserInfo,
    checkAuth,
    updateUserInfo,
  }
}

export default {
  state,
  login,
  register,
  logout,
  fetchUserInfo,
  checkAuth,
}
