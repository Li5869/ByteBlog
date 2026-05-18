<script setup>
import {useRoute, useRouter} from 'vue-router'
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {marked} from 'marked'
import {articleApi, commentApi, interactionApi, isLoggedIn, userApi} from '../utils/request.js'
import {useUserStore} from '../stores/user.js'
import {toast} from '@/utils/toast'
import {modal} from '@/utils/modal'
import {DEFAULT_AVATAR, DEFAULT_COVER} from '@/utils/defaults'
import {formatAbsoluteDate, formatNumber} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const articleId = computed(() => route.params.id)

const commentId = computed(() => route.query.commentId ? String(route.query.commentId) : null)

const isAuthor = computed(() => {
  if (!article.value || !userStore.state.userInfo) return false
  return article.value.authorId === userStore.state.userInfo.id
})

const loading = ref(true)
const error = ref(null)
const article = ref(null)
const authorInfo = ref(null)
const relatedArticles = ref([])
const hotArticles = ref([])
const randomArticles = ref([])
const hotArticlesLoading = ref(true)
const randomArticlesLoading = ref(true)
const comments = ref([])
const commentTotal = ref(0)
const commentCurrent = ref(1)
const commentSize = ref(10)
const followStatusLoaded = ref(false) // 关注状态是否已确认加载完成

const newComment = ref('')
const showComments = ref(false)
const expandedReplies = ref({})
const activeHeadingId = ref('')
const articleContentRef = ref(null)
const replyingTo = ref(null)
const replyContent = ref({})
const showFullContent = ref(false)

