<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {NAvatar, NBadge, NButton, NEmpty, NSpin, NTabPane, NTabs} from 'naive-ui'
import {getUserInfo, interactionApi, isLoggedIn, uploadApi, userApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {emit, Events} from '@/utils/eventBus'
import OnlineIndicator from '@/components/OnlineIndicator.vue'
import wsManager from '@/utils/websocket'
import sseManager from '@/utils/sse'
import {useNotificationStore} from '@/stores/notification'
import {formatAbsoluteDate} from '@/utils/format'

const router = useRouter()
const route = useRoute()
const notificationStore = useNotificationStore()

// ========== 状态定义 ==========
const loading = ref(true)
const activeTab = ref('biz')  // biz | system | private

// ========== 气泡拖拽清除功能 ==========
const badgeRef = ref(null)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const currentX = ref(0)
const currentY = ref(0)
const isBursting = ref(false)
const burstParticles = ref([])
const clearZoneRef = ref(null)
const showClearZone = ref(false)
const hasTriggeredClear = ref(false)

// 业务通知
const bizNotifications = ref([])
const selectedBizNotification = ref(null)
const selectedGroup = ref(null)  // 选中的分组通知
const bizFilter = ref('all')  // all | like | comment | reply | follow | collection

// 系统通知
const systemNotifications = ref([])
const selectedSystemNotification = ref(null)

// 私信
const conversations = ref([])
const selectedConversation = ref(null)
const messageHistory = ref([])
const messagesScrollRef = ref(null)
const privateMessageInput = ref('')
const sendingMessage = ref(false)
const pendingImages = ref([])  // 待发送的图片列表
const uploadingImage = ref(false)  // 图片上传中状态

// 图片预览
const showImagePreview = ref(false)
const previewImageUrl = ref('')
const previewImageScale = ref(1)

// 未读数统计（从后端获取）
const bizUnreadCount = ref(0)
const systemUnreadCount = ref(0)
const privateUnreadCount = ref(0)

// ========== 删除功能 ==========
const selectedBizIds = ref(new Set())  // 选中的业务通知ID
const selectedSystemIds = ref(new Set())  // 选中的系统通知ID
const selectedConversationIds = ref(new Set())  // 选中的会话ID
const isDeleteMode = ref(false)  // 是否处于删除模式
const deleteLoading = ref(false)  // 删除中状态

// ========== 计算属性 ==========

// 业务通知过滤
const filteredBizNotifications = computed(() => {
  if (bizFilter.value === 'all') {
    return bizNotifications.value
  }
  return bizNotifications.value.filter(n => n.actionType === bizFilter.value)
})

// 业务通知分组折叠
const groupedBizNotifications = computed(() => {
  const groups = new Map()
  
  filteredBizNotifications.value.forEach(notification => {
    const key = `${notification.targetType}-${notification.targetId}-${notification.actionType}`
    
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        targetType: notification.targetType,
        targetId: notification.targetId,
        actionType: notification.actionType,
        targetTitle: notification.targetTitle,
        relatedId: notification.relatedId,
        notifications: [],  // 保存完整的通知信息
        latestTime: notification.createdAt,
        hasUnread: false,
        notificationIds: []
      })
    }
    
    const group = groups.get(key)
    group.notifications.push(notification)  // 保存完整通知
    group.notificationIds.push(notification.id)
    
    if (new Date(notification.createdAt) > new Date(group.latestTime)) {
      group.latestTime = notification.createdAt
    }
    
    if (!notification.isRead) {
      group.hasUnread = true
    }
  })
  
  return Array.from(groups.values()).sort((a, b) => 
    new Date(b.latestTime) - new Date(a.latestTime)
  )
})

// 未读数统计（使用后端接口返回的数据）
const unreadCounts = computed(() => ({
  biz: bizUnreadCount.value,
  system: systemUnreadCount.value,
  private: privateUnreadCount.value
}))

const totalUnread = computed(() => Object.values(unreadCounts.value).reduce((a, b) => a + b, 0))

// 监听筛选变化，重置选中状态
watch(bizFilter, () => {
  selectedGroup.value = null
  selectedBizNotification.value = null
})

// ========== API 请求 ==========

// 获取业务通知未读数
const fetchBizUnreadCount = async () => {
  try {
    const data = await interactionApi.getBizUnreadCount()
    bizUnreadCount.value = data.count || 0
  } catch (error) {
    console.error('获取业务通知未读数失败:', error)
  }
}

// 获取系统通知未读数
const fetchSystemUnreadCount = async () => {
  try {
    const data = await interactionApi.getSystemUnreadCount()
    systemUnreadCount.value = data.count || 0
  } catch (error) {
    console.error('获取系统通知未读数失败:', error)
  }
}

// 获取私信未读数
const fetchPrivateUnreadCount = async () => {
  try {
    const data = await interactionApi.getPrivateUnreadCount()
    privateUnreadCount.value = data.count || 0
  } catch (error) {
    console.error('获取私信未读数失败:', error)
  }
}

// 获取业务通知
const fetchBizNotifications = async () => {
  try {
    const data = await interactionApi.getBizNotifications({ current: 1, size: 100 })
    bizNotifications.value = (data.records || []).map(n => ({
      ...n,
      sender: n.sender || { id: n.senderId, name: '用户', avatar: '' }
    }))
  } catch (error) {
    console.error('获取业务通知失败:', error)
    bizNotifications.value = []
  }
}

// 获取系统通知
const fetchSystemNotifications = async () => {
  try {
    const data = await interactionApi.getSystemNotifications({ current: 1, size: 100 })
    systemNotifications.value = data.records || []
  } catch (error) {
    console.error('获取系统通知失败:', error)
    systemNotifications.value = []
  }
}

// 获取私信会话
const fetchConversations = async () => {
  try {
    const data = await interactionApi.getConversations()
    conversations.value = data || []
  } catch (error) {
    console.error('获取私信会话失败:', error)
    conversations.value = []
  }
}

// 获取消息历史
const fetchMessageHistory = async (userId) => {
  try {
    const data = await interactionApi.getMessageHistory(userId, 1, 100)
    messageHistory.value = (data.records || []).reverse()
    nextTick(() => scrollToBottom())
  } catch (error) {
    console.error('获取消息历史失败:', error)
    messageHistory.value = []
    nextTick(() => scrollToBottom())
  }
}

