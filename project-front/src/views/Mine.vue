<script setup>
import {onMounted, ref, watch} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {NModal, NPagination, NSelect} from 'naive-ui'
import {articleApi, columnApi, interactionApi, isLoggedIn, pointsApi, userApi, vipApi} from '@/utils/request'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'
import OnlineIndicator from '@/components/OnlineIndicator.vue'
import {DEFAULT_AVATAR} from '@/utils/defaults'
import {formatAbsoluteDate, formatNumber, formatRelativeDate} from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const userInfo = ref({
  id: null,
  name: '',
  avatar: '',
  bio: '',
  phone: '',
  gender: null,
  email: '',
  stats: {
    articles: 0,
    fans: 0,
    following: 0,
    likes: 0,
    collections: 0
  }
})

const tabs = [
  { name: '我的文章', key: 'articles', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
  { name: '我的专栏', key: 'columns', icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10' },
  { name: '我的收藏', key: 'collections', icon: 'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z' },
  { name: '我的点赞', key: 'likes', icon: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z' },
  { name: '浏览历史', key: 'history', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
  { name: '我的订阅', key: 'subscriptions', icon: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9' }
]

const activeTab = ref('articles')

const myArticles = ref([])
const myColumns = ref([])
const myCollections = ref([])
const myLikes = ref([])
const browseHistory = ref([])

const mySubscriptions = ref([])

const articlesLoading = ref(false)
const columnsLoading = ref(false)
const collectionsLoading = ref(false)
const likesLoading = ref(false)
const historyLoading = ref(false)
const subscriptionsLoading = ref(false)

const articlesPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })
const collectionsPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })
const likesPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })
const historyPagination = ref({ current: 1, size: 10, total: 0, pages: 0 })

const articlesOrderBy = ref('created_at')
const orderByOptions = [
  { label: '按时间排序', value: 'created_at' },
  { label: '按点赞数排序', value: 'likes' },
  { label: '按阅读数排序', value: 'views' }
]


const getGenderText = (gender) => {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '保密'
}

const maskPhone = (phone) => {
  if (!phone || phone.length < 7) return phone || '未设置'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const maskEmail = (email) => {
  if (!email) return '未设置'
  const parts = email.split('@')
  if (parts.length !== 2) return email
  const name = parts[0]
  const domain = parts[1]
  if (name.length <= 2) {
    return name[0] + '***@' + domain
  }
  return name.slice(0, 2) + '***@' + domain
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发布'
    case 2: return '已下架'
    default: return '未知'
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 0: return 'bg-gradient-to-r from-yellow-400 to-amber-400 text-white shadow-lg shadow-yellow-500/30'
    case 1: return 'bg-gradient-to-r from-green-400 to-emerald-400 text-white shadow-lg shadow-green-500/30'
    case 2: return 'bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
    default: return 'bg-gray-100 text-gray-600'
  }
}

const getReviewStatusText = (review) => {
  switch (review) {
    case 'approved': return '审核通过'
    case 'rejected': return '审核未通过'
    case 'pending': return '审核中'
    default: return ''
  }
}

const getReviewStatusClass = (review) => {
  switch (review) {
    case 'rejected': return 'bg-gradient-to-r from-red-400 to-rose-400 text-white shadow-lg shadow-red-500/30'
    case 'pending': return 'bg-gradient-to-r from-blue-400 to-cyan-400 text-white shadow-lg shadow-blue-500/30'
    default: return ''
  }
}

