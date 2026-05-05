<script setup>
import {computed, ref} from 'vue'

const activeTab = ref('broadcast')

const broadcastForm = ref({
  title: '',
  content: '',
  type: 'system'
})

const userNotificationForm = ref({
  userId: '',
  userName: '',
  title: '',
  content: '',
  type: 'private'
})

const searchQuery = ref('')

const mockUsers = ref([
  { id: 1, name: '张三', email: 'zhangsan@example.com', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop' },
  { id: 2, name: '李四', email: 'lisi@example.com', avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop' },
  { id: 3, name: '王五', email: 'wangwu@example.com', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop' },
  { id: 4, name: '赵六', email: 'zhaoliu@example.com', avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop' },
  { id: 5, name: '孙七', email: 'sunqi@example.com', avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop' }
])

const notificationHistory = ref([
  { id: 1, title: '系统维护通知', content: '系统将于今晚22:00-次日02:00进行维护，届时服务将暂停访问。', type: 'system', target: '全体用户', createdAt: '2024-03-26 10:30:00', status: 'sent' },
  { id: 2, title: '新功能上线', content: '博客系统新增了问答模块，欢迎大家体验！', type: 'feature', target: '全体用户', createdAt: '2024-03-25 15:00:00', status: 'sent' },
  { id: 3, title: '违规提醒', content: '您发布的文章存在违规内容，已被下架处理。', type: 'warning', target: '张三', createdAt: '2024-03-24 09:20:00', status: 'sent' },
  { id: 4, title: '账号安全提醒', content: '检测到您的账号在新设备登录，请确认是否为本人操作。', type: 'security', target: '李四', createdAt: '2024-03-23 18:45:00', status: 'sent' }
])

const filteredUsers = computed(() => {
  if (!searchQuery.value) return mockUsers.value
  const query = searchQuery.value.toLowerCase()
  return mockUsers.value.filter(user => 
    user.name.toLowerCase().includes(query) || 
    user.email.toLowerCase().includes(query)
  )
})

const notificationTypes = [
  { value: 'system', label: '系统通知', color: 'blue' },
  { value: 'feature', label: '功能更新', color: 'green' },
  { value: 'warning', label: '警告提醒', color: 'yellow' },
  { value: 'security', label: '安全提醒', color: 'red' }
]

const showUserModal = ref(false)
const selectedUser = ref(null)

const openUserModal = () => {
  showUserModal.value = true
  searchQuery.value = ''
}

const selectUser = (user) => {
  selectedUser.value = user
  userNotificationForm.value.userId = user.id
  userNotificationForm.value.userName = user.name
  showUserModal.value = false
}

const sendBroadcast = () => {
  if (!broadcastForm.value.title || !broadcastForm.value.content) {
    alert('请填写通知标题和内容')
    return
  }
  
  notificationHistory.value.unshift({
    id: Date.now(),
    title: broadcastForm.value.title,
    content: broadcastForm.value.content,
    type: broadcastForm.value.type,
    target: '全体用户',
    createdAt: new Date().toLocaleString('zh-CN'),
    status: 'sent'
  })
  
  broadcastForm.value = { title: '', content: '', type: 'system' }
  alert('系统通知发送成功！')
}

const sendUserNotification = () => {
  if (!userNotificationForm.value.userId || !userNotificationForm.value.title || !userNotificationForm.value.content) {
    alert('请选择用户并填写通知标题和内容')
    return
  }
  
  notificationHistory.value.unshift({
    id: Date.now(),
    title: userNotificationForm.value.title,
    content: userNotificationForm.value.content,
    type: userNotificationForm.value.type,
    target: userNotificationForm.value.userName,
    createdAt: new Date().toLocaleString('zh-CN'),
    status: 'sent'
  })
  
  userNotificationForm.value = { userId: '', userName: '', title: '', content: '', type: 'private' }
  selectedUser.value = null
  alert('通知发送成功！')
}

const getTypeLabel = (type) => {
  const found = notificationTypes.find(t => t.value === type)
  return found ? found.label : type
}

const getTypeClass = (type) => {
  const classes = {
    system: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
    feature: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
    warning: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
    security: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    private: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400'
  }
  return classes[type] || ''
}
</script>

<template>
  <div class="min-h-screen bg-gray-100 dark:bg-gray-900 p-6">
    <div class="max-w-7xl mx-auto">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">通知管理</h1>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">管理系统通知和用户消息</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="border-b border-gray-200 dark:border-gray-700">
          <nav class="flex -mb-px">
            <button
              @click="activeTab = 'broadcast'"
              :class="[
                'px-6 py-4 text-sm font-medium border-b-2 transition-colors',
                activeTab === 'broadcast'
                  ? 'border-red-500 text-red-600 dark:text-red-400'
                  : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              ]"
            >
              发布系统通知
            </button>
            <button
              @click="activeTab = 'user'"
              :class="[
                'px-6 py-4 text-sm font-medium border-b-2 transition-colors',
                activeTab === 'user'
                  ? 'border-red-500 text-red-600 dark:text-red-400'
                  : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              ]"
            >
              发送用户通知
            </button>
            <button
              @click="activeTab = 'history'"
              :class="[
                'px-6 py-4 text-sm font-medium border-b-2 transition-colors',
                activeTab === 'history'
                  ? 'border-red-500 text-red-600 dark:text-red-400'
                  : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              ]"
            >
              发送记录
            </button>
          </nav>
        </div>

        <div class="p-6">
          <div v-if="activeTab === 'broadcast'" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知类型
              </label>
              <select
                v-model="broadcastForm.type"
                class="w-full max-w-xs px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option v-for="type in notificationTypes" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知标题 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="broadcastForm.title"
                type="text"
                placeholder="请输入通知标题"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知内容 <span class="text-red-500">*</span>
              </label>
              <textarea
                v-model="broadcastForm.content"
                rows="5"
                placeholder="请输入通知内容"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent resize-none"
              ></textarea>
            </div>

            <div class="flex items-center gap-4">
              <button
                @click="sendBroadcast"
                class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors font-medium"
              >
                发送通知
              </button>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                通知将发送给所有用户
              </p>
            </div>
          </div>

          <div v-if="activeTab === 'user'" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                选择用户 <span class="text-red-500">*</span>
              </label>
              <div class="flex items-center gap-4">
                <div v-if="selectedUser" class="flex items-center gap-3 px-4 py-2 bg-gray-100 dark:bg-gray-700 rounded-lg">
                  <img :src="selectedUser.avatar" class="w-8 h-8 rounded-full" />
                  <div>
                    <p class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedUser.name }}</p>
                    <p class="text-xs text-gray-500 dark:text-gray-400">{{ selectedUser.email }}</p>
                  </div>
                </div>
                <button
                  @click="openUserModal"
                  class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  {{ selectedUser ? '更换用户' : '选择用户' }}
                </button>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知类型
              </label>
              <select
                v-model="userNotificationForm.type"
                class="w-full max-w-xs px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option value="private">私信通知</option>
                <option value="warning">警告提醒</option>
                <option value="security">安全提醒</option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知标题 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="userNotificationForm.title"
                type="text"
                placeholder="请输入通知标题"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                通知内容 <span class="text-red-500">*</span>
              </label>
              <textarea
                v-model="userNotificationForm.content"
                rows="5"
                placeholder="请输入通知内容"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent resize-none"
              ></textarea>
            </div>

            <div class="flex items-center gap-4">
              <button
                @click="sendUserNotification"
                class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors font-medium"
              >
                发送通知
              </button>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                通知将发送给指定用户
              </p>
            </div>
          </div>

          <div v-if="activeTab === 'history'">
            <div class="overflow-x-auto">
              <table class="w-full">
                <thead class="bg-gray-50 dark:bg-gray-700/50">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">标题</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">类型</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">接收者</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">发送时间</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">状态</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
                  <tr v-for="notification in notificationHistory" :key="notification.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td class="px-4 py-4">
                      <div class="text-sm font-medium text-gray-900 dark:text-white">{{ notification.title }}</div>
                      <div class="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-1">{{ notification.content }}</div>
                    </td>
                    <td class="px-4 py-4">
                      <span :class="['px-2 py-1 text-xs rounded-full', getTypeClass(notification.type)]">
                        {{ getTypeLabel(notification.type) }}
                      </span>
                    </td>
                    <td class="px-4 py-4 text-sm text-gray-600 dark:text-gray-300">{{ notification.target }}</td>
                    <td class="px-4 py-4 text-sm text-gray-500 dark:text-gray-400">{{ notification.createdAt }}</td>
                    <td class="px-4 py-4">
                      <span class="px-2 py-1 text-xs rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400">
                        已发送
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <transition
        enter-active-class="transition ease-out duration-200"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition ease-in duration-150"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showUserModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click="showUserModal = false">
          <transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="opacity-0 scale-95"
            enter-to-class="opacity-100 scale-100"
            leave-active-class="transition ease-in duration-150"
            leave-from-class="opacity-100 scale-100"
            leave-to-class="opacity-0 scale-95"
          >
            <div v-if="showUserModal" class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md" @click.stop>
              <div class="p-4 border-b border-gray-200 dark:border-gray-700">
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white">选择用户</h3>
              </div>
              <div class="p-4">
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="搜索用户名或邮箱..."
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-red-500 focus:border-transparent mb-4"
                />
                <div class="max-h-64 overflow-y-auto space-y-2">
                  <div
                    v-for="user in filteredUsers"
                    :key="user.id"
                    @click="selectUser(user)"
                    class="flex items-center gap-3 p-3 rounded-lg cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    <img :src="user.avatar" class="w-10 h-10 rounded-full" />
                    <div>
                      <p class="text-sm font-medium text-gray-900 dark:text-white">{{ user.name }}</p>
                      <p class="text-xs text-gray-500 dark:text-gray-400">{{ user.email }}</p>
                    </div>
                  </div>
                </div>
              </div>
              <div class="p-4 border-t border-gray-200 dark:border-gray-700 flex justify-end">
                <button
                  @click="showUserModal = false"
                  class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                >
                  取消
                </button>
              </div>
            </div>
          </transition>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