// 标记业务通知已读
const markBizNotificationRead = async (id) => {
  const notification = bizNotifications.value.find(n => n.id === id)
  const wasUnread = notification && !notification.isRead
  
  try {
    await interactionApi.markBizNotificationRead(id)
    if (wasUnread) {
      notification.isRead = true
      bizUnreadCount.value = Math.max(0, bizUnreadCount.value - 1)
      // 通知Navbar更新未读数
      emit(Events.NOTIFICATION_READ, 1)
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 标记系统通知已读
const markSystemNotificationRead = async (id) => {
  const notification = systemNotifications.value.find(n => n.id === id)
  const wasUnread = notification && !notification.isRead
  
  try {
    await interactionApi.markSystemNotificationRead(id)
    if (wasUnread) {
      notification.isRead = true
      systemUnreadCount.value = Math.max(0, systemUnreadCount.value - 1)
      // 通知Navbar更新未读数
      emit(Events.NOTIFICATION_READ, 1)
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 标记私信已读
const markMessagesRead = async (userId) => {
  const conversation = conversations.value.find(c => c.user?.id === userId)
  const oldUnreadCount = conversation?.unreadCount || 0
  
  try {
    await interactionApi.markMessagesRead(userId)
    if (conversation) {
      conversation.unreadCount = 0
      // 通知Navbar更新未读数
      if (oldUnreadCount > 0) {
        emit(Events.NOTIFICATION_READ, oldUnreadCount)
      }
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// ========== 删除功能 ==========

// 删除单个业务通知
const deleteBizNotification = async (notification) => {
  try {
    await interactionApi.deleteBizNotification(notification.id)
    // 如果删除的是未读通知，更新未读数
    if (!notification.isRead) {
      bizUnreadCount.value = Math.max(0, bizUnreadCount.value - 1)
    }
    // 从列表中移除
    const groupKey = `${notification.targetType}-${notification.targetId}-${notification.actionType}`
    const group = groupedBizNotifications.value.find(g => g.key === groupKey)
    if (group) {
      const idx = group.notifications.findIndex(n => n.id === notification.id)
      if (idx > -1) {
        group.notifications.splice(idx, 1)
        group.notificationIds = group.notificationIds.filter(id => id !== notification.id)
        // 如果组内没有通知了，移除整个组
        if (group.notifications.length === 0) {
          groupedBizNotifications.value = groupedBizNotifications.value.filter(g => g.key !== groupKey)
        }
      }
    }
    bizNotifications.value = bizNotifications.value.filter(n => n.id !== notification.id)
    toast.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败')
  }
}

// 删除单个系统通知
const deleteSystemNotification = async (notification) => {
  try {
    await interactionApi.deleteSystemNotification(notification.id)
    // 如果删除的是未读通知，更新未读数
    if (!notification.isRead) {
      systemUnreadCount.value = Math.max(0, systemUnreadCount.value - 1)
    }
    systemNotifications.value = systemNotifications.value.filter(n => n.id !== notification.id)
    if (selectedSystemNotification.value?.id === notification.id) {
      selectedSystemNotification.value = null
    }
    toast.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败')
  }
}

// 删除单个会话
const deleteConversation = async (conversation) => {
  try {
    await interactionApi.deleteConversation(conversation.id)
    conversations.value = conversations.value.filter(c => c.id !== conversation.id)
    if (selectedConversation.value?.id === conversation.id) {
      selectedConversation.value = null
      messageHistory.value = []
    }
    toast.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败')
  }
}

// 切换删除模式
const toggleDeleteMode = () => {
  isDeleteMode.value = !isDeleteMode.value
  if (!isDeleteMode.value) {
    // 退出删除模式时清除选中
    selectedBizIds.value.clear()
    selectedSystemIds.value.clear()
    selectedConversationIds.value.clear()
  }
}

// 切换业务通知选中状态
const toggleBizSelection = (notification) => {
  if (selectedBizIds.value.has(notification.id)) {
    selectedBizIds.value.delete(notification.id)
  } else {
    selectedBizIds.value.add(notification.id)
  }
  // 同步选中组内所有通知
  const groupKey = `${notification.targetType}-${notification.targetId}-${notification.actionType}`
  const group = groupedBizNotifications.value.find(g => g.key === groupKey)
  if (group) {
    group.notifications.forEach(n => {
      if (selectedBizIds.value.has(notification.id)) {
        selectedBizIds.value.add(n.id)
      } else {
        selectedBizIds.value.delete(n.id)
      }
    })
  }
}

// 切换系统通知选中状态
const toggleSystemSelection = (notification) => {
  if (selectedSystemIds.value.has(notification.id)) {
    selectedSystemIds.value.delete(notification.id)
  } else {
    selectedSystemIds.value.add(notification.id)
  }
}

// 切换会话选中状态
const toggleConversationSelection = (conversation) => {
  if (selectedConversationIds.value.has(conversation.id)) {
    selectedConversationIds.value.delete(conversation.id)
  } else {
    selectedConversationIds.value.add(conversation.id)
  }
}

// 批量删除选中的通知
const batchDeleteSelected = async () => {
  deleteLoading.value = true
  
  try {
    // 批量删除业务通知
    if (selectedBizIds.value.size > 0) {
      const ids = Array.from(selectedBizIds.value)
      await interactionApi.batchDeleteBizNotifications(ids)
      // 刷新业务通知和未读数
      await Promise.all([
        fetchBizNotifications(),
        fetchBizUnreadCount()
      ])
    }
    
    // 批量删除系统通知
    if (selectedSystemIds.value.size > 0) {
      const ids = Array.from(selectedSystemIds.value)
      await interactionApi.batchDeleteSystemNotifications(ids)
      systemNotifications.value = systemNotifications.value.filter(n => !selectedSystemIds.value.has(n.id))
      if (selectedSystemNotification.value && selectedSystemIds.value.has(selectedSystemNotification.value.id)) {
        selectedSystemNotification.value = null
      }
      // 重新获取系统通知未读数
      await fetchSystemUnreadCount()
    }
    
    // 批量删除会话
    if (selectedConversationIds.value.size > 0) {
      const ids = Array.from(selectedConversationIds.value)
      await interactionApi.batchDeleteConversations(ids)
      conversations.value = conversations.value.filter(c => !selectedConversationIds.value.has(c.id))
      if (selectedConversation.value && selectedConversationIds.value.has(selectedConversation.value.id)) {
        selectedConversation.value = null
        messageHistory.value = []
      }
    }
    
    toast.success(`已删除 ${selectedBizIds.value.size + selectedSystemIds.value.size + selectedConversationIds.value.size} 项`)
    
    // 退出删除模式
    toggleDeleteMode()
  } catch (error) {
    console.error('批量删除失败:', error)
    toast.error('批量删除失败')
  } finally {
    deleteLoading.value = false
  }
}

// 获取选中的数量
const totalSelectedCount = computed(() => {
  return selectedBizIds.value.size + selectedSystemIds.value.size + selectedConversationIds.value.size
})

// ========== 全选功能 ==========

// 判断是否全选
const isAllBizSelected = computed(() => {
  if (groupedBizNotifications.value.length === 0) return false
  return groupedBizNotifications.value.every(group => 
    group.notificationIds.some(id => selectedBizIds.value.has(id))
  )
})

const isAllSystemSelected = computed(() => {
  if (systemNotifications.value.length === 0) return false
  return systemNotifications.value.every(n => selectedSystemIds.value.has(n.id))
})

const isAllConversationSelected = computed(() => {
  if (conversations.value.length === 0) return false
  return conversations.value.every(c => selectedConversationIds.value.has(c.id))
})

// 切换全选
const toggleSelectAllBiz = () => {
  if (isAllBizSelected.value) {
    // 取消全选
    groupedBizNotifications.value.forEach(group => {
      group.notificationIds.forEach(id => {
        selectedBizIds.value.delete(id)
      })
    })
  } else {
    // 全选
    groupedBizNotifications.value.forEach(group => {
      group.notifications.forEach(n => {
        selectedBizIds.value.add(n.id)
      })
    })
  }
}

const toggleSelectAllSystem = () => {
  if (isAllSystemSelected.value) {
    // 取消全选
    systemNotifications.value.forEach(n => {
      selectedSystemIds.value.delete(n.id)
    })
  } else {
    // 全选
    systemNotifications.value.forEach(n => {
      selectedSystemIds.value.add(n.id)
    })
  }
}

const toggleSelectAllConversation = () => {
  if (isAllConversationSelected.value) {
    // 取消全选
    conversations.value.forEach(c => {
      selectedConversationIds.value.delete(c.id)
    })
  } else {
    // 全选
    conversations.value.forEach(c => {
      selectedConversationIds.value.add(c.id)
    })
  }
}

// 图片上传相关
const handleImageSelect = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  const file = files[0]
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件')
    return
  }
  
  // 验证文件大小（最大5MB）
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过5MB')
    return
  }
  
  uploadingImage.value = true
  
  try {
    // 创建本地预览
    const reader = new FileReader()
    reader.onload = (e) => {
      pendingImages.value.push({
        file: file,
        preview: e.target.result,
        name: file.name
      })
    }
    reader.readAsDataURL(file)
    
    // 清空input，允许重复选择同一文件
    event.target.value = ''
  } catch (error) {
    console.error('图片处理失败:', error)
    toast.error('图片处理失败')
  } finally {
    uploadingImage.value = false
  }
}

const removePendingImage = (index) => {
  pendingImages.value.splice(index, 1)
}

// 解析消息内容，支持图片显示
const parseMessageContent = (content) => {
  if (!content) return { text: '', images: [] }
  
  const images = []
  const imageRegex = /!\[image\]\((.*?)\)/g
  let match
  let textContent = content
  
  // 提取所有图片URL
  while ((match = imageRegex.exec(content)) !== null) {
    images.push(match[1])
    textContent = textContent.replace(match[0], '').trim()
  }
  
  return {
    text: textContent,
    images: images
  }
}

// 图片预览
const previewImage = (url) => {
  previewImageUrl.value = url
  previewImageScale.value = 1
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
  previewImageUrl.value = ''
  previewImageScale.value = 1
}

const zoomIn = () => {
  if (previewImageScale.value < 3) {
    previewImageScale.value += 0.2
  }
}

const zoomOut = () => {
  if (previewImageScale.value > 0.5) {
    previewImageScale.value -= 0.2
  }
}

const resetZoom = () => {
  previewImageScale.value = 1
}

// 鼠标滚轮缩放
const handleWheel = (e) => {
  e.preventDefault()
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

// 格式化会话最后一条消息显示
const formatLastMessage = (message) => {
  if (!message) return ''
  
  const parsed = parseMessageContent(message)
  
  // 如果有图片，显示[图片]
  if (parsed.images.length > 0) {
    return parsed.text ? `${parsed.text} [图片]` : '[图片]'
  }
  
  return parsed.text || message
}

// 发送私信
const sendMessage = async () => {
  if (!selectedConversation.value) return
  
  const hasText = privateMessageInput.value.trim()
  const hasImages = pendingImages.value.length > 0
  
  if (!hasText && !hasImages) return
  
  const receiverId = selectedConversation.value.user.id
  sendingMessage.value = true
  
  try {
    let messageContent = privateMessageInput.value.trim()
    let uploadedImageUrls = []
    
    // 上传图片
    if (hasImages) {
      for (const image of pendingImages.value) {
        try {
          const result = await uploadApi.uploadFile(image.file)
          if (result) {
            uploadedImageUrls.push(result)
          }
        } catch (error) {
          console.error('图片上传失败:', error)
          toast.error(`图片 ${image.name} 上传失败: ${error.message}`)
        }
      }
    }
    
    // 构建消息内容
    if (uploadedImageUrls.length > 0) {
      const imageMarkdown = uploadedImageUrls.map(url => `![image](${url})`).join('\n')
      messageContent = messageContent ? `${messageContent}\n${imageMarkdown}` : imageMarkdown
    }
    
    if (!messageContent) {
      toast.error('消息内容为空')
      return
    }
    
    // 发送消息
    const result = await interactionApi.sendMessage(receiverId, messageContent)
    
    if (result) {
      messageHistory.value.push({
        id: result.id || Date.now(),
        senderId: getUserInfo()?.id,
        receiverId: receiverId,
        type: 'sent',
        content: messageContent,
        createdAt: result.createdAt || new Date().toISOString()
      })
      selectedConversation.value.lastMessage = hasText ? hasText : '[图片]'
      privateMessageInput.value = ''
      pendingImages.value = []
      nextTick(() => scrollToBottom())
      toast.success('发送成功')
    }
  } catch (error) {
    console.error('发送失败:', error)
    toast.error(error.message || '发送失败')
  } finally {
    sendingMessage.value = false
  }
}

// ========== 拖拽气泡清除功能 ==========

// 开始拖拽
const onDragStart = (e) => {
  if (totalUnread.value === 0) return
  isDragging.value = true
  showClearZone.value = true
  hasTriggeredClear.value = false
  
  const touch = e.touches ? e.touches[0] : e
  dragStartX.value = touch.clientX
  dragStartY.value = touch.clientY
  currentX.value = 0
  currentY.value = 0
  
  // 添加全局事件监听
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
}

// 拖拽中
const onDragMove = (e) => {
  if (!isDragging.value) return
  e.preventDefault()
  
  const touch = e.touches ? e.touches[0] : e
  const deltaX = touch.clientX - dragStartX.value
  const deltaY = touch.clientY - dragStartY.value
  
  // 限制拖拽范围
  currentX.value = Math.max(-100, Math.min(100, deltaX))
  currentY.value = Math.max(-50, Math.min(50, deltaY))
  
  // 检测是否进入清除区域
  checkClearZone(touch.clientX, touch.clientY)
}

// 检查是否进入清除区域
const checkClearZone = (x, y) => {
  if (hasTriggeredClear.value || !clearZoneRef.value) return
  
  const rect = clearZoneRef.value.getBoundingClientRect()
  if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
    triggerBurst()
  }
}

// 拖拽结束
const onDragEnd = () => {
  if (!isDragging.value) return
  isDragging.value = false
  
  // 移除全局事件监听
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
  
  // 如果没有触发清除，恢复原位
  if (!hasTriggeredClear.value) {
    currentX.value = 0
    currentY.value = 0
    setTimeout(() => {
      showClearZone.value = false
    }, 300)
  }
}

// 触发气泡破裂效果
const triggerBurst = async () => {
  if (hasTriggeredClear.value) return
  hasTriggeredClear.value = true
  
  // 生成粒子
  burstParticles.value = Array.from({ length: 12 }, (_, i) => ({
    id: i,
    angle: (i * 30) + Math.random() * 20,
    distance: 40 + Math.random() * 30,
    size: 4 + Math.random() * 6,
    duration: 400 + Math.random() * 200
  }))
  
  // 触发破裂动画
  isBursting.value = true
  isDragging.value = false
  
  // 隐藏清除区域
  showClearZone.value = false
  
  // 等待动画完成后清除所有未读
  setTimeout(async () => {
    await clearAllUnread()
    isBursting.value = false
    burstParticles.value = []
    currentX.value = 0
    currentY.value = 0
  }, 600)
}

// 清除所有未读
const clearAllUnread = async () => {
  try {
    // 调用统一的一键清除接口
    await interactionApi.clearAllUnread()
    
    // 更新本地状态
    bizNotifications.value.forEach(n => n.isRead = true)
    systemNotifications.value.forEach(n => n.isRead = true)
    conversations.value.forEach(c => c.unreadCount = 0)
    
    // 重置未读数
    bizUnreadCount.value = 0
    systemUnreadCount.value = 0
    privateUnreadCount.value = 0
    
    // 通知Navbar更新未读数为0
    emit(Events.REFRESH_UNREAD_COUNT)
    
    toast.success('已清除所有未读消息')
  } catch (error) {
    console.error('清除未读失败:', error)
    toast.error('清除失败，请重试')
  }
}

// ========== 操作方法 ==========

// Tab切换
const switchTab = (tab) => {
  activeTab.value = tab
  selectedBizNotification.value = null
  selectedSystemNotification.value = null
  selectedConversation.value = null
  selectedGroup.value = null
  
  if (tab !== 'private') {
    notificationStore.clearCurrentChatUser()
  }
}

// 选择业务通知
const selectBizNotification = (notification) => {
  selectedBizNotification.value = notification
  if (!notification.isRead) {
    markBizNotificationRead(notification.id)
    notification.isRead = true
  }
}

// 选择分组通知
const selectGroupedNotification = (group) => {
  selectedGroup.value = group
  selectedBizNotification.value = bizNotifications.value.find(n => n.id === group.notificationIds[0])
  
  // 标记组内所有通知为已读
  if (group.hasUnread) {
    let unreadCount = 0
    group.notificationIds.forEach(id => {
      const notification = bizNotifications.value.find(n => n.id === id)
      if (notification && !notification.isRead) {
        markBizNotificationRead(id)
        notification.isRead = true
        unreadCount++
      }
    })
    group.hasUnread = false
  }
}

// 选择系统通知
const selectSystemNotification = (notification) => {
  selectedSystemNotification.value = notification
  if (!notification.isRead) {
    markSystemNotificationRead(notification.id)
    notification.isRead = true
  }
}

// 选择私信会话
const selectConversation = (conversation) => {
  selectedConversation.value = conversation
  fetchMessageHistory(conversation.user.id)
  
  notificationStore.setCurrentChatUser(conversation.user.id)
  
  if (conversation.unreadCount > 0) {
    const oldUnreadCount = conversation.unreadCount
    conversation.unreadCount = 0
    markMessagesRead(conversation.user.id)
    // 更新私信未读数
    privateUnreadCount.value = Math.max(0, privateUnreadCount.value - oldUnreadCount)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesScrollRef.value) {
    messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}


// 获取行为类型配置
const getActionConfig = (actionType, targetType) => {
  const configs = {
    like: { name: '点赞', color: '#ef4444', icon: '❤️', bgColor: 'bg-red-50 dark:bg-red-900/20' },
    comment: { name: '评论', color: '#8b5cf6', icon: '💬', bgColor: 'bg-purple-50 dark:bg-purple-900/20' },
    reply: { name: '回复', color: '#3b82f6', icon: '↩️', bgColor: 'bg-blue-50 dark:bg-blue-900/20' },
    follow: { name: '关注了你', color: '#10b981', icon: '👤', bgColor: 'bg-green-50 dark:bg-green-900/20' },
    collection: { name: '收藏', color: '#f59e0b', icon: '⭐', bgColor: 'bg-yellow-50 dark:bg-yellow-900/20' }
  }
  if (actionType === 'follow') {
    return configs.follow
  }
  return configs[actionType] || configs.like
}

// 获取行为描述文本
const getActionText = (actionType, targetType) => {
  if (actionType === 'follow') {
    return '关注了你'
  }
  return `${getActionConfig(actionType).name}了你的${getTargetTypeName(targetType)}`
}

// 获取目标类型名称
const getTargetTypeName = (targetType) => {
  const names = {
    article: '文章',
    comment: '评论',
    user: '用户'
  }
  return names[targetType] || '内容'
}

// 跳转详情
const goToTarget = (notification) => {
  if (notification.targetType === 'article') {
    // 评论文章的通知：targetId=文章ID, relatedId=评论ID
    if (notification.relatedId && (notification.actionType === 'comment' || notification.actionType === 'like' || notification.actionType === 'collection')) {
      router.push({
        path: `/article/${notification.targetId}`,
        query: { commentId: notification.relatedId }
      })
    } else {
      router.push(`/article/${notification.targetId}`)
    }
  } else if (notification.targetType === 'comment') {
    // 回复评论的通知：targetId=父评论ID, relatedId=文章ID
    if (notification.relatedId) {
      router.push({
        path: `/article/${notification.relatedId}`,
        query: { commentId: notification.targetId }
      })
    }
  } else if (notification.targetType === 'user') {
    router.push(`/user/${notification.targetId}`)
  }
}

// 跳转到评论详情
const goToCommentDetail = (notification) => {
  let articleId, commentId
  
  if (notification.actionType === 'comment') {
    articleId = notification.targetId
    commentId = notification.relatedId
  } else if (notification.actionType === 'reply') {
    articleId = notification.relatedId
    commentId = notification.targetId
  } else if (notification.actionType === 'like' && notification.targetType === 'comment') {
    articleId = notification.relatedId
    commentId = notification.targetId
  }
  
  if (articleId && commentId) {
    router.push({
      path: `/article/${articleId}`,
      query: { commentId: commentId }
    })
  } else if (articleId) {
    router.push(`/article/${articleId}`)
  }
}

// 跳转用户主页
const goToUserProfile = (userId) => {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

// 打开与指定用户的会话
const openConversationWithUser = async (userId) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    router.push('/')
    return
  }
  
  activeTab.value = 'private'
  
  await fetchConversations()
  
  const userIdStr = String(userId)
  const currentUserId = getUserInfo()?.id
  
  if (currentUserId && String(currentUserId) === userIdStr) {
    toast.error('不能给自己发私信')
    return
  }
  
  const conversation = conversations.value.find(c => String(c.user?.id) === userIdStr)
  
  if (conversation) {
    selectConversation(conversation)
  } else {
    const newConversation = {
      id: `temp-${Date.now()}`,
      user: {
        id: userIdStr,
        name: '用户',
        avatar: ''
      },
      lastMessage: '',
      unreadCount: 0,
      updatedAt: new Date().toISOString()
    }
    
    try {
      const authorData = await userApi.getAuthorInfo(userIdStr)
      if (authorData) {
        newConversation.user.name = authorData.nickname || authorData.username
        newConversation.user.avatar = authorData.avatar
        newConversation.user.id = String(authorData.id)
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      toast.error('获取用户信息失败')
      return
    }
    
    conversations.value.unshift(newConversation)
    selectedConversation.value = newConversation
    messageHistory.value = []
    
    await nextTick()
    scrollToBottom()
  }
}

// ========== WebSocket 实时消息处理 ==========
const handleRealtimeMessage = async (data) => {
  // 检查是否正在查看与发送者的对话
  if (selectedConversation.value && 
      String(selectedConversation.value.user?.id) === String(data.senderId)) {
    // 添加消息到历史记录
    messageHistory.value.push({
      id: data.messageId || Date.now(),
      senderId: data.senderId,
      receiverId: getUserInfo()?.id,
      type: 'received',
      content: data.content,
      createdAt: data.createdAt || new Date().toISOString()
    })
    
    // 更新会话的最后一条消息
    selectedConversation.value.lastMessage = data.content
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 标记消息为已读
    try {
      await interactionApi.markMessagesRead(data.senderId)
      // 重置会话未读数
      if (selectedConversation.value) {
        selectedConversation.value.unreadCount = 0
      }
    } catch (error) {
      console.error('[Notifications] 标记已读失败:', error)
    }
  } else {
  }
  
  // 更新会话列表中的最后一条消息
  const conversation = conversations.value.find(c => String(c.user?.id) === String(data.senderId))
  if (conversation) {
    conversation.lastMessage = data.content
    conversation.updatedAt = new Date().toISOString()
    
    // 如果不是当前对话，增加未读数
    if (!selectedConversation.value || 
        String(selectedConversation.value.user?.id) !== String(data.senderId)) {
      conversation.unreadCount = (conversation.unreadCount || 0) + 1
    }
  }
}

// ========== 生命周期 ==========
onMounted(async () => {
  loading.value = true
  await Promise.all([
    fetchBizNotifications(),
    fetchSystemNotifications(),
    fetchConversations(),
    fetchBizUnreadCount(),
    fetchSystemUnreadCount(),
    fetchPrivateUnreadCount()
  ])
  loading.value = false
  
  const userId = route.query.userId
  if (userId) {
    await openConversationWithUser(userId)
  }
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  
  // 监听WebSocket实时消息
  wsManager.on('private_message', handleRealtimeMessage)
  
  // 监听 SSE 通知
  sseManager.on('notification', handleSseNotification)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  
  wsManager.off('private_message', handleRealtimeMessage)
  
  // 移除 SSE 监听
  sseManager.off('notification', handleSseNotification)
  
  notificationStore.clearCurrentChatUser()
})

// 键盘事件处理
const handleKeydown = (e) => {
  if (e.key === 'Escape' && showImagePreview.value) {
    closeImagePreview()
  }
}

// 处理 SSE 通知
const handleSseNotification = (notification) => {
  // 将新通知添加到列表顶部
  bizNotifications.value.unshift({
    ...notification,
    sender: notification.senderNickname ? {
      id: notification.senderId,
      nickname: notification.senderNickname,
      avatar: notification.senderAvatar
    } : { id: notification.senderId, nickname: '用户', avatar: '' },
    isRead: false
  })
  
  // 更新未读数
  bizUnreadCount.value++
  
  // 触发全局未读数更新
  emit(Events.REFRESH_UNREAD_COUNT)
}
</script>

<template>
  <div class="h-[calc(100vh-64px)] flex bg-gray-50 dark:bg-gray-900">
    <!-- ========== 左侧栏 ========== -->
    <div class="relative w-80 bg-white dark:bg-gray-800 flex flex-col border-r border-gray-200 dark:border-gray-700">
      <!-- 拖拽清除区域提示 -->
      <div 
        v-if="showClearZone && !hasTriggeredClear"
        ref="clearZoneRef"
        class="absolute top-16 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-full bg-gradient-to-r from-red-500 to-orange-500 text-white text-sm font-medium shadow-lg clear-zone"
      >
        <span class="flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          释放清除所有未读 ✨
        </span>
      </div>

      <!-- 顶部标题 -->
      <div class="p-6 border-b border-gray-100 dark:border-gray-700">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold text-gray-800 dark:text-white">消息中心</h2>
          
          <!-- 可拖拽的未读气泡 -->
          <div 
            ref="badgeRef"
            class="relative cursor-grab active:cursor-grabbing select-none"
            :class="{ 'dragging': isDragging, 'pointer-events-none': totalUnread === 0 }"
            @mousedown="onDragStart"
            @touchstart="onDragStart"
          >
            <n-badge 
              :value="totalUnread" 
              :max="99" 
              :show="totalUnread > 0 && !isBursting"
              class="badge-container"
              :class="{ 'bursting': isBursting }"
              :style="{
                transform: `translate(${currentX}px, ${currentY}px) rotate(${currentX * 0.5}deg)`,
                transition: isDragging ? 'none' : 'transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)'
              }"
            >
              <div class="w-8 h-8 rounded-full bg-pink-100 dark:bg-pink-900/30 flex items-center justify-center">
                <svg class="w-5 h-5 text-pink-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
            </n-badge>
            
            <!-- 气泡破裂粒子效果 -->
            <div v-if="isBursting" class="absolute inset-0 pointer-events-none overflow-visible">
              <div
                v-for="(particle, index) in burstParticles"
                :key="particle.id"
                class="absolute rounded-full bg-gradient-to-r from-pink-400 to-pink-600 burst-particle"
                :style="{
                  width: particle.size + 'px',
                  height: particle.size + 'px',
                  '--angle': particle.angle + 'deg',
                  '--distance': particle.distance + 'px',
                  '--duration': particle.duration + 'ms',
                  '--delay': (index * 30) + 'ms',
                  left: '50%',
                  top: '50%',
                  marginLeft: (-particle.size / 2) + 'px',
                  marginTop: (-particle.size / 2) + 'px'
                }"
              />
            </div>
          </div>
        </div>
        
        <!-- 拖拽提示（首次显示） -->
        <div 
          v-if="totalUnread > 10 && !isDragging && !showClearZone"
          class="mt-3 text-xs text-gray-400 dark:text-gray-500 flex items-center gap-1"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          提示：拖动气泡可一键清除所有未读
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <div class="flex items-center justify-between mb-2">
          <n-tabs v-model:value="activeTab" type="line" justify-content="space-around" class="flex-1">
            <n-tab-pane name="biz" tab="通知">
              <template #tab>
                <div class="flex items-center gap-2">
                  <span>通知</span>
                  <n-badge v-if="unreadCounts.biz > 0" :value="unreadCounts.biz" :max="99" />
                </div>
              </template>
            </n-tab-pane>
            <n-tab-pane name="system" tab="系统">
              <template #tab>
                <div class="flex items-center gap-2">
                  <span>系统</span>
                  <n-badge v-if="unreadCounts.system > 0" :value="unreadCounts.system" :max="99" />
                </div>
              </template>
            </n-tab-pane>
            <n-tab-pane name="private" tab="私信">
              <template #tab>
                <div class="flex items-center gap-2">
                  <span>私信</span>
                  <n-badge v-if="unreadCounts.private > 0" :value="unreadCounts.private" :max="99" />
                </div>
              </template>
            </n-tab-pane>
          </n-tabs>
          
          <!-- 删除/编辑按钮 -->
          <button
            @click="toggleDeleteMode"
            class="ml-3 p-2 rounded-lg transition-colors"
            :class="isDeleteMode 
              ? 'bg-red-100 dark:bg-red-900/30 text-red-500' 
              : 'hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500'"
            :title="isDeleteMode ? '取消删除' : '编辑删除'"
          >
            <svg v-if="!isDeleteMode" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <!-- 批量删除操作栏 -->
        <div v-if="isDeleteMode && totalSelectedCount > 0" class="mt-2 p-2 bg-red-50 dark:bg-red-900/20 rounded-lg flex items-center justify-between">
          <span class="text-sm text-red-600 dark:text-red-400">
            已选择 {{ totalSelectedCount }} 项
          </span>
          <n-button 
            type="error" 
            size="small" 
            :loading="deleteLoading"
            @click="batchDeleteSelected"
          >
            <template #icon>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </template>
            批量删除
          </n-button>
        </div>
      </div>

      <!-- 列表内容 -->
      <div class="flex-1 overflow-y-auto">
        <div v-if="loading" class="flex items-center justify-center py-16">
          <n-spin size="large" />
        </div>

        <!-- 业务通知列表 -->
        <template v-else-if="activeTab === 'biz'">
          <!-- 筛选器 -->
          <div class="px-4 py-2 border-b border-gray-100 dark:border-gray-700 flex gap-2 flex-wrap items-center">
            <button
              @click="bizFilter = 'all'"
              class="px-3 py-1 text-xs rounded-full transition-colors"
              :class="bizFilter === 'all' ? 'bg-pink-500 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'"
            >
              全部
            </button>
            <button
              v-for="(config, key) in { like: {name: '点赞'}, comment: {name: '评论'}, reply: {name: '回复'}, follow: {name: '关注'}, collection: {name: '收藏'} }"
              :key="key"
              @click="bizFilter = key"
              class="px-3 py-1 text-xs rounded-full transition-colors"
              :class="bizFilter === key ? 'bg-pink-500 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'"
            >
              {{ config.name }}
            </button>
            <!-- 全选按钮 -->
            <div v-if="isDeleteMode && groupedBizNotifications.length > 0" class="ml-auto flex items-center gap-1">
              <input
                type="checkbox"
                :checked="isAllBizSelected"
                @change="toggleSelectAllBiz"
                class="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
              />
              <span class="text-xs text-gray-500 dark:text-gray-400 cursor-pointer" @click="toggleSelectAllBiz">全选</span>
            </div>
          </div>

          <!-- 通知列表 -->
          <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
            <div
              v-for="group in groupedBizNotifications"
              :key="group.key"
              class="relative transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
              :class="{ 'bg-blue-50/30 dark:bg-blue-900/10': group.hasUnread, 'bg-pink-50 dark:bg-pink-900/10': selectedGroup?.key === group.key }"
            >
              <div class="flex items-center">
                <!-- 删除模式下的复选框 -->
                <div v-if="isDeleteMode" class="pl-4 pr-2">
                  <input
                    type="checkbox"
                    :checked="group.notificationIds.some(id => selectedBizIds.has(id))"
                    @change="toggleBizSelection(group.notifications[0])"
                    class="w-5 h-5 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
                  />
                </div>
                
                <!-- 通知内容 -->
                <div 
                  class="flex-1 px-4 py-3 cursor-pointer"
                  :class="{ 'pointer-events-none': isDeleteMode }"
                  @click="!isDeleteMode && selectGroupedNotification(group)"
                >
                  <!-- 未读红点 -->
                  <div
                    v-if="group.hasUnread"
                    class="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full"
                  ></div>
                  <div class="flex gap-3">
                    <!-- 堆叠头像 -->
                    <div class="relative flex-shrink-0 cursor-pointer" :style="{ width: Math.min(group.notifications.length * 16 + 40, 68) + 'px', height: '40px' }" @click="goToUserProfile(group.notifications[0]?.sender?.id)">
                      <!-- 第一个头像 -->
                      <n-avatar 
                        v-if="group.notifications[0]" 
                        :src="group.notifications[0].sender?.avatar" 
                        :size="40" 
                        round 
                        class="absolute left-0 top-0 ring-2 ring-white dark:ring-gray-800 hover:ring-pink-400 transition-all"
                      />
                      <!-- 第二个头像 -->
                      <n-avatar 
                        v-if="group.notifications[1]" 
                        :src="group.notifications[1].sender?.avatar" 
                        :size="40" 
                        round 
                        class="absolute left-3 top-0 ring-2 ring-white dark:ring-gray-800 hover:ring-pink-400 transition-all"
                      />
                      <!-- 第三个头像 -->
                      <n-avatar 
                        v-if="group.notifications[2]" 
                        :src="group.notifications[2].sender?.avatar" 
                        :size="40" 
                        round 
                        class="absolute left-6 top-0 ring-2 ring-white dark:ring-gray-800 hover:ring-pink-400 transition-all"
                      />
                      <!-- 行为类型图标 -->
                      <div
                        class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center text-xs z-10"
                        :class="getActionConfig(group.actionType).bgColor"
                      >
                        {{ getActionConfig(group.actionType).icon }}
                      </div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between mb-1">
                        <span class="text-sm font-medium text-gray-800 dark:text-white">
                          <!-- 显示发送者名称 -->
                          <template v-if="group.notifications.length === 1">
                            {{ group.notifications[0].sender?.name }}
                          </template>
                          <template v-else-if="group.notifications.length === 2">
                            {{ group.notifications[0].sender?.name }}、{{ group.notifications[1].sender?.name }}
                          </template>
                          <template v-else>
                            {{ group.notifications[0].sender?.name }}、{{ group.notifications[1].sender?.name }}<span class="text-gray-500 dark:text-gray-400">...等{{ group.notifications.length }}人</span>
                          </template>
                        </span>
                        <span class="text-xs text-gray-400">{{ formatAbsoluteDate(group.latestTime) }}</span>
                      </div>
                      <p class="text-xs text-gray-600 dark:text-gray-300">
                        {{ getActionText(group.actionType, group.targetType) }}
                        <span v-if="group.targetTitle" class="text-gray-800 dark:text-white font-medium">「{{ group.targetTitle }}」</span>
                      </p>
                    </div>
                  </div>
                </div>
                
                <!-- 单个删除按钮 -->
                <button
                  v-if="!isDeleteMode"
                  @click.stop="deleteBizNotification(group.notifications[0])"
                  class="mr-2 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                  title="删除"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <n-empty v-if="groupedBizNotifications.length === 0" description="暂无通知" class="py-16" />
        </template>

        <!-- 系统通知列表 -->
        <template v-else-if="activeTab === 'system'">
          <!-- 全选按钮 -->
          <div v-if="isDeleteMode && systemNotifications.length > 0" class="px-4 py-2 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2 bg-gray-50 dark:bg-gray-800/50">
            <input
              type="checkbox"
              :checked="isAllSystemSelected"
              @change="toggleSelectAllSystem"
              class="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
            />
            <span class="text-xs text-gray-600 dark:text-gray-400 cursor-pointer" @click="toggleSelectAllSystem">全选</span>
            <span class="text-xs text-gray-400">共 {{ systemNotifications.length }} 条</span>
          </div>
          <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
            <div
              v-for="notification in systemNotifications"
              :key="notification.id"
              class="relative transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
              :class="{ 'bg-blue-50/30 dark:bg-blue-900/10': !notification.isRead, 'bg-pink-50 dark:bg-pink-900/10': selectedSystemNotification?.id === notification.id }"
            >
              <div class="flex items-center">
                <!-- 删除模式下的复选框 -->
                <div v-if="isDeleteMode" class="pl-4 pr-2">
                  <input
                    type="checkbox"
                    :checked="selectedSystemIds.has(notification.id)"
                    @change="toggleSystemSelection(notification)"
                    class="w-5 h-5 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
                  />
                </div>
                
                <!-- 通知内容 -->
                <div 
                  class="flex-1 px-4 py-3 cursor-pointer"
                  :class="{ 'pointer-events-none': isDeleteMode }"
                  @click="!isDeleteMode && selectSystemNotification(notification)"
                >
                  <!-- 未读红点 -->
                  <div
                    v-if="!notification.isRead"
                    class="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full"
                  ></div>
                  <div class="flex gap-3">
                    <div class="relative flex-shrink-0">
                      <div class="w-11 h-11 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-xl">📢</div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between mb-1">
                        <span class="text-sm font-medium text-gray-800 dark:text-white">{{ notification.title }}</span>
                        <span class="text-xs text-gray-400">{{ formatAbsoluteDate(notification.createdAt) }}</span>
                      </div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 line-clamp-2">{{ notification.content }}</p>
                    </div>
                  </div>
                </div>
                
                <!-- 单个删除按钮 -->
                <button
                  v-if="!isDeleteMode"
                  @click.stop="deleteSystemNotification(notification)"
                  class="mr-2 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                  title="删除"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <n-empty v-if="systemNotifications.length === 0" description="暂无系统通知" class="py-16" />
        </template>

        <!-- 私信列表 -->
        <template v-else-if="activeTab === 'private'">
          <!-- 全选按钮 -->
          <div v-if="isDeleteMode && conversations.length > 0" class="px-4 py-2 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2 bg-gray-50 dark:bg-gray-800/50">
            <input
              type="checkbox"
              :checked="isAllConversationSelected"
              @change="toggleSelectAllConversation"
              class="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
            />
            <span class="text-xs text-gray-600 dark:text-gray-400 cursor-pointer" @click="toggleSelectAllConversation">全选</span>
            <span class="text-xs text-gray-400">共 {{ conversations.length }} 条</span>
          </div>
          <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
            <div
              v-for="conversation in conversations"
              :key="conversation.id"
              class="px-4 py-3 transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
              :class="{ 'bg-pink-50 dark:bg-pink-900/10': selectedConversation?.id === conversation.id }"
            >
              <div class="flex items-center">
                <!-- 删除模式下的复选框 -->
                <div v-if="isDeleteMode" class="pr-2">
                  <input
                    type="checkbox"
                    :checked="selectedConversationIds.has(conversation.id)"
                    @change="toggleConversationSelection(conversation)"
                    class="w-5 h-5 rounded border-gray-300 text-red-500 focus:ring-red-500 cursor-pointer"
                  />
                </div>
                
                <!-- 会话内容 -->
                <div 
                  class="flex-1 flex gap-3 cursor-pointer"
                  :class="{ 'pointer-events-none': isDeleteMode }"
                  @click="!isDeleteMode && selectConversation(conversation)"
                >
                  <div class="relative flex-shrink-0 cursor-pointer" @click="goToUserProfile(conversation.user?.id)">
                    <n-avatar :src="conversation.user?.avatar" :size="44" round class="hover:ring-2 hover:ring-pink-400 transition-all" />
                    <div class="absolute bottom-0 right-0">
                      <OnlineIndicator v-if="conversation.user?.id" :userId="conversation.user.id" size="sm" />
                    </div>
                    <div
                      v-if="conversation.unreadCount > 0"
                      class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center"
                    >
                      {{ conversation.unreadCount > 9 ? '9+' : conversation.unreadCount }}
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between mb-1">
                      <span class="text-sm font-medium text-gray-800 dark:text-white">{{ conversation.user?.name }}</span>
                      <span class="text-xs text-gray-400">{{ formatAbsoluteDate(conversation.updatedAt) }}</span>
                    </div>
                    <p class="text-xs truncate" :class="conversation.unreadCount > 0 ? 'text-gray-700 dark:text-gray-200 font-medium' : 'text-gray-500 dark:text-gray-400'">
                      {{ formatLastMessage(conversation.lastMessage) }}
                    </p>
                  </div>
                </div>
                
                <!-- 单个删除按钮 -->
                <button
                  v-if="!isDeleteMode"
                  @click.stop="deleteConversation(conversation)"
                  class="ml-2 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                  title="删除"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <n-empty v-if="conversations.length === 0" description="暂无私信" class="py-16" />
        </template>
      </div>
    </div>

    <!-- ========== 右侧详情 ========== -->
    <div class="flex-1 flex flex-col bg-gray-50 dark:bg-gray-900">
      <!-- 业务通知详情 -->
      <template v-if="activeTab === 'biz' && selectedGroup">
        <div class="flex-1 overflow-y-auto p-6">
          <div class="max-w-2xl mx-auto">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm p-6">
              <!-- 头部标题 -->
              <div class="text-center mb-6">
                <div
                  class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center text-4xl"
                  :class="getActionConfig(selectedGroup.actionType).bgColor"
                >
                  {{ getActionConfig(selectedGroup.actionType).icon }}
                </div>
                <p class="text-xl font-medium text-gray-800 dark:text-white mb-2">
                  {{ selectedGroup.notifications.length }}人{{ getActionText(selectedGroup.actionType, selectedGroup.targetType) }}
                </p>
                <p v-if="selectedGroup.targetTitle" class="text-sm text-gray-500 dark:text-gray-400">
                  「{{ selectedGroup.targetTitle }}」
                </p>
              </div>

              <!-- 评论/回复/回答列表 -->
              <div class="border-t border-gray-100 dark:border-gray-700 pt-6">
                <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">
                  {{ getActionConfig(selectedGroup.actionType).name }}列表
                </h4>
                <div class="space-y-4">
                  <div
                    v-for="(notification, index) in selectedGroup.notifications"
                    :key="notification.id"
                    class="p-4 rounded-xl bg-gray-50 dark:bg-gray-700/30 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors"
                    @click="goToCommentDetail(notification)"
                  >
                    <div class="flex items-start gap-3 mb-2">
                      <n-avatar :src="notification.sender?.avatar" :size="40" round class="cursor-pointer hover:ring-2 hover:ring-pink-400 transition-all" @click.stop="goToUserProfile(notification.sender?.id)" />
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between mb-1">
                          <span class="text-sm font-medium text-gray-800 dark:text-white">{{ notification.sender?.name }}</span>
                          <span class="text-xs text-gray-400">{{ formatAbsoluteDate(notification.createdAt) }}</span>
                        </div>
                        <p v-if="notification.content" class="text-sm text-gray-600 dark:text-gray-300 mb-3">
                          {{ notification.content }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="mt-6 pt-6 border-t border-gray-100 dark:border-gray-700">
                <n-button
                  v-if="selectedGroup.targetType !== 'user'"
                  type="primary"
                  block
                  size="large"
                  @click="goToTarget({ targetType: selectedGroup.targetType, targetId: selectedGroup.targetId, relatedId: selectedGroup.notifications[0]?.relatedId, actionType: selectedGroup.actionType })"
                >
                  查看{{ getTargetTypeName(selectedGroup.targetType) }}详情
                </n-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 系统通知详情 -->
      <template v-else-if="activeTab === 'system' && selectedSystemNotification">
        <div class="flex-1 overflow-y-auto p-6">
          <div class="max-w-2xl mx-auto">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm p-6">
              <!-- 头部 -->
              <div class="text-center mb-6">
                <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-4xl">📢</div>
                <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-2">{{ selectedSystemNotification.title }}</h3>
                <p class="text-sm text-gray-500 dark:text-gray-400">{{ formatAbsoluteDate(selectedSystemNotification.createdAt) }}</p>
              </div>

              <!-- 内容 -->
              <div class="p-6 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                <p class="text-base text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-wrap">{{ selectedSystemNotification.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 私信聊天 -->
      <template v-else-if="activeTab === 'private' && selectedConversation">
        <!-- 聊天头部 -->
        <div class="h-16 px-6 flex items-center gap-3 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
          <n-avatar :src="selectedConversation.user?.avatar" :size="40" round class="cursor-pointer hover:ring-2 hover:ring-pink-400 transition-all" @click="goToUserProfile(selectedConversation.user?.id)" />
          <div class="flex-1 cursor-pointer" @click="goToUserProfile(selectedConversation.user?.id)">
            <h3 class="text-base font-semibold text-gray-800 dark:text-white">{{ selectedConversation.user?.name }}</h3>
            <OnlineIndicator v-if="selectedConversation.user?.id" :userId="selectedConversation.user.id" size="sm" show-text />
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="messagesScrollRef" class="flex-1 overflow-y-auto p-6 space-y-4">
          <div v-for="message in messageHistory" :key="message.id">
            <!-- 对方消息 -->
            <div v-if="message.type === 'received'" class="flex gap-3 mb-4">
              <n-avatar :src="selectedConversation.user?.avatar" :size="36" round class="flex-shrink-0 mt-1 cursor-pointer hover:ring-2 hover:ring-pink-400 transition-all" @click="goToUserProfile(selectedConversation.user?.id)" />
              <div class="max-w-[70%]">
                <div class="flex items-end gap-2">
                  <div class="px-4 py-3 bg-white dark:bg-gray-700 rounded-2xl rounded-bl-md shadow-sm">
                    <!-- 文字内容 -->
                    <p v-if="parseMessageContent(message.content).text" class="text-sm text-gray-800 dark:text-gray-100 whitespace-pre-wrap">{{ parseMessageContent(message.content).text }}</p>
                    <!-- 图片内容 -->
                    <div v-if="parseMessageContent(message.content).images.length > 0" class="mt-2 space-y-2">
                      <img 
                        v-for="(img, idx) in parseMessageContent(message.content).images" 
                        :key="idx"
                        :src="img" 
                        class="max-w-full rounded-lg cursor-pointer hover:opacity-90"
                        style="max-height: 300px;"
                        @click="previewImage(img)"
                      />
                    </div>
                  </div>
                  <span class="text-xs text-gray-400 pb-1">{{ formatTime(message.createdAt) }}</span>
                </div>
              </div>
            </div>

            <!-- 我的消息 -->
            <div v-else class="flex gap-3 mb-4 justify-end">
              <div class="max-w-[70%]">
                <div class="flex items-end gap-2 justify-end">
                  <span class="text-xs text-gray-400 pb-1">{{ formatTime(message.createdAt) }}</span>
                  <div class="px-4 py-3 bg-pink-500 text-white rounded-2xl rounded-br-md shadow-sm">
                    <!-- 文字内容 -->
                    <p v-if="parseMessageContent(message.content).text" class="text-sm whitespace-pre-wrap">{{ parseMessageContent(message.content).text }}</p>
                    <!-- 图片内容 -->
                    <div v-if="parseMessageContent(message.content).images.length > 0" class="mt-2 space-y-2">
                      <img 
                        v-for="(img, idx) in parseMessageContent(message.content).images" 
                        :key="idx"
                        :src="img" 
                        class="max-w-full rounded-lg cursor-pointer hover:opacity-90"
                        style="max-height: 300px;"
                        @click="previewImage(img)"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <n-avatar :src="getUserInfo()?.avatar" :size="36" round class="flex-shrink-0 mt-1" />
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="p-4 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
          <!-- 图片预览区域 -->
          <div v-if="pendingImages.length > 0" class="mb-3 flex flex-wrap gap-2">
            <div 
              v-for="(image, index) in pendingImages" 
              :key="index"
              class="relative group"
            >
              <img 
                :src="image.preview" 
                class="w-20 h-20 object-cover rounded-lg border border-gray-200 dark:border-gray-600"
              />
              <button
                @click="removePendingImage(index)"
                class="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
          
          <div class="flex items-end gap-3">
            <!-- 图片上传按钮 -->
            <div class="flex-shrink-0">
              <input 
                type="file" 
                accept="image/*" 
                @change="handleImageSelect"
                class="hidden"
                id="image-upload"
                :disabled="uploadingImage || sendingMessage"
              />
              <label 
                for="image-upload"
                class="w-9 h-9 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center cursor-pointer hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                :class="{ 'opacity-50 cursor-not-allowed': uploadingImage || sendingMessage }"
              >
                <svg class="w-5 h-5 text-gray-600 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </label>
            </div>
            
            <div class="flex-1">
              <textarea
                v-model="privateMessageInput"
                rows="1"
                placeholder="输入消息..."
                class="w-full px-4 py-3 bg-gray-100 dark:bg-gray-700 border-0 rounded-2xl resize-none focus:ring-2 focus:ring-pink-500 text-sm text-gray-900 dark:text-white placeholder-gray-400"
                :disabled="sendingMessage"
                @keydown.enter.exact.prevent="sendMessage"
                @input="e => { e.target.style.height = 'auto'; e.target.style.height = e.target.scrollHeight + 'px' }"
              ></textarea>
            </div>
            <n-button
              type="primary"
              :disabled="(!privateMessageInput.trim() && pendingImages.length === 0) || sendingMessage"
              :loading="sendingMessage"
              @click="sendMessage"
            >
              发送
            </n-button>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <template v-else>
        <div class="flex-1 flex items-center justify-center">
          <div class="text-center">
            <div class="w-24 h-24 mx-auto mb-6 rounded-2xl bg-white dark:bg-gray-800 shadow-sm flex items-center justify-center">
              <svg class="w-12 h-12 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
              </svg>
            </div>
            <p class="text-base text-gray-500 dark:text-gray-400">选择左侧通知查看详情</p>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 图片预览模态框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div 
          v-if="showImagePreview" 
          class="fixed inset-0 z-[9999] flex items-center justify-center bg-black bg-opacity-90"
          @click="closeImagePreview"
        >
          <!-- 关闭按钮 -->
          <button 
            @click="closeImagePreview"
            class="absolute top-4 right-4 z-10 w-10 h-10 rounded-full bg-white bg-opacity-10 hover:bg-opacity-20 text-white flex items-center justify-center transition-all"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          
          <!-- 缩放控制 -->
          <div class="absolute bottom-4 left-1/2 -translate-x-1/2 z-10 flex items-center gap-2 px-4 py-2 rounded-full bg-white bg-opacity-10 backdrop-blur-sm">
            <button 
              @click.stop="zoomOut"
              class="w-8 h-8 rounded-full hover:bg-white hover:bg-opacity-20 text-white flex items-center justify-center transition-all"
              :disabled="previewImageScale <= 0.5"
              :class="{ 'opacity-50 cursor-not-allowed': previewImageScale <= 0.5 }"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
              </svg>
            </button>
            
            <button 
              @click.stop="resetZoom"
              class="px-3 py-1 rounded-lg hover:bg-white hover:bg-opacity-20 text-white text-sm transition-all"
            >
              {{ Math.round(previewImageScale * 100) }}%
            </button>
            
            <button 
              @click.stop="zoomIn"
              class="w-8 h-8 rounded-full hover:bg-white hover:bg-opacity-20 text-white flex items-center justify-center transition-all"
              :disabled="previewImageScale >= 3"
              :class="{ 'opacity-50 cursor-not-allowed': previewImageScale >= 3 }"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </button>
          </div>
          
          <!-- 图片容器 -->
          <div 
            class="relative max-w-[90vw] max-h-[90vh] overflow-hidden"
            @click.stop
          >
            <img 
              :src="previewImageUrl" 
              :style="{ transform: `scale(${previewImageScale})`, transition: 'transform 0.3s ease' }"
              class="max-w-full max-h-[90vh] object-contain cursor-move"
              @wheel.prevent="handleWheel"
              draggable="false"
            />
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ========== 过渡动画 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ========== 滚动条样式 ========== */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.dark .overflow-y-auto::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}

.dark .overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ========== 气泡拖拽清除样式 ========== */

/* 拖拽中样式 */
.dragging .badge-container {
  cursor: grabbing;
}

/* 清除区域 */
.clear-zone {
  z-index: 100;
  animation: pulse-glow 1s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
  }
  50% {
    box-shadow: 0 0 20px rgba(239, 68, 68, 0.6), 0 0 30px rgba(249, 115, 22, 0.4);
  }
}

/* 粒子破裂动画 */
.burst-particle {
  animation: burst var(--duration) var(--delay) ease-out forwards;
}

@keyframes burst {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  100% {
    transform: 
      translate(
        calc(cos(var(--angle) * 3.14159 / 180) * var(--distance)),
        calc(sin(var(--angle) * 3.14159 / 180) * var(--distance))
      ) 
      scale(0);
    opacity: 0;
  }
}

/* 气泡缩放动画（破裂前） */
.badge-container.bursting {
  animation: badge-burst 0.3s ease-in forwards;
}

@keyframes badge-burst {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(0);
    opacity: 0;
  }
}
</style>