const fetchArticleData = async () => {
  try {
    loading.value = true
    error.value = null
    
    // 并行请求：基础信息（含正文，可缓存） + 互动数据（实时）
    const [metadata, interaction] = await Promise.all([
      articleApi.getArticleMetadata(articleId.value),
      articleApi.getArticleInteraction(articleId.value)
    ])
    
    // 合并互动数据到文章对象
    article.value = { ...metadata, ...interaction }
    
    fetchRelatedArticles()
    if (metadata?.authorId) {
      fetchAuthorInfo(metadata.authorId)
    }
  } catch (e) {
    error.value = e.message || '加载文章失败'
    console.error('加载文章失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchAuthorInfo = async (userId) => {
  try {
    const data = await userApi.getAuthorInfo(userId)
    authorInfo.value = data
    // 初始化关注状态，避免闪烁
    if (!isAuthor.value) {
      authorInfo.value.isFollowed = false
    }
    
    if (isLoggedIn() && data?.id && !isAuthor.value) {
      try {
        const followedIds = await interactionApi.checkBatchFollowStatus([data.id])
        authorInfo.value.isFollowed = followedIds && followedIds.length > 0
      } catch (e) {
        console.error('获取关注状态失败:', e)
        authorInfo.value.isFollowed = false
      } finally {
        // 关注状态检查完成，标记已加载
        followStatusLoaded.value = true
      }
    } else {
      // 非登录状态或作者本人，不需要检查关注状态
      followStatusLoaded.value = true
    }
  } catch (e) {
    console.error('加载作者信息失败:', e)
    followStatusLoaded.value = true // 出错时也标记已加载，避免按钮不显示
  }
}

const fetchSidebarData = async () => {
  hotArticlesLoading.value = true
  randomArticlesLoading.value = true
  try {
    const [hotData, randomData] = await Promise.all([
      articleApi.getHotArticles(3).finally(() => { hotArticlesLoading.value = false }),
      articleApi.getRandomArticles(4).finally(() => { randomArticlesLoading.value = false })
    ])
    hotArticles.value = hotData || []
    randomArticles.value = randomData || []
  } catch (e) {
    console.error('加载侧边栏数据失败:', e)
  }
}

const fetchRelatedArticles = async () => {
  if (!articleId.value) return
  try {
    const data = await articleApi.getRelatedArticles(articleId.value, 3)
    relatedArticles.value = data || []
  } catch (e) {
    console.error('加载相关文章失败:', e)
    relatedArticles.value = []
  }
}

const fetchComments = async (page = 1) => {
  if (!articleId.value) return
  try {
    const data = await commentApi.getCommentPage(articleId.value, page, commentSize.value)
    comments.value = data.records || []
    commentTotal.value = data.total || 0
    commentCurrent.value = data.current || 1
    if (comments.value.length > 0 && isLoggedIn()) {
      await fetchCommentLikes()
    }
  } catch (e) {
    console.error('加载评论失败:', e)
    comments.value = []
  }
}

const fetchCommentLikes = async () => {
  if (!comments.value.length) return
  try {
    // 收集所有评论 ID，包括父评论和回复
    const allCommentIds = []
    comments.value.forEach(comment => {
      allCommentIds.push(comment.id)
      if (comment.replies && comment.replies.length > 0) {
        comment.replies.forEach(reply => {
          allCommentIds.push(reply.id)
        })
      }
    })
    const likedIds = await commentApi.getIsLikes(allCommentIds, 'comment')
    const likedSet = new Set(likedIds)
    // 设置父评论的点赞状态
    comments.value.forEach(comment => {
      comment.isLiked = likedSet.has(comment.id)
      // 设置回复的点赞状态
      if (comment.replies && comment.replies.length > 0) {
        comment.replies.forEach(reply => {
          reply.isLiked = likedSet.has(reply.id)
        })
      }
    })
  } catch (e) {
    console.error('获取评论点赞状态失败:', e)
  }
}

const extractHeadings = (content) => {
  if (!content) return []
  const headings = []
  const lines = content.split('\n')
  let inCodeBlock = false
  let index = 0

  for (const line of lines) {
    // 遇到 ``` 开头的行切换代码块状态（支持 ```、```java 等）
    if (line.trimStart().startsWith('```')) {
      inCodeBlock = !inCodeBlock
      continue
    }
    if (inCodeBlock) continue

    const match = line.match(/^(#{1,6})\s+(.+)$/)
    if (match) {
      const level = match[1].length
      const text = match[2].trim()
      headings.push({ level, text, id: `heading-${index}` })
      index++
    }
  }

  return headings
}

const headings = computed(() => {
  if (!article.value?.content) return []
  return extractHeadings(article.value.content)
})

const renderedContent = computed(() => {
  if (!article.value?.content) return ''

  let index = 0
  const renderer = new marked.Renderer()

  renderer.heading = function({ text, depth }) {
    const id = `heading-${index}`
    index++
    return `<h${depth} id="${id}">${text}</h${depth}>`
  }

  return marked(article.value.content, { renderer })
})

// 基于纯文本字符数判断是否长文章（比 HTML 长度更准确）
const isLongArticle = computed(() => {
  if (!article.value?.content) return false
  // 统计纯文本字符数（去掉 Markdown 语法符号）
  const plainText = article.value.content
    .replace(/#{1,6}\s/g, '') // 去掉标题符号
    .replace(/\*\*/g, '')      // 去掉粗体
    .replace(/\*/g, '')         // 去掉斜体
    .replace(/`{1,3}[^`]*`{1,3}/g, '') // 去掉代码
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1') // 保留链接文字
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '') // 去掉图片
    .replace(/\n+/g, ' ')       // 换行变空格
    .trim()
  return plainText.length > 1200 // 纯文本超过1200字符才折叠
})

const scrollToHeading = (id) => {
  const element = document.getElementById(id)
  if (element) {
    // 导航栏 sticky h-14 (56px)，留 4px 间距
    const navbarHeight = 60
    const top = element.getBoundingClientRect().top + window.scrollY - navbarHeight
    window.scrollTo({ top, behavior: 'smooth' })
    activeHeadingId.value = id
  }
}

const updateActiveHeading = () => {
  const headingElements = headings.value.map(h => ({
    id: h.id,
    element: document.getElementById(h.id)
  })).filter(h => h.element)

  if (headingElements.length === 0) return

  // 导航栏下方 70px 作为判定阈值
  const navbarBottom = 70
  let currentId = headingElements[0].id

  // 从下往上找第一个已经滚过导航栏的标题
  for (let i = headingElements.length - 1; i >= 0; i--) {
    const rect = headingElements[i].element.getBoundingClientRect()
    if (rect.top <= navbarBottom) {
      currentId = headingElements[i].id
      break
    }
  }

  activeHeadingId.value = currentId
}

const handleToggleLike = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  try {
    const newIsLike = !article.value.isLiked
    const data = await interactionApi.toggleLike(
      article.value.id, 
      'article', 
      newIsLike, 
      authorInfo.value?.id,
      article.value.title,
      article.value.summary
    )
    article.value.isLiked = newIsLike
    article.value.totalLikes = data.likes
  } catch (e) {
    console.error('点赞失败:', e)
  }
}

const handleToggleCollect = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  try {
    const result = await interactionApi.toggleCollect(
      article.value.id, 
      article.value.isCollected,
      authorInfo.value?.id
    )
    article.value.isCollected = !article.value.isCollected
    article.value.collections = result.collectionTimes
  } catch (e) {
    console.error('收藏失败:', e)
    toast.error(e.message || '操作失败，请稍后重试')
  }
}

const handleToggleFollow = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  try {
    const result = await interactionApi.toggleFollow(
      authorInfo.value.id, 
      authorInfo.value.isFollowed
    )
    authorInfo.value.isFollowed = result.isFollow
  } catch (e) {
    console.error('关注失败:', e)
    toast.error(e.message || '操作失败，请稍后重试')
  }
}

const handleSendMessage = () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  if (!authorInfo.value?.id) {
    toast.error('用户信息不存在')
    return
  }
  router.push({
    path: '/notifications',
    query: { userId: authorInfo.value.id }
  })
}

const toggleComments = async () => {
  showComments.value = !showComments.value
  if (showComments.value && comments.value.length === 0) {
    await fetchComments()
  }
}

const scrollToComment = async (targetCommentId) => {
  if (!targetCommentId) return
  
  // 确保评论区展开
  showComments.value = true
  await new Promise(resolve => setTimeout(resolve, 100))
  
  // 如果评论列表为空，先加载
  if (comments.value.length === 0) {
    await fetchComments()
    await new Promise(resolve => setTimeout(resolve, 300))
  }
  
  // 如果目标评论是某条父评论下的回复，且该回复在折叠区域，先展开
  for (const comment of comments.value) {
    if (comment.replies) {
      const targetReply = comment.replies.find(r => String(r.id) === String(targetCommentId))
      if (targetReply && !expandedReplies.value[comment.id]) {
        expandedReplies.value[comment.id] = true
        await new Promise(resolve => setTimeout(resolve, 100))
      }
    }
  }
  
  // 查找目标评论（先在父评论中找，再在回复中找）
  let commentElement = document.querySelector(`[data-comment-id="${targetCommentId}"]`)
  
  // 如果第1页没找到，尝试翻页搜索（最多翻5页）
  let searchPage = commentCurrent.value + 1
  const maxSearchPages = 5
  while (!commentElement && searchPage <= maxSearchPages && searchPage * commentSize.value < commentTotal.value) {
    await fetchComments(searchPage)
    await new Promise(resolve => setTimeout(resolve, 200))
    // 翻页后也检查折叠
    for (const comment of comments.value) {
      if (comment.replies) {
        const targetReply = comment.replies.find(r => String(r.id) === String(targetCommentId))
        if (targetReply && !expandedReplies.value[comment.id]) {
          expandedReplies.value[comment.id] = true
          await new Promise(resolve => setTimeout(resolve, 100))
        }
      }
    }
    commentElement = document.querySelector(`[data-comment-id="${targetCommentId}"]`)
    searchPage++
  }
  
  if (commentElement) {
    const offset = 100
    const elementPosition = commentElement.getBoundingClientRect().top
    const offsetPosition = elementPosition + window.pageYOffset - offset
    
    window.scrollTo({
      top: offsetPosition,
      behavior: 'smooth'
    })
    
    commentElement.classList.add('highlight-comment')
    setTimeout(() => {
      commentElement.classList.remove('highlight-comment')
    }, 3000)
  }
}

const handleToggleCommentLike = async (comment) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  try {
    const newIsLike = !comment.isLiked
    const data = await interactionApi.toggleLike(
      comment.id, 
      'comment', 
      newIsLike, 
      comment.author?.id,
      article.value.title,
      comment.content,
      articleId.value
    )
    comment.isLiked = newIsLike
    comment.likes = data.likes
  } catch (e) {
    console.error('点赞评论失败:', e)
  }
}

const submitComment = async () => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  if (!newComment.value.trim()) {
    toast.error('评论内容不能为空')
    return
  }
  
  if (newComment.value.length > 500) {
    toast.error('评论内容不能超过500字')
    return
  }
  
  try {
    const data = await commentApi.postComment({
      articleId: articleId.value,
      content: newComment.value.trim(),
      parentId: null,
      articleTitle: article.value?.title || '',
      totalComment: article.value?.comments || 0
    })
    
    // 将新评论添加到列表顶部
    comments.value.unshift(data)
    
    // 更新文章评论数
    if (article.value) {
      article.value.comments = data.commentTotal
    }
    
    // 清空输入框
    newComment.value = ''
    toast.success('评论发表成功')
  } catch (e) {
    console.error('发表评论失败:', e)
    toast.error(e.message || '发表评论失败，请稍后重试')
  }
}

const deleteComment = async (commentId) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  const confirmed = await modal.confirm('确定要删除这条评论吗？此操作不可恢复。', {
    title: '删除评论',
    confirmText: '删除',
    icon: 'error'
  })
  if (!confirmed) return
  
  try {
    const result = await commentApi.deleteComment(commentId)
    
    // 从列表中移除评论（包括子评论）
    const index = comments.value.findIndex(c => c.id === commentId)
    if (index !== -1) {
      comments.value.splice(index, 1)
    }
    
    // 计算新的评论总数
    const deleteTotal = result.deleteTotal || 1
    if (article.value) {
      article.value.comments = Math.max(0, (article.value.comments || 0) - deleteTotal)
    }
    
    toast.success('评论删除成功')
  } catch (e) {
    console.error('删除评论失败:', e)
    toast.error(e.message || '删除评论失败，请稍后重试')
  }
}

const toggleReply = (commentId) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  if (replyingTo.value === commentId) {
    replyingTo.value = null
  } else {
    replyingTo.value = commentId
    replyContent.value[commentId] = ''
  }
}

const submitReply = async (commentId) => {
  if (!isLoggedIn()) {
    toast.error('请先登录')
    return
  }
  
  const content = replyContent.value[commentId]?.trim()
  if (!content) {
    toast.error('回复内容不能为空')
    return
  }
  
  if (content.length > 500) {
    toast.error('回复内容不能超过500字')
    return
  }
  
  try {
    const data = await commentApi.postComment({
      articleId: articleId.value,
      content: content,
      parentId: commentId,
      articleTitle: article.value?.title || '',
      totalComment: article.value?.comments || 0
    })
    
    // 找到父评论并添加回复
    const parentComment = comments.value.find(c => c.id === commentId)
    if (parentComment) {
      if (!parentComment.replies) {
        parentComment.replies = []
      }
      parentComment.replies.push(data)
    }
    
    // 更新文章评论数
    if (article.value) {
      article.value.comments = data.commentTotal
    }
    
    // 清空并关闭回复框
    replyContent.value[commentId] = ''
    replyingTo.value = null
    
    toast.success('回复发表成功')
  } catch (e) {
    console.error('发表回复失败:', e)
    toast.error(e.message || '发表回复失败，请稍后重试')
  }
}

const cancelReply = () => {
  if (replyingTo.value) {
    replyContent.value[replyingTo.value] = ''
  }
  replyingTo.value = null
}

const goToUserHome = (userId) => {
  const currentUserId = userStore.state.userInfo?.id
  if (currentUserId && String(userId) === String(currentUserId)) {
    router.push('/mine')
  } else {
    router.push(`/user/${userId}`)
  }
}


watch(articleId, async () => {
  if (articleId.value) {
    // 切换文章时重置展开状态
    showFullContent.value = false
    await fetchArticleData()
    // 切换文章后如果有 commentId，重新定位
    if (commentId.value) {
      await scrollToComment(commentId.value)
    }
  }
})

// 监听 commentId 变化（从通知页跳转时 query 参数变化不会触发 onMounted）
watch(commentId, (newVal) => {
  if (newVal) {
    scrollToComment(newVal)
  }
})

onMounted(async () => {
  // 并行发起：主文章 + 侧边栏同时加载，不互相等待
  const articlePromise = fetchArticleData()
  fetchSidebarData()

  await articlePromise
  window.addEventListener('scroll', updateActiveHeading)
  updateActiveHeading()

  if (commentId.value) {
    await scrollToComment(commentId.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateActiveHeading)
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800 relative">
    <div v-if="loading" class="flex items-center justify-center min-h-screen">
      <div class="flex flex-col items-center gap-4">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
        <div class="text-gray-500 dark:text-gray-400 font-medium">加载中...</div>
      </div>
    </div>
    
    <div v-else-if="error" class="flex items-center justify-center min-h-screen">
      <div class="text-center">
        <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
          <svg class="w-10 h-10 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div class="text-red-500 font-medium">{{ error }}</div>
      </div>
    </div>
    
    <div v-else-if="article" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6">
      <div class="flex flex-col lg:flex-row gap-4 sm:gap-6">
        <div class="lg:w-64 flex-shrink-0 hidden lg:block">
          <div class="sticky top-20 space-y-4 sm:space-y-6">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="p-4 sm:p-5">
                <div class="flex items-center gap-3 mb-4">
                  <img 
                    :src="authorInfo?.avatar || DEFAULT_AVATAR" 
                    :alt="authorInfo?.nickname"
                    class="w-14 h-14 rounded-xl cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-gray-100 dark:ring-gray-700"
                    @click="goToUserHome(authorInfo?.id)"
                  />
                  <div class="flex-1 min-w-0">
                    <h4 
                      class="font-bold text-gray-900 dark:text-white cursor-pointer hover:text-primary-500 transition-colors truncate"
                      @click="goToUserHome(authorInfo?.id)"
                    >{{ authorInfo?.nickname }}</h4>
                    <p class="text-xs text-gray-400 mt-0.5">{{ authorInfo?.bio || '这个人很懒，什么都没写' }}</p>
                  </div>
                </div>

                <div class="grid grid-cols-4 gap-1 text-center mb-4">
                  <div class="py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ authorInfo?.articleCount || 0 }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">原创</div>
                  </div>
                  <div class="py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(authorInfo?.likeCount) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">获赞</div>
                  </div>
                  <div class="py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(authorInfo?.collectionCount) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">收藏</div>
                  </div>
                  <div class="py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                    <div class="text-base font-bold text-gray-900 dark:text-white">{{ formatNumber(authorInfo?.fansCount) }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">粉丝</div>
                  </div>
                </div>

                <div class="flex gap-2">
                  <button 
                    v-if="isAuthor"
                    @click="router.push('/mine')"
                    class="flex-1 px-3 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl text-sm hover:from-primary-600 hover:to-primary-700 transition-all font-medium shadow-lg shadow-primary-500/20"
                  >
                    进入我的主页
                  </button>
                  <template v-else-if="followStatusLoaded">
                    <button 
                      @click="handleToggleFollow"
                      :class="authorInfo?.isFollowed 
                        ? 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-600' 
                        : 'bg-gradient-to-r from-red-500 to-rose-500 text-white shadow-lg shadow-red-500/20'"
                      class="flex-1 px-3 py-2.5 rounded-xl text-sm hover:opacity-90 transition-all font-medium"
                    >
                      {{ authorInfo?.isFollowed ? '已关注' : '关注' }}
                    </button>
                    <button 
                      @click="handleSendMessage"
                      class="flex-1 px-3 py-2.5 border border-gray-200 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-xl text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium"
                    >
                      私信
                    </button>
                  </template>
                  <!-- 关注状态加载中显示占位，避免布局跳动 -->
                  <template v-else>
                    <div class="flex-1 px-3 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-700 animate-pulse"></div>
                    <div class="flex-1 px-3 py-2.5 rounded-xl bg-gray-100 dark:bg-gray-700 animate-pulse"></div>
                  </template>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-red-500 to-rose-500 flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                  </svg>
                </div>
                <h3 class="font-bold text-gray-900 dark:text-white text-sm">热门文章</h3>
              </div>
              <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
                <template v-if="hotArticlesLoading">
                  <div v-for="i in 3" :key="i" class="flex items-start gap-3 p-3">
                    <div class="flex-shrink-0 w-6 h-6 rounded-lg bg-gray-200 dark:bg-gray-700 animate-pulse"></div>
                    <div class="flex-1 min-w-0 space-y-2">
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-full"></div>
                      <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-2/3"></div>
                      <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-1/3 mt-1"></div>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <router-link
                    v-for="(item, index) in hotArticles"
                    :key="item.id"
                    :to="`/article/${item.id}`"
                    class="flex items-start gap-3 p-3 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors group"
                  >
                    <span
                      class="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-lg text-xs font-bold transition-transform group-hover:scale-110"
                      :class="{
                        'bg-gradient-to-br from-red-500 to-rose-500 text-white shadow-lg shadow-red-500/30': index === 0,
                        'bg-gradient-to-br from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-500/30': index === 1,
                        'bg-gradient-to-br from-yellow-500 to-orange-400 text-white shadow-lg shadow-yellow-500/30': index === 2
                      }"
                    >
                      {{ index + 1 }}
                    </span>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-primary-500 line-clamp-2 transition-colors font-medium leading-relaxed">
                        {{ item.title }}
                      </p>
                      <p class="text-xs text-gray-400 mt-1 flex items-center gap-1">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        {{ formatNumber(item.views) }} 阅读
                      </p>
                    </div>
                  </router-link>
                </template>
              </div>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                </div>
                <h3 class="font-bold text-gray-900 dark:text-white text-sm">猜你喜欢</h3>
              </div>
              <div class="divide-y divide-gray-50 dark:divide-gray-700/50">
                <template v-if="randomArticlesLoading">
                  <div v-for="i in 4" :key="i" class="flex items-center justify-between p-3">
                    <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse flex-1 mr-3"></div>
                    <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse w-14"></div>
                  </div>
                </template>
                <template v-else>
                  <router-link
                    v-for="item in randomArticles"
                    :key="item.id"
                    :to="`/article/${item.id}`"
                    class="flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors group"
                  >
                    <p class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-primary-500 line-clamp-1 flex-1 mr-2 font-medium">
                      {{ item.title }}
                    </p>
                    <span class="text-xs text-gray-400 whitespace-nowrap">
                      {{ formatNumber(item.views) }} 阅读
                    </span>
                  </router-link>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 min-w-0">
          <article class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
            <div class="p-4 sm:p-6 lg:p-8">
              <header class="mb-6 sm:mb-8">
                <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 mb-4 flex-wrap">
                  <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    {{ formatAbsoluteDate(article.createdAt) }}
                  </span>
                  <span class="text-gray-300 dark:text-gray-600">·</span>
                  <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ formatNumber(article.views) }} 阅读
                  </span>
                </div>
                
                <h1 class="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6 leading-tight">
                  {{ article.title }}
                </h1>

                <div class="flex flex-wrap gap-2 mb-4 sm:mb-6">
                  <span class="px-3 py-1.5 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 text-sm rounded-lg font-medium">
                    {{ article.category?.name || '未分类' }}
                  </span>
                  <span 
                    v-for="tag in article.tags" 
                    :key="tag.id"
                    class="px-3 py-1.5 bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400 text-sm rounded-lg font-medium hover:bg-primary-100 dark:hover:bg-primary-900/30 transition-colors cursor-pointer"
                  >
                    #{{ tag.name }}
                  </span>
                </div>
              </header>

              <div 
                ref="articleContentRef"
                class="prose prose-base sm:prose-lg dark:prose-invert max-w-none mb-6 sm:mb-8 relative transition-all duration-500"
                :class="!showFullContent && isLongArticle ? 'max-h-[600px] overflow-hidden' : ''"
              >
                <!-- 渐变遮罩 -->
                <div 
                  v-if="!showFullContent && isLongArticle"
                  class="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-white dark:from-gray-800 to-transparent pointer-events-none z-10"
                ></div>
                
                <div v-html="renderedContent"></div>
              </div>

              <!-- 查看更多按钮 -->
              <div 
                v-if="isLongArticle" 
                class="flex justify-center mb-6 sm:mb-8"
              >
                <button
                  @click="showFullContent = !showFullContent"
                  class="flex items-center gap-2 px-6 py-3 text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium text-sm transition-all group"
                >
                  <span>{{ showFullContent ? '收起内容' : '点击查看更多内容' }}</span>
                  <svg 
                    class="w-4 h-4 transition-transform duration-300" 
                    :class="showFullContent ? 'rotate-180' : ''"
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
              </div>

              <div class="flex items-center justify-center gap-6 sm:gap-10 py-6 sm:py-8 border-t border-gray-100 dark:border-gray-700">
                <button 
                  @click="handleToggleLike"
                  class="flex flex-col items-center gap-2 group"
                >
                  <div 
                    class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
                    :class="article.isLiked 
                      ? 'bg-gradient-to-br from-red-500 to-rose-500 shadow-lg shadow-red-500/30' 
                      : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-red-50 dark:group-hover:bg-red-900/20'"
                  >
                    <svg 
                      class="w-6 h-6 transition-colors" 
                      :class="article.isLiked ? 'text-white' : 'text-gray-500 dark:text-gray-400 group-hover:text-red-500'"
                      :fill="article.isLiked ? 'currentColor' : 'none'" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                  </div>
                  <span class="text-sm font-medium" :class="article.isLiked ? 'text-red-500' : 'text-gray-500 dark:text-gray-400'">{{ formatNumber(article.totalLikes) }}</span>
                </button>

                <button 
                  @click="handleToggleCollect"
                  class="flex flex-col items-center gap-2 group"
                >
                  <div 
                    class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
                    :class="article.isCollected 
                      ? 'bg-gradient-to-br from-yellow-500 to-orange-500 shadow-lg shadow-yellow-500/30' 
                      : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-yellow-50 dark:group-hover:bg-yellow-900/20'"
                  >
                    <svg 
                      class="w-6 h-6 transition-colors" 
                      :class="article.isCollected ? 'text-white' : 'text-gray-500 dark:text-gray-400 group-hover:text-yellow-500'"
                      :fill="article.isCollected ? 'currentColor' : 'none'" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                    </svg>
                  </div>
                  <span class="text-sm font-medium" :class="article.isCollected ? 'text-yellow-500' : 'text-gray-500 dark:text-gray-400'">{{ formatNumber(article.collections) }}</span>
                </button>

                <button 
                  @click="toggleComments"
                  class="flex flex-col items-center gap-2 group"
                >
                  <div 
                    class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110"
                    :class="showComments 
                      ? 'bg-gradient-to-br from-primary-500 to-primary-600 shadow-lg shadow-primary-500/30' 
                      : 'bg-gray-100 dark:bg-gray-700 group-hover:bg-primary-50 dark:group-hover:bg-primary-900/20'"
                  >
                    <svg 
                      class="w-6 h-6 transition-colors" 
                      :class="showComments ? 'text-white' : 'text-gray-500 dark:text-gray-400 group-hover:text-primary-500'"
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                  </div>
                  <span class="text-sm font-medium" :class="showComments ? 'text-primary-500' : 'text-gray-500 dark:text-gray-400'">{{ article.comments }}</span>
                </button>
              </div>
            </div>
          </article>

          <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-4 sm:p-6 mt-4 sm:mt-6">
            <div class="flex items-center gap-3 mb-4 sm:mb-6">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 flex items-center justify-center shadow-lg shadow-violet-500/20">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                </svg>
              </div>
              <h3 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">相关文章</h3>
            </div>
            <div v-if="relatedArticles.length > 0" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <router-link 
                v-for="related in relatedArticles" 
                :key="related.id"
                :to="`/article/${related.id}`"
                class="group"
              >
                <div class="relative h-32 sm:h-36 rounded-xl overflow-hidden mb-3">
                  <img 
                    :src="related.cover || DEFAULT_COVER" 
                    :alt="related.title"
                    class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                  />
                  <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                </div>
                <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 group-hover:text-primary-500 transition-colors line-clamp-2 leading-relaxed">
                  {{ related.title }}
                </h4>
              </router-link>
            </div>
            <div v-else class="text-center py-8">
              <div class="w-16 h-16 mx-auto mb-3 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="text-gray-400 text-sm">暂无相关文章</p>
            </div>
          </div>

          <transition
            enter-active-class="transition duration-300 ease-out"
            enter-from-class="opacity-0 max-h-0"
            enter-to-class="opacity-100 max-h-[2000px]"
            leave-active-class="transition duration-200 ease-in"
            leave-from-class="opacity-100 max-h-[2000px]"
            leave-to-class="opacity-0 max-h-0"
          >
            <div v-if="showComments" class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 p-4 sm:p-6 mt-4 sm:mt-6 overflow-hidden">
              <div class="flex items-center gap-3 mb-4 sm:mb-6">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/20">
                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <h3 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                  评论 <span class="text-primary-500">({{ article.comments }})</span>
                </h3>
              </div>

              <div class="mb-6 sm:mb-8">
                <div class="flex gap-3">
                  <img 
                    :src="userStore.state.userInfo?.avatar || DEFAULT_AVATAR" 
                    alt="用户头像"
                    class="w-10 h-10 rounded-xl flex-shrink-0 ring-2 ring-gray-100 dark:ring-gray-700"
                  />
                  <div class="flex-1">
                    <textarea 
                      v-model="newComment"
                      placeholder="发表你的评论..."
                      class="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-gray-50 dark:bg-gray-700/50 focus:bg-white dark:focus:bg-gray-700 text-gray-900 dark:text-white text-sm transition-all"
                      rows="3"
                    ></textarea>
                    <div class="flex justify-between items-center mt-3">
                      <span class="text-xs text-gray-400">{{ newComment.length }}/500</span>
                      <button 
                        @click="submitComment"
                        :disabled="!newComment.trim()"
                        class="px-5 py-2 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl hover:from-primary-600 hover:to-primary-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm font-semibold shadow-lg shadow-primary-500/20"
                      >
                        发表评论
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="space-y-4 sm:space-y-6">
                <div 
                  v-for="comment in comments" 
                  :key="comment.id"
                  :data-comment-id="comment.id"
                  class="p-4 rounded-xl bg-gray-50 dark:bg-gray-700/30 hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors"
                >
                  <div class="flex gap-3">
                    <img 
                      :src="comment.author?.avatar || DEFAULT_AVATAR" 
                      :alt="comment.author?.name"
                      class="w-10 h-10 rounded-xl flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-gray-100 dark:ring-gray-700"
                      @click="goToUserHome(comment.author?.id)"
                    />
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-2">
                        <span 
                          class="font-semibold text-gray-900 dark:text-white text-sm cursor-pointer hover:text-primary-500 transition-colors"
                          @click="goToUserHome(comment.author?.id)"
                        >{{ comment.author?.name }}</span>
                        <span 
                          v-if="comment.author?.id === article.authorId"
                          class="px-1.5 py-0.5 bg-gradient-to-r from-red-500 to-orange-500 text-white text-xs font-medium rounded"
                        >UP主</span>
                        <span class="text-xs text-gray-400">{{ formatAbsoluteDate(comment.createdAt) }}</span>
                      </div>
                      <p class="text-gray-700 dark:text-gray-300 mb-3 text-sm leading-relaxed">{{ comment.content }}</p>
                      <div class="flex items-center gap-4">
                        <button 
                          @click="handleToggleCommentLike(comment)"
                          class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-primary-500 transition-colors"
                          :class="comment.isLiked ? 'text-primary-500' : ''"
                        >
                          <svg 
                            class="w-4 h-4" 
                            :fill="comment.isLiked ? 'currentColor' : 'none'" 
                            stroke="currentColor" 
                            viewBox="0 0 24 24"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                          </svg>
                          <span class="font-medium">{{ comment.likes }}</span>
                        </button>
                        <button 
                          @click="toggleReply(comment.id)"
                          class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-primary-500 transition-colors"
                          :class="replyingTo === comment.id ? 'text-primary-500' : ''"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                          </svg>
                          <span class="font-medium">{{ replyingTo === comment.id ? '取消回复' : '回复' }}</span>
                        </button>
                        <button 
                          v-if="userStore.state.userInfo?.id === comment.author?.id"
                          @click="deleteComment(comment.id)"
                          class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-500 transition-colors"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                          <span class="font-medium">删除</span>
                        </button>
                      </div>

                      <!-- 回复输入框 -->
                      <div v-if="replyingTo === comment.id" class="mt-4 pl-4">
                        <div class="flex gap-3">
                          <img 
                            :src="userStore.state.userInfo?.avatar || DEFAULT_AVATAR" 
                            alt="用户头像"
                            class="w-8 h-8 rounded-lg flex-shrink-0 ring-2 ring-gray-100 dark:ring-gray-700"
                          />
                          <div class="flex-1">
                            <textarea 
                              v-model="replyContent[comment.id]"
                              placeholder="发表你的回复..."
                              class="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-gray-50 dark:bg-gray-700/50 focus:bg-white dark:focus:bg-gray-700 text-gray-900 dark:text-white text-sm transition-all"
                              rows="2"
                            ></textarea>
                            <div class="flex justify-between items-center mt-2">
                              <span class="text-xs text-gray-400">{{ (replyContent[comment.id] || '').length }}/500</span>
                              <div class="flex gap-2">
                                <button 
                                  @click="cancelReply"
                                  class="px-3 py-1.5 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 text-sm transition-colors"
                                >
                                  取消
                                </button>
                                <button 
                                  @click="submitReply(comment.id)"
                                  :disabled="!(replyContent[comment.id] || '').trim()"
                                  class="px-4 py-1.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-lg hover:from-primary-600 hover:to-primary-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium"
                                >
                                  发送
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div v-if="comment.replies && comment.replies.length > 0" class="mt-4 pl-4 border-l-2 border-primary-200 dark:border-primary-800 space-y-3">
                        <template v-for="(reply, rIdx) in comment.replies" :key="reply.id">
                          <div 
                            v-show="expandedReplies[comment.id] || rIdx < 3"
                            :data-comment-id="reply.id"
                            class="flex gap-3"
                          >
                          <img 
                            :src="reply.author?.avatar || DEFAULT_AVATAR" 
                            :alt="reply.author?.name"
                            class="w-8 h-8 rounded-lg flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
                            @click="goToUserHome(reply.author?.id)"
                          />
                          <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-1">
                              <span 
                                class="font-semibold text-gray-900 dark:text-white text-sm cursor-pointer hover:text-primary-500 transition-colors"
                                @click="goToUserHome(reply.author?.id)"
                              >{{ reply.author?.name }}</span>
                              <span 
                                v-if="reply.author?.id === article.authorId"
                                class="px-1.5 py-0.5 bg-gradient-to-r from-red-500 to-orange-500 text-white text-xs font-medium rounded"
                              >UP主</span>
                              <span class="text-xs text-gray-400">{{ formatAbsoluteDate(reply.createdAt) }}</span>
                            </div>
                            <p class="text-gray-700 dark:text-gray-300 mb-2 text-sm leading-relaxed">{{ reply.content }}</p>
                            <button 
                              @click="handleToggleCommentLike(reply)"
                              class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-primary-500 transition-colors"
                              :class="reply.isLiked ? 'text-primary-500' : ''"
                            >
                              <svg 
                                class="w-4 h-4" 
                                :fill="reply.isLiked ? 'currentColor' : 'none'" 
                                stroke="currentColor" 
                                viewBox="0 0 24 24"
                              >
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                              </svg>
                              <span class="font-medium">{{ reply.likes }}</span>
                            </button>
                          </div>
                        </div>
                        </template>
                        <!-- 展开/收起更多回复 -->
                        <button
                          v-if="comment.replies.length > 3"
                          @click="expandedReplies[comment.id] = !expandedReplies[comment.id]"
                          class="flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium mt-2 transition-colors"
                        >
                          <svg
                            class="w-4 h-4 transition-transform duration-300"
                            :class="expandedReplies[comment.id] ? 'rotate-180' : ''"
                            fill="none" stroke="currentColor" viewBox="0 0 24 24"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                          </svg>
                          {{ expandedReplies[comment.id] ? '收起回复' : `展开更多回复（共${comment.replies.length}条）` }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div v-if="comments.length === 0" class="text-center py-12">
                <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                  <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <p class="text-gray-400 font-medium">暂无评论</p>
                <p class="text-sm text-gray-400 dark:text-gray-500 mt-1">快来发表第一条评论吧！</p>
              </div>
            </div>
          </transition>
        </div>

        <div class="lg:w-56 flex-shrink-0 hidden lg:block">
          <div class="sticky top-20">
            <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700/50 overflow-hidden">
              <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
                  </svg>
                </div>
                <h3 class="font-bold text-gray-900 dark:text-white text-sm">目录</h3>
              </div>
              <div class="p-3 max-h-[calc(100vh-200px)] overflow-y-auto">
                <nav class="space-y-1">
                  <template v-for="heading in headings" :key="heading.id">
                    <a
                      @click.prevent="scrollToHeading(heading.id)"
                      href="#"
                      class="block py-1.5 px-3 rounded-lg cursor-pointer transition-all duration-200 leading-snug"
                      :class="[
                        activeHeadingId === heading.id
                          ? 'text-primary-500 bg-primary-50 dark:bg-primary-900/20 font-semibold border-l-2 border-primary-500'
                          : 'text-gray-600 dark:text-gray-400 hover:text-primary-500 hover:bg-gray-50 dark:hover:bg-gray-700/50',
                        heading.level === 1 ? 'text-sm font-bold pl-3' : '',
                        heading.level === 2 ? 'text-[13px] font-semibold pl-3' : '',
                        heading.level === 3 ? 'text-xs pl-6' : '',
                        heading.level === 4 ? 'text-[11px] pl-9' : '',
                        heading.level === 5 ? 'text-[11px] pl-11' : '',
                        heading.level === 6 ? 'text-[11px] pl-[52px]' : ''
                      ]"
                    >
                      {{ heading.text }}
                    </a>
                  </template>
                </nav>
                <div v-if="headings.length === 0" class="text-sm text-gray-400 py-4 text-center">
                  <div class="w-12 h-12 mx-auto mb-2 rounded-xl bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                    <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
                    </svg>
                  </div>
                  暂无目录
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.prose {
  @apply text-gray-700 dark:text-gray-300;
}

.prose h1 {
  @apply text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mt-6 sm:mt-8 mb-4 sm:mb-6;
}

.prose h2 {
  @apply text-xl sm:text-2xl font-bold text-gray-900 dark:text-white mt-6 sm:mt-8 mb-3 sm:mb-4;
}

.prose h3 {
  @apply text-lg sm:text-xl font-bold text-gray-900 dark:text-white mt-4 sm:mt-6 mb-2 sm:mb-3;
}

.prose h4 {
  @apply text-base sm:text-lg font-bold text-gray-900 dark:text-white mt-3 sm:mt-4 mb-2;
}

.prose p {
  @apply mb-3 sm:mb-4 leading-relaxed text-sm sm:text-base;
}

.prose ul, .prose ol {
  @apply my-3 sm:my-4 pl-5 sm:pl-6;
}

.prose li {
  @apply mb-1 sm:mb-2 text-sm sm:text-base;
}

.prose code {
  @apply px-1.5 py-0.5 bg-gray-100 dark:bg-gray-700 rounded text-xs sm:text-sm font-mono text-pink-600 dark:text-pink-400;
}

.prose pre {
  @apply my-3 sm:my-4 p-3 sm:p-4 bg-gray-900 rounded-lg overflow-x-auto text-xs sm:text-sm;
}

.prose pre code {
  @apply bg-transparent p-0 text-gray-100;
}

.prose blockquote {
  @apply my-4 pl-4 border-l-4 border-primary-500 bg-gray-50 dark:bg-gray-800/50 py-2 px-4 italic text-gray-600 dark:text-gray-400;
}

.prose a {
  @apply text-primary-500 hover:text-primary-600 underline;
}

.prose img {
  @apply my-4 rounded-lg max-w-full h-auto;
}

.prose table {
  @apply my-4 w-full border-collapse;
}

.prose th {
  @apply bg-gray-100 dark:bg-gray-700 px-4 py-2 text-left font-bold border border-gray-200 dark:border-gray-600;
}

.prose td {
  @apply px-4 py-2 border border-gray-200 dark:border-gray-600;
}

.prose hr {
  @apply my-6 border-t border-gray-200 dark:border-gray-700;
}

.prose strong {
  @apply font-bold text-gray-900 dark:text-white;
}

.prose em {
  @apply italic;
}

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

.highlight-comment {
  animation: highlight-pulse 3s ease-out;
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.3);
  border-radius: 0.75rem;
}

@keyframes highlight-pulse {
  0% {
    box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.6);
    background-color: rgba(236, 72, 153, 0.1);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.3);
    background-color: rgba(236, 72, 153, 0.05);
  }
  100% {
    box-shadow: 0 0 0 3px rgba(236, 72, 153, 0);
    background-color: transparent;
  }
}
</style>
