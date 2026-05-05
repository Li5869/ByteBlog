<script setup>
import {onMounted, ref} from 'vue'
import UserDetail from '../components/UserDetail.vue'
import UserEditModal from '../components/UserEditModal.vue'
import {adminUserApi} from '../utils/request'

const searchQuery = ref('')
const statusFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const showDetailModal = ref(false)
const showEditModal = ref(false)
const selectedUser = ref(null)
const users = ref([])

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '正常', value: 'normal' },
  { label: '封禁', value: 'banned' }
]

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value
    }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    const res = await adminUserApi.getPage(params)
    users.value = res.records || []
    total.value = res.total || 0
    totalPages.value = res.pages || 0
  } catch (e) {
    console.error('获取用户列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 搜索防抖
let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchUsers()
  }, 300)
}

const onStatusChange = (status) => {
  statusFilter.value = status
  currentPage.value = 1
  fetchUsers()
}

const getStatusText = (status) => {
  const map = { normal: '正常', banned: '封禁' }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    normal: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    banned: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400'
  }
  return map[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const viewDetail = async (user) => {
  try {
    const detail = await adminUserApi.getDetail(user.id)
    selectedUser.value = detail
    showDetailModal.value = true
  } catch (e) {
    console.error('获取用户详情失败:', e)
  }
}

const editUser = (user) => {
  selectedUser.value = { ...user }
  showEditModal.value = true
}

const saveUser = async (userData) => {
  try {
    await adminUserApi.update(userData.id, userData)
    showEditModal.value = false
    selectedUser.value = null
    fetchUsers()
  } catch (e) {
    console.error('更新用户失败:', e)
    alert(e.message || '更新失败')
  }
}

const toggleStatus = async (user) => {
  const newStatus = user.status === 'banned' ? 'normal' : 'banned'
  const actionText = newStatus === 'banned' ? '封禁' : '解封'
  if (confirm(`确定要${actionText}该用户吗？`)) {
    try {
      await adminUserApi.updateStatus(user.id, { status: newStatus === 'banned' ? 0 : 1 })
      fetchUsers()
    } catch (e) {
      console.error('更新用户状态失败:', e)
      alert(e.message || '操作失败')
    }
  }
}

const deleteUser = async (user) => {
  if (confirm(`确定要删除用户 ${user.nickname} 吗？`)) {
    try {
      await adminUserApi.delete(user.id)
      fetchUsers()
    } catch (e) {
      console.error('删除用户失败:', e)
      alert(e.message || '删除失败')
    }
  }
}

const handlePageChange = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchUsers()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchUsers()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchUsers()
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedUser.value = null
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedUser.value = null
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-7xl mx-auto">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">用户管理</h1>
          </div>
        </div>

        <div class="p-4 sm:p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row gap-4">
            <div class="flex-1">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  @input="onSearch"
                  type="text"
                  placeholder="搜索用户名、昵称或邮箱..."
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
                />
              </div>
            </div>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                @click="onStatusChange(option.value)"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="statusFilter === option.value
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          <p class="mt-2 text-gray-500 dark:text-gray-400 text-sm">加载中...</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">ID</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">头像</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">用户名</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">昵称</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">邮箱</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">文章数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden lg:table-cell">粉丝数</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden xl:table-cell">注册时间</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
              <tr
                v-for="user in users"
                :key="user.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">{{ user.id }}</td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <img
                    :src="user.avatar"
                    :alt="user.nickname"
                    class="w-10 h-10 rounded-full object-cover"
                  />
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">{{ user.username }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden sm:table-cell">{{ user.nickname }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden md:table-cell">{{ user.email }}</td>
                <td class="px-4 py-4 whitespace-nowrap">
                  <span
                    class="inline-flex px-2 py-1 text-xs font-medium rounded-full"
                    :class="getStatusClass(user.status)"
                  >
                    {{ getStatusText(user.status) }}
                  </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ user.articleCount }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ user.fansCount }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 hidden xl:table-cell">{{ formatDate(user.createdAt) }}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm">
                  <div class="flex items-center gap-2">
                    <button
                      @click="viewDetail(user)"
                      class="text-primary-500 hover:text-primary-600 transition-colors"
                      title="查看详情"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button
                      @click="editUser(user)"
                      class="text-blue-500 hover:text-blue-600 transition-colors"
                      title="编辑"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      @click="toggleStatus(user)"
                      class="transition-colors"
                      :class="user.status === 'normal' ? 'text-red-500 hover:text-red-600' : 'text-green-500 hover:text-green-600'"
                      :title="user.status === 'normal' ? '封禁' : '解封'"
                    >
                      <svg v-if="user.status === 'normal'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                      <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </button>
                    <button
                      @click="deleteUser(user)"
                      class="text-red-500 hover:text-red-600 transition-colors"
                      title="删除"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!loading && users.length === 0" class="p-8 text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
          </svg>
          <p class="mt-4 text-gray-500 dark:text-gray-400">暂无用户数据</p>
        </div>

        <div v-if="total > 0" class="px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="text-sm text-gray-600 dark:text-gray-400">
              共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <button
                @click="prevPage"
                :disabled="currentPage === 1"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                上一页
              </button>
              <div class="flex items-center gap-1">
                <button
                  v-for="page in Math.min(totalPages, 7)"
                  :key="page"
                  @click="handlePageChange(page)"
                  class="w-8 h-8 text-sm rounded-lg transition-colors"
                  :class="currentPage === page
                    ? 'bg-primary-500 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
                >
                  {{ page }}
                </button>
              </div>
              <button
                @click="nextPage"
                :disabled="currentPage === totalPages"
                class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <UserDetail
      v-if="showDetailModal"
      :user="selectedUser"
      @close="closeDetailModal"
      @edit="editUser"
      @toggle-status="toggleStatus"
    />

    <UserEditModal
      v-if="showEditModal"
      :user="selectedUser"
      @close="closeEditModal"
      @save="saveUser"
    />
  </div>
</template>