const fetchUserProfile = async () => {
  if (!isLoggedIn()) {
    return
  }
  
  loading.value = true
  try {
    const data = await userApi.getProfileStats()
    userInfo.value = {
      id: data.id,
      name: data.nickname || data.username,
      avatar: data.avatar || DEFAULT_AVATAR,
      bio: data.bio || '',
      phone: data.phone || '',
      gender: data.gender,
      email: data.email || '',
      stats: {
        articles: data.stats?.articleCount || 0,
        fans: data.stats?.fanCount || 0,
        following: data.stats?.followingCount || 0,
        likes: data.stats?.likeReceivedCount || 0,
        collections: data.stats?.collectionCount || 0
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchMyArticles = async () => {
  if (articlesLoading.value) return
  
  articlesLoading.value = true
  try {
    const data = await articleApi.getMyArticles(
      articlesPagination.value.current, 
      articlesPagination.value.size,
      null,
      articlesOrderBy.value
    )
    
    myArticles.value = data.records || []
    
    articlesPagination.value = {
      current: data.current || 1,
      size: data.size || 10,
      total: data.total || 0,
      pages: data.pages || 0
    }
  } catch (error) {
    console.error('获取我的文章失败:', error)
  } finally {
    articlesLoading.value = false
  }
}

const fetchMyColumns = async () => {
  if (columnsLoading.value) return

  columnsLoading.value = true
  try {
    myColumns.value = await columnApi.getMyColumns()
  } catch (error) {
    console.error('获取我的专栏失败:', error)
  } finally {
    columnsLoading.value = false
  }
}

const fetchMySubscriptions = async () => {
  if (subscriptionsLoading.value) return

  subscriptionsLoading.value = true
  try {
    mySubscriptions.value = await columnApi.getSubscriptions()
  } catch (error) {
    console.error('获取我的订阅失败:', error)
  } finally {
    subscriptionsLoading.value = false
  }
}

const fetchMyCollections = async () => {
  if (collectionsLoading.value) return
  
  collectionsLoading.value = true
  try {
    const data = await interactionApi.getMyCollections(collectionsPagination.value.current, collectionsPagination.value.size)
    
    myCollections.value = data.records || []
    
    collectionsPagination.value = {
      current: data.current || 1,
      size: data.size || 10,
      total: data.total || 0,
      pages: data.pages || 0
    }
  } catch (error) {
    console.error('获取我的收藏失败:', error)
  } finally {
    collectionsLoading.value = false
  }
}

const fetchMyLikes = async () => {
  if (likesLoading.value) return
  
  likesLoading.value = true
  try {
    const data = await interactionApi.getMyLikes(likesPagination.value.current, likesPagination.value.size)
    
    myLikes.value = data.records || []
    
    likesPagination.value = {
      current: data.current || 1,
      size: data.size || 10,
      total: data.total || 0,
      pages: data.pages || 0
    }
  } catch (error) {
    console.error('获取我的点赞失败:', error)
  } finally {
    likesLoading.value = false
  }
}

const fetchBrowseHistory = async () => {
  if (historyLoading.value) return
  
  historyLoading.value = true
  try {
    const data = await interactionApi.getBrowseHistory(historyPagination.value.current, historyPagination.value.size)
    
    browseHistory.value = data.records || []
    
    historyPagination.value = {
      current: data.current || 1,
      size: data.size || 10,
      total: data.total || 0,
      pages: data.pages || 0
    }
  } catch (error) {
    console.error('获取浏览历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

const handleArticlesPageChange = (page) => {
  articlesPagination.value.current = page
  fetchMyArticles()
}

const handleOrderByChange = () => {
  articlesPagination.value.current = 1
  fetchMyArticles()
}

const handleCollectionsPageChange = (page) => {
  collectionsPagination.value.current = page
  fetchMyCollections()
}

const handleLikesPageChange = (page) => {
  likesPagination.value.current = page
  fetchMyLikes()
}

const handleHistoryPageChange = (page) => {
  historyPagination.value.current = page
  fetchBrowseHistory()
}

const goToArticle = (articleId, isDeleted) => {
  if (isDeleted) {
    toast.error('该文章已被删除，无法查看')
    return
  }
  router.push({ name: 'Article', params: { id: articleId } })
}

const handleEditArticle = (articleId) => {
  router.push(`/create-article?articleId=${articleId}`)
}

// VIP 套餐数据
const showVipModal = ref(false)
const selectedPlan = ref(null)
const vipPlans = ref([]) // 从 API 获取的套餐列表
const plansLoading = ref(false) // 套餐加载状态

// VIP 会员信息（独立 ref，从 vipApi.getMembership 获取）
const vipInfo = ref({ isVip: false, endTime: '', vipLevel: 0 })

// 积分余额（从 pointsApi.getBalance 获取）
const pointsBalance = ref(0)

// 获取 VIP 会员信息和积分余额
const fetchVipData = async () => {
  if (!isLoggedIn()) return
  try {
    const [membership, balance] = await Promise.all([
      vipApi.getMembership(),
      pointsApi.getBalance()
    ])
    if (membership) {
      vipInfo.value = membership
    }
    // 获取可用积分余额（API返回 { totalPoints, availablePoints, todayEarned, rank }）
    pointsBalance.value = balance?.availablePoints ?? 0
  } catch (error) {
    console.error('获取VIP/积分信息失败:', error)
  }
}

// 获取 VIP 套餐列表
const fetchVipPlans = async () => {
  plansLoading.value = true
  try {
    const plans = await vipApi.getPlans()
    vipPlans.value = (plans || []).map(plan => ({
      id: plan.id,
      name: plan.planName,
      duration: `${plan.durationMonths}个月`,
      price: plan.pointsPrice,
      tag: plan.durationMonths >= 12 ? '最划算' : (plan.durationMonths >= 3 ? '' : '')
    }))
    // 默认选中第一个套餐
    if (vipPlans.value.length > 0 && !selectedPlan.value) {
      selectedPlan.value = vipPlans.value[0].id
    }
  } catch (error) {
    console.error('获取VIP套餐失败:', error)
  } finally {
    plansLoading.value = false
  }
}

const handleOpenVip = () => {
  showVipModal.value = true
  // 打开弹窗时异步加载套餐列表
  fetchVipPlans()
}

const handleConfirmVipPlan = async () => {
  if (!selectedPlan.value) return
  try {
    // 调用后端创建预订单（自动匹配最优优惠券）
    const order = await vipApi.createOrder({ planId: selectedPlan.value })
    showVipModal.value = false
    // 跳转订单确认页，携带 orderId 和积分余额
    router.push({ name: 'VipOrder', query: { orderId: order.orderId, points: pointsBalance.value } })
  } catch (error) {
    console.error('创建预订单失败:', error)
  }
}

const handleDeleteArticle = async (articleId) => {
  const confirmed = await modal.confirm('确认删除这篇文章吗？删除后不可恢复。', {
    title: '删除文章',
    confirmText: '删除',
    icon: 'error'
  })
  if (!confirmed) return
  
  try {
    await articleApi.deleteArticle(articleId)
    toast.success('删除成功')
    await fetchMyArticles()
  } catch (error) {
    console.error('删除文章失败:', error)
    toast.error(error.message || '删除失败，请稍后重试')
  }
}

watch(activeTab, (newTab) => {
  if (newTab === 'articles' && myArticles.value.length === 0) {
    fetchMyArticles()
  } else if (newTab === 'columns' && myColumns.value.length === 0) {
    fetchMyColumns()
  } else if (newTab === 'collections' && myCollections.value.length === 0) {
    fetchMyCollections()
  } else if (newTab === 'likes' && myLikes.value.length === 0) {
    fetchMyLikes()
  } else if (newTab === 'history' && browseHistory.value.length === 0) {
    fetchBrowseHistory()
  } else if (newTab === 'subscriptions' && mySubscriptions.value.length === 0) {
    fetchMySubscriptions()
  }
})

onMounted(() => {
  fetchUserProfile()
  fetchMyArticles()
  fetchVipData() // 获取VIP会员信息和积分余额
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
    <div class="bg-gradient-to-r from-primary-500 via-primary-600 to-primary-700 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px); background-size: 50px 50px;"></div>
      <div class="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
      <div class="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
      
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative">
        <div v-if="loading" class="flex justify-center py-8">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-white"></div>
        </div>
        <div v-else class="flex flex-col md:flex-row items-center md:items-start gap-6">
          <div class="relative group">
            <img 
              :src="userInfo.avatar" 
              :alt="userInfo.name"
              class="w-24 h-24 sm:w-28 sm:h-28 md:w-32 md:h-32 rounded-2xl object-cover border-4 border-white/30 shadow-2xl ring-4 ring-white/20 group-hover:scale-105 transition-transform duration-300"
            />
            <span class="absolute -bottom-1 -right-1"><OnlineIndicator v-if="userInfo.id" :userId="userInfo.id" size="md" /></span>
          </div>
          
          <div class="flex-1 text-center md:text-left">
            <div class="flex items-center gap-2 mb-2 justify-center md:justify-start">
              <h1 class="text-2xl sm:text-3xl font-bold text-white">{{ userInfo.name }}</h1>
              <!-- VIP图标 - 已开通 -->
              <div v-if="vipInfo?.isVip" class="relative group/vip">
                <div class="flex items-center gap-1 px-2.5 py-1 bg-gradient-to-r from-amber-500 to-yellow-400 rounded-full shadow-lg shadow-amber-500/30 cursor-pointer hover:scale-105 transition-transform">
                  <svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                  </svg>
                  <span class="text-xs font-bold text-white">VIP</span>
                </div>
                <!-- Hover提示 -->
                <div class="absolute top-full left-1/2 -translate-x-1/2 mt-2 px-3 py-2 bg-gray-900/95 backdrop-blur-sm text-white text-xs rounded-lg shadow-xl opacity-0 group-hover/vip:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">
                  <div class="font-medium text-amber-300">尊贵VIP会员</div>
                  <div class="text-gray-400 mt-0.5">有效期至 {{ vipInfo?.endTime?.split(' ')[0] || '未知' }}</div>
                  <div class="absolute bottom-full left-1/2 -translate-x-1/2 w-2 h-2 bg-gray-900/95 rotate-45 -mb-1"></div>
                </div>
              </div>
              <!-- 开通会员按钮 - 未开通 -->
              <button v-else @click="handleOpenVip" class="flex items-center gap-1.5 px-3 py-1.5 bg-white/20 backdrop-blur-sm border border-white/30 rounded-full text-xs font-medium text-white hover:bg-white/30 transition-all hover:scale-105">
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                </svg>
                开通会员
              </button>
            </div>
            <p v-if="userInfo.bio" class="text-white/80 mb-4 text-sm sm:text-base max-w-md">{{ userInfo.bio }}</p>
            
            <div class="flex flex-wrap justify-center md:justify-start gap-x-5 gap-y-2 text-sm text-white/70 mb-5">
              <div class="flex items-center gap-1.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <span>{{ getGenderText(userInfo.gender) }}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
                <span>{{ maskPhone(userInfo.phone) }}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
                <span>{{ maskEmail(userInfo.email) }}</span>
              </div>
            </div>

            <div class="flex flex-wrap justify-center md:justify-start gap-6 sm:gap-8">
              <div class="text-center group">
                <div class="text-2xl sm:text-3xl font-bold text-white group-hover:scale-110 transition-transform">{{ userInfo.stats.articles }}</div>
                <div class="text-white/60 text-sm">原创</div>
              </div>
              <div class="text-center group">
                <div class="text-2xl sm:text-3xl font-bold text-white group-hover:scale-110 transition-transform">{{ formatNumber(userInfo.stats.fans) }}</div>
                <div class="text-white/60 text-sm">粉丝</div>
              </div>
              <div class="text-center group">
                <div class="text-2xl sm:text-3xl font-bold text-white group-hover:scale-110 transition-transform">{{ formatNumber(userInfo.stats.following) }}</div>
                <div class="text-white/60 text-sm">关注</div>
              </div>
              <div class="text-center group">
                <div class="text-2xl sm:text-3xl font-bold text-white group-hover:scale-110 transition-transform">{{ formatNumber(userInfo.stats.likes) }}</div>
                <div class="text-white/60 text-sm">获赞</div>
              </div>
              <div class="text-center group hidden sm:block">
                <div class="text-2xl sm:text-3xl font-bold text-white group-hover:scale-110 transition-transform">{{ userInfo.stats.collections }}</div>
                <div class="text-white/60 text-sm">收藏</div>
              </div>
            </div>
          </div>

          <div class="flex gap-3 hidden md:flex">
            <RouterLink to="/profile/edit" class="px-5 py-2.5 bg-white text-primary-600 rounded-xl text-sm font-semibold hover:bg-white/90 transition-all shadow-lg hover:shadow-xl hover:-translate-y-0.5">
              编辑资料
            </RouterLink>
            <RouterLink to="/writing-tasks" class="px-5 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/30 rounded-xl text-sm font-semibold hover:bg-white/30 transition-all flex items-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
              写作任务
            </RouterLink>
            <RouterLink to="/create-article" class="px-5 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/30 rounded-xl text-sm font-semibold hover:bg-white/30 transition-all">
              写文章
            </RouterLink>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <aside class="hidden lg:block lg:col-span-4 xl:col-span-3">
          <div class="sticky top-20 space-y-6">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/20">
                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
                <div>
                  <h3 class="font-bold text-gray-900 dark:text-white">快捷入口</h3>
                  <p class="text-xs text-gray-400">常用功能</p>
                </div>
              </div>
              <div class="p-3 space-y-1">
                <RouterLink to="/mine/analytics" class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-sm font-medium">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                  数据概览
                </RouterLink>
                <RouterLink to="/mine/orders" class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-sm font-medium">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                  </svg>
                  我的订单
                </RouterLink>
                <RouterLink to="/profile/edit" class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-sm font-medium">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  账号设置
                </RouterLink>
              </div>
            </div>
          </div>
        </aside>

        <div class="lg:col-span-8 xl:col-span-9">
          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
            <div class="flex items-center flex-wrap border-b border-gray-100 dark:border-gray-700 px-2 gap-1">
              <button 
                v-for="tab in tabs" 
                :key="tab.key"
                @click="activeTab = tab.key"
                class="flex items-center gap-2 px-4 sm:px-5 py-3 text-sm font-medium whitespace-nowrap transition-all border-b-2 -mb-px rounded-t-lg"
                :class="activeTab === tab.key 
                  ? 'text-primary-500 border-primary-500 bg-primary-50/50 dark:bg-primary-900/10' 
                  : 'text-gray-500 dark:text-gray-400 border-transparent hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700/50'"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="tab.icon" />
                </svg>
                {{ tab.name }}
              </button>
            </div>

            <div class="p-4 sm:p-5">
              <!-- 我的文章 Tab -->
              <div v-if="activeTab === 'articles'" class="space-y-4">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ articlesPagination.total }} 篇文章</span>
                  <n-select 
                    v-model:value="articlesOrderBy" 
                    :options="orderByOptions" 
                    placeholder="排序方式" 
                    size="small" 
                    style="width: 140px"
                    @update:value="handleOrderByChange"
                  />
                </div>
                
                <div v-if="articlesLoading && myArticles.length === 0" class="space-y-4">
                  <div v-for="i in 3" :key="i" class="p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                    <div class="flex gap-4">
                      <div class="flex-1 space-y-3">
                        <div class="h-5 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                        <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                        <div class="flex gap-4">
                          <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                          <div class="h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div v-else-if="myArticles.length === 0" class="text-center py-12">
                  <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                    <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <p class="text-gray-500 dark:text-gray-400 font-medium">暂无文章</p>
                  <RouterLink to="/create-article" class="inline-block mt-4 px-4 py-2 bg-primary-500 text-white rounded-lg text-sm hover:bg-primary-600 transition-colors">
                    写篇文章
                  </RouterLink>
                </div>
                
                <template v-else>
                  <div 
                    v-for="article in myArticles" 
                    :key="article.id"
                    class="group p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300"
                  >
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2 mb-2">
                          <span 
                            class="px-2.5 py-1 text-xs font-semibold rounded-lg"
                            :class="getStatusClass(article.status)"
                          >
                            {{ getStatusText(article.status) }}
                          </span>
                          <span 
                            v-if="article.review && article.review !== 'approved'"
                            class="px-2.5 py-1 text-xs font-semibold rounded-lg"
                            :class="getReviewStatusClass(article.review)"
                          >
                            {{ getReviewStatusText(article.review) }}
                          </span>
                        </div>
                        <h3 class="font-semibold text-gray-900 dark:text-white mb-2 group-hover:text-primary-500 transition-colors">{{ article.title }}</h3>
                        <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400 flex-wrap">
                          <span class="flex items-center gap-1">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            {{ formatAbsoluteDate(article.createdAt) }}
                          </span>
                          <span class="flex items-center gap-1">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                            </svg>
                            {{ article.views || 0 }}
                          </span>
                          <span class="flex items-center gap-1">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                            </svg>
                            {{ article.likes || 0 }}
                          </span>
                          <span class="flex items-center gap-1 hidden sm:flex">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                            </svg>
                            {{ article.comments || 0 }}
                          </span>
                        </div>
                      </div>
                      <div class="flex items-center gap-2 self-end sm:self-center">
                        <button
                          @click="handleEditArticle(article.id)"
                          class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20 rounded-lg transition-colors font-medium"
                        >
                          编辑
                        </button>
                        <button
                          @click="handleDeleteArticle(article.id)"
                          class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors font-medium"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  </div>
                  <div class="flex justify-center pt-4">
                    <n-pagination
                      v-model:page="articlesPagination.current"
                      :page-size="articlesPagination.size"
                      :item-count="articlesPagination.total"
                      @update:page="handleArticlesPageChange"
                    />
                  </div>
                </template>
            </div>

            <!-- 我的专栏 Tab -->
            <div v-else-if="activeTab === 'columns'" class="space-y-4">
              <div class="flex items-center justify-between mb-4">
                <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ myColumns.length }} 个专栏</span>
                <RouterLink to="/my-columns" class="px-4 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-lg text-sm font-medium hover:from-purple-600 hover:to-pink-600 transition-all shadow-md flex items-center gap-2">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  管理专栏
                </RouterLink>
              </div>

              <div v-if="columnsLoading && myColumns.length === 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div v-for="i in 2" :key="i" class="p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                  <div class="flex gap-4">
                    <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                    <div class="flex-1 space-y-3">
                      <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="myColumns.length === 0" class="text-center py-12">
                <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center">
                  <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                </div>
                <p class="text-gray-500 dark:text-gray-400 font-medium">暂无专栏</p>
                <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">创建你的第一个专栏，开始整理你的知识体系</p>
                <RouterLink to="/my-columns" class="inline-block mt-4 px-4 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-lg text-sm hover:from-purple-600 hover:to-pink-600 transition-all shadow-lg">
                  创建专栏
                </RouterLink>
              </div>

              <template v-else>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div
                    v-for="column in myColumns"
                    :key="column.id"
                    @click="router.push({ name: 'ColumnDetail', params: { id: column.id } })"
                    class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-purple-200 dark:hover:border-purple-800 transition-all duration-300 cursor-pointer"
                  >
                    <div
                      v-if="column.cover"
                      class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                    >
                      <img :src="column.cover" :alt="column.title" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                    <div
                      v-else
                      class="flex-shrink-0 w-24 h-24 rounded-xl bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center"
                    >
                      <svg class="w-10 h-10 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-2">
                        <span
                          class="px-2.5 py-1 text-xs font-semibold rounded-lg"
                          :class="column.status === 1 ? 'bg-gradient-to-r from-green-400 to-emerald-400 text-white shadow-lg shadow-green-500/30' : 'bg-gradient-to-r from-yellow-400 to-amber-400 text-white shadow-lg shadow-yellow-500/30'"
                        >
                          {{ column.status === 1 ? '已发布' : '草稿' }}
                        </span>
                      </div>
                      <h3 class="font-semibold text-gray-900 dark:text-white mb-1 group-hover:text-purple-500 transition-colors line-clamp-1">
                        {{ column.title }}
                      </h3>
                      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1 mb-2">
                        {{ column.description || '暂无描述' }}
                      </p>
                      <div class="flex items-center gap-3 text-xs text-gray-400">
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                          {{ column.articlesCount }}篇
                        </span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                          {{ formatNumber(column.views) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>

            <!-- 我的订阅 Tab -->
            <div v-else-if="activeTab === 'subscriptions'" class="space-y-4">
              <div class="flex items-center justify-between mb-4">
                <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ mySubscriptions.length }} 个订阅</span>
              </div>

              <div v-if="subscriptionsLoading && mySubscriptions.length === 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div v-for="i in 2" :key="i" class="p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                  <div class="flex gap-4">
                    <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                    <div class="flex-1 space-y-3">
                      <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="mySubscriptions.length === 0" class="text-center py-12">
                <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center">
                  <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                  </svg>
                </div>
                <p class="text-gray-500 dark:text-gray-400 font-medium">暂无订阅</p>
                <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">去发现感兴趣的专栏吧</p>
                <RouterLink to="/columns" class="inline-block mt-4 px-4 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-lg text-sm hover:from-orange-600 hover:to-red-600 transition-all shadow-lg">
                  浏览专栏
                </RouterLink>
              </div>

              <template v-else>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div
                    v-for="sub in mySubscriptions"
                    :key="sub.columnId"
                    @click="router.push({ name: 'ColumnDetail', params: { id: sub.columnId } })"
                    class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-orange-200 dark:hover:border-orange-800 transition-all duration-300 cursor-pointer"
                  >
                    <div
                      v-if="sub.columnCover"
                      class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                    >
                      <img :src="sub.columnCover" :alt="sub.columnTitle" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                    <div
                      v-else
                      class="flex-shrink-0 w-24 h-24 rounded-xl bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center"
                    >
                      <svg class="w-10 h-10 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold text-gray-900 dark:text-white mb-1 group-hover:text-orange-500 transition-colors line-clamp-1">
                        {{ sub.columnTitle }}
                      </h3>
                      <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1 mb-2">
                        {{ sub.authorName }}
                      </p>
                      <div class="flex items-center gap-3 text-xs text-gray-400">
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                          {{ sub.articlesCount }}篇
                        </span>
                        <span class="flex items-center gap-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                          </svg>
                          {{ formatAbsoluteDate(sub.subscribedAt) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>

            <!-- 我的收藏 Tab -->
              <div v-else-if="activeTab === 'collections'" class="space-y-4">
                <div v-if="collectionsLoading && myCollections.length === 0" class="space-y-4">
                  <div v-for="i in 3" :key="i" class="flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                    <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                    <div class="flex-1 space-y-3">
                      <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                      <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    </div>
                  </div>
                </div>
                
                <div v-else-if="myCollections.length === 0" class="text-center py-12">
                  <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-primary-400 to-orange-400 flex items-center justify-center">
                    <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                    </svg>
                  </div>
                  <p class="text-gray-500 dark:text-gray-400 font-medium">暂无收藏</p>
                  <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">收藏的文章将在这里显示</p>
                </div>
                
                <template v-else>
                  <div 
                    v-for="item in myCollections" 
                    :key="item.id"
                    @click="goToArticle(item.articleId, item.isDeleted)"
                    class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300 cursor-pointer"
                    :class="{ 'opacity-60': item.isDeleted }"
                  >
                    <div 
                      v-if="item.cover && !item.isDeleted"
                      class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                    >
                      <img :src="item.cover" :alt="item.title" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                    <div 
                      v-else
                      class="flex-shrink-0 w-24 h-24 rounded-xl flex items-center justify-center"
                      :class="item.isDeleted ? 'bg-gray-200 dark:bg-gray-700' : 'bg-gradient-to-br from-primary-400 to-orange-400'"
                    >
                      <svg v-if="item.isDeleted" class="w-10 h-10 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      <svg v-else class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold mb-1 transition-colors line-clamp-1" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-900 dark:text-white group-hover:text-primary-500'">
                        {{ item.isDeleted ? '已删除文章' : item.title }}
                      </h3>
                      <p class="text-sm line-clamp-2 mb-2" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                        {{ item.isDeleted ? '该文章已被删除，无法查看' : (item.summary || '暂无摘要') }}
                      </p>
                      <div class="flex items-center justify-between flex-wrap gap-2">
                        <div class="flex items-center gap-2">
                          <img 
                            v-if="!item.isDeleted"
                            :src="item.authorAvatar || DEFAULT_AVATAR" 
                            :alt="item.authorName"
                            class="w-5 h-5 rounded-full object-cover"
                          />
                          <span class="text-xs" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                            {{ item.isDeleted ? '文章已不存在' : (item.authorName || '未知作者') }}
                          </span>
                        </div>
                        <div class="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
                          <span v-if="!item.isDeleted">{{ formatAbsoluteDate(item.createdAt) }}</span>
                          <span v-if="!item.isDeleted">·</span>
                          <span>收藏于 {{ formatRelativeDate(item.collectedAt) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="flex justify-center pt-4">
                    <n-pagination
                      v-model:page="collectionsPagination.current"
                      :page-size="collectionsPagination.size"
                      :item-count="collectionsPagination.total"
                      @update:page="handleCollectionsPageChange"
                    />
                  </div>
                </template>
              </div>

              <!-- 我的点赞 Tab -->
              <div v-else-if="activeTab === 'likes'" class="space-y-4">
                <div v-if="likesLoading && myLikes.length === 0" class="space-y-4">
                  <div v-for="i in 3" :key="i" class="flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                    <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                    <div class="flex-1 space-y-3">
                      <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                      <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    </div>
                  </div>
                </div>
                
                <div v-else-if="myLikes.length === 0" class="text-center py-12">
                  <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-red-400 to-pink-400 flex items-center justify-center">
                    <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                  </div>
                  <p class="text-gray-500 dark:text-gray-400 font-medium">暂无点赞</p>
                  <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">点赞的文章将在这里显示</p>
                </div>
                
                <template v-else>
                  <div 
                    v-for="item in myLikes" 
                    :key="item.id"
                    @click="goToArticle(item.articleId, item.isDeleted)"
                    class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300 cursor-pointer"
                    :class="{ 'opacity-60': item.isDeleted }"
                  >
                    <div 
                      v-if="item.cover && !item.isDeleted"
                      class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                    >
                      <img :src="item.cover" :alt="item.title" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                    <div 
                      v-else
                      class="flex-shrink-0 w-24 h-24 rounded-xl flex items-center justify-center"
                      :class="item.isDeleted ? 'bg-gray-200 dark:bg-gray-700' : 'bg-gradient-to-br from-red-400 to-pink-400'"
                    >
                      <svg v-if="item.isDeleted" class="w-10 h-10 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      <svg v-else class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold mb-1 transition-colors line-clamp-1" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-900 dark:text-white group-hover:text-primary-500'">
                        {{ item.isDeleted ? '已删除文章' : item.title }}
                      </h3>
                      <p class="text-sm line-clamp-2 mb-2" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                        {{ item.isDeleted ? '该文章已被删除，无法查看' : (item.summary || '暂无摘要') }}
                      </p>
                      <div class="flex items-center justify-between flex-wrap gap-2">
                        <div class="flex items-center gap-2">
                          <img 
                            v-if="!item.isDeleted"
                            :src="item.authorAvatar || DEFAULT_AVATAR" 
                            :alt="item.authorName"
                            class="w-5 h-5 rounded-full object-cover"
                          />
                          <span class="text-xs" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                            {{ item.isDeleted ? '文章已不存在' : (item.authorName || '未知作者') }}
                          </span>
                        </div>
                        <div class="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
                          <span v-if="!item.isDeleted">{{ formatAbsoluteDate(item.createdAt) }}</span>
                          <span v-if="!item.isDeleted">·</span>
                          <span>点赞于 {{ formatRelativeDate(item.likedAt) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="flex justify-center pt-4">
                    <n-pagination
                      v-model:page="likesPagination.current"
                      :page-size="likesPagination.size"
                      :item-count="likesPagination.total"
                      @update:page="handleLikesPageChange"
                    />
                  </div>
                </template>
              </div>

              <!-- 浏览历史 Tab -->
              <div v-else-if="activeTab === 'history'" class="space-y-4">
                <div v-if="historyLoading && browseHistory.length === 0" class="space-y-4">
                  <div v-for="i in 3" :key="i" class="flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl">
                    <div class="w-24 h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse flex-shrink-0"></div>
                    <div class="flex-1 space-y-3">
                      <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-3/4"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse w-full"></div>
                      <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
                    </div>
                  </div>
                </div>
                
                <div v-else-if="browseHistory.length === 0" class="text-center py-12">
                  <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-blue-400 to-cyan-400 flex items-center justify-center">
                    <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <p class="text-gray-500 dark:text-gray-400 font-medium">暂无浏览历史</p>
                  <p class="text-sm text-gray-400 dark:text-gray-500 mt-2">您浏览过的文章将在这里显示</p>
                </div>
                
                <template v-else>
                  <div 
                    v-for="(item, index) in browseHistory" 
                    :key="item.articleId || index"
                    @click="goToArticle(item.articleId, item.isDeleted)"
                    class="group flex gap-4 p-4 border border-gray-100 dark:border-gray-700 rounded-xl hover:shadow-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300 cursor-pointer"
                    :class="{ 'opacity-60': item.isDeleted }"
                  >
                    <div 
                      v-if="item.articleCover && !item.isDeleted"
                      class="flex-shrink-0 w-24 h-24 rounded-xl overflow-hidden"
                    >
                      <img :src="item.articleCover" :alt="item.articleTitle" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300" />
                    </div>
                    <div 
                      v-else
                      class="flex-shrink-0 w-24 h-24 rounded-xl flex items-center justify-center"
                      :class="item.isDeleted ? 'bg-gray-200 dark:bg-gray-700' : 'bg-gradient-to-br from-blue-400 to-cyan-400'"
                    >
                      <svg v-if="item.isDeleted" class="w-10 h-10 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                      <svg v-else class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold mb-1 transition-colors line-clamp-1" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-900 dark:text-white group-hover:text-primary-500'">
                        {{ item.isDeleted ? '已删除文章' : item.articleTitle }}
                      </h3>
                      <p class="text-sm line-clamp-2 mb-2" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                        {{ item.isDeleted ? '该文章已被删除，无法查看' : (item.summary || '暂无摘要') }}
                      </p>
                      <div class="flex items-center justify-between flex-wrap gap-2">
                        <div class="flex items-center gap-2">
                          <img 
                            v-if="!item.isDeleted"
                            :src="item.authorAvatar || DEFAULT_AVATAR" 
                            :alt="item.authorName"
                            class="w-5 h-5 rounded-full object-cover"
                          />
                          <span class="text-xs" :class="item.isDeleted ? 'text-gray-400 dark:text-gray-500' : 'text-gray-500 dark:text-gray-400'">
                            {{ item.isDeleted ? '文章已不存在' : (item.authorName || '未知作者') }}
                          </span>
                        </div>
                        <div class="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
                          <span v-if="!item.isDeleted">{{ formatAbsoluteDate(item.createdAt) }}</span>
                          <span v-if="!item.isDeleted">·</span>
                          <span>浏览于 {{ formatRelativeDate(item.browseTime) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="flex justify-center pt-4">
                    <n-pagination
                      v-model:page="historyPagination.current"
                      :page-size="historyPagination.size"
                      :item-count="historyPagination.total"
                      @update:page="handleHistoryPageChange"
                    />
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VIP 套餐选择弹窗 -->
    <n-modal v-model:show="showVipModal" :mask-closable="true">
      <div class="w-[720px] max-w-[90vw] bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-2xl">
        <!-- 弹窗头部 -->
        <div class="bg-gradient-to-r from-amber-500 via-yellow-500 to-amber-400 px-8 py-6 relative overflow-hidden">
          <div class="absolute top-0 right-0 w-40 h-40 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2"></div>
          <div class="absolute bottom-0 left-0 w-24 h-24 bg-white/10 rounded-full translate-y-1/2 -translate-x-1/2"></div>
          <div class="relative flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
                <svg class="w-7 h-7 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                </svg>
              </div>
              <div>
                <h3 class="text-xl font-bold text-white">开通 VIP 会员</h3>
                <p class="text-white/80 text-sm mt-0.5">尊享专属特权，提升创作体验</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="text-right">
                <p class="text-white/70 text-xs">当前积分</p>
                <p class="text-white text-lg font-bold">{{ pointsBalance }}</p>
              </div>
              <button @click="showVipModal = false" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/20 transition-colors">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 套餐卡片列表：加载中显示骨架屏，加载完成显示真实数据 -->
        <div class="p-6 grid grid-cols-3 gap-4">
          <!-- 骨架屏加载动画 -->
          <template v-if="plansLoading">
            <div v-for="i in 3" :key="i"
              class="relative flex flex-col items-center p-5 rounded-2xl border-2 border-gray-200 dark:border-gray-600 animate-pulse">
              <div class="w-14 h-14 rounded-2xl bg-gray-200 dark:bg-gray-600 mb-3"></div>
              <div class="h-4 w-16 bg-gray-200 dark:bg-gray-600 rounded mb-1"></div>
              <div class="h-3 w-12 bg-gray-200 dark:bg-gray-600 rounded mb-4"></div>
              <div class="h-7 w-20 bg-gray-200 dark:bg-gray-600 rounded"></div>
            </div>
          </template>
          <!-- 真实套餐数据 -->
          <template v-else>
          <div
            v-for="plan in vipPlans"
            :key="plan.id"
            @click="selectedPlan = plan.id"
            class="relative flex flex-col items-center p-5 rounded-2xl border-2 cursor-pointer transition-all duration-200 group"
            :class="selectedPlan === plan.id
              ? 'border-amber-400 bg-gradient-to-b from-amber-50 to-yellow-50 dark:from-amber-900/30 dark:to-yellow-900/20 shadow-lg shadow-amber-500/15 scale-[1.02]'
              : 'border-gray-200 dark:border-gray-600 hover:border-amber-300 dark:hover:border-amber-600 hover:shadow-md'"
          >
            <!-- 标签 -->
            <div v-if="plan.tag" class="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 text-xs font-bold text-white bg-gradient-to-r from-red-500 to-orange-500 rounded-full shadow-md whitespace-nowrap">
              {{ plan.tag }}
            </div>

            <!-- 图标 -->
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-3 transition-all"
              :class="selectedPlan === plan.id
                ? 'bg-gradient-to-br from-amber-400 to-yellow-500 shadow-lg shadow-amber-500/30'
                : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-amber-100 dark:group-hover:bg-amber-900/30'"
            >
              <svg class="w-7 h-7 transition-colors"
                :class="selectedPlan === plan.id ? 'text-white' : 'text-gray-400 dark:text-gray-500 group-hover:text-amber-500'"
                viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
              </svg>
            </div>

            <!-- 套餐名 -->
            <span class="text-base font-bold text-gray-900 dark:text-white mb-1">{{ plan.name }}</span>
            <span class="text-xs text-gray-500 dark:text-gray-400 mb-4">{{ plan.duration }}</span>

            <!-- 价格 -->
            <div class="text-center">
              <div class="flex items-baseline justify-center gap-0.5">
                <span class="text-2xl font-bold transition-colors"
                  :class="selectedPlan === plan.id ? 'text-amber-600 dark:text-amber-400' : 'text-gray-900 dark:text-white'">
                  {{ plan.price }}
                </span>
                <span class="text-xs text-gray-500 dark:text-gray-400">积分</span>
              </div>
            </div>

            <!-- 选中指示器 -->
            <div v-if="selectedPlan === plan.id" class="absolute top-3 right-3 w-5 h-5 rounded-full bg-amber-500 flex items-center justify-center">
              <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
              </svg>
            </div>
          </div>
          </template>
        </div>

        <!-- 底部操作 -->
        <div class="px-6 pb-6 flex gap-3">
          <button
            @click="showVipModal = false"
            class="flex-1 py-3 text-sm font-medium text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 rounded-xl hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleConfirmVipPlan"
            :disabled="plansLoading || !selectedPlan"
            class="flex-1 py-3 text-sm font-bold text-white bg-gradient-to-r from-amber-500 to-yellow-500 rounded-xl hover:from-amber-600 hover:to-yellow-600 transition-all shadow-lg shadow-amber-500/25 hover:shadow-amber-500/40 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ plansLoading ? '加载中...' : '确认选择' }}
          </button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
