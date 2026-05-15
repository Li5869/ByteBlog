<script setup>
import {computed, h, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {
  NAvatar,
  NButton,
  NCard,
  NDivider,
  NGradientText,
  NIcon,
  NNumberAnimation,
  NProgress,
  NSelect,
  NSpin,
  NTooltip
} from 'naive-ui'
import {
  ArrowBackOutline,
  BarChartOutline,
  ChatbubbleOutline,
  ChevronForwardOutline,
  EyeOutline,
  FlameOutline,
  HeartOutline,
  PeopleOutline,
  PersonAddOutline,
  PieChartOutline,
  SparklesOutline,
  TimeOutline,
  TrendingDownOutline,
  TrendingUpOutline
} from '@vicons/ionicons5'
import {isLoggedIn} from '@/utils/request'
import {toast} from '@/utils/toast'
import {formatNumber} from '@/utils/format'

const router = useRouter()
const loading = ref(true)
const timeRange = ref('7days')

const timeRangeOptions = [
  { label: '近7天', value: '7days' },
  { label: '近30天', value: '30days' },
  { label: '近90天', value: '90days' }
]

const mockDataByRange = {
  '7days': {
    overview: {
      totalViews: 12580,
      totalLikes: 892,
      totalComments: 156,
      totalFans: 234,
      viewsGrowth: 12.5,
      likesGrowth: 8.3,
      commentsGrowth: -2.1,
      fansGrowth: 15.2
    },
    trendData: {
      labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      views: [1200, 1800, 1500, 2200, 1900, 2800, 2100],
      likes: [85, 120, 95, 140, 110, 180, 130]
    }
  },
  '30days': {
    overview: {
      totalViews: 48620,
      totalLikes: 3256,
      totalComments: 478,
      totalFans: 892,
      viewsGrowth: 23.8,
      likesGrowth: 18.5,
      commentsGrowth: 12.3,
      fansGrowth: 28.6
    },
    trendData: {
      labels: ['第1周', '第2周', '第3周', '第4周'],
      views: [11200, 13500, 10800, 13120],
      likes: [780, 920, 650, 906]
    }
  },
  '90days': {
    overview: {
      totalViews: 156800,
      totalLikes: 9845,
      totalComments: 1234,
      totalFans: 2456,
      viewsGrowth: 45.2,
      likesGrowth: 32.8,
      commentsGrowth: 28.5,
      fansGrowth: 56.3
    },
    trendData: {
      labels: ['1月', '2月', '3月'],
      views: [48000, 52600, 56200],
      likes: [3100, 3345, 3400]
    }
  }
}

const mockStats = ref(JSON.parse(JSON.stringify(mockDataByRange['7days'])))

const topArticles = ref([
  { id: 1, title: 'Vue 3 组合式 API 最佳实践', views: 3520, likes: 186, comments: 32, date: '2024-01-15', cover: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=100&h=100&fit=crop' },
  { id: 2, title: 'Spring Boot 微服务架构设计', views: 2890, likes: 142, comments: 28, date: '2024-01-12', cover: 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=100&h=100&fit=crop' },
  { id: 3, title: 'PostgreSQL 性能优化指南', views: 2150, likes: 98, comments: 18, date: '2024-01-10', cover: 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=100&h=100&fit=crop' },
  { id: 4, title: 'Redis 缓存策略详解', views: 1820, likes: 76, comments: 15, date: '2024-01-08', cover: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=100&h=100&fit=crop' },
  { id: 5, title: 'Docker 容器化部署实战', views: 1560, likes: 65, comments: 12, date: '2024-01-05', cover: 'https://images.unsplash.com/photo-1605745341112-85968b19335b?w=100&h=100&fit=crop' }
])

const recentFans = ref([
  { id: 1, name: '张三', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=50&h=50&fit=crop', time: '2小时前', isFollowing: false },
  { id: 2, name: '李四', avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=50&h=50&fit=crop', time: '5小时前', isFollowing: true },
  { id: 3, name: '王五', avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=50&h=50&fit=crop', time: '1天前', isFollowing: false },
  { id: 4, name: '赵六', avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=50&h=50&fit=crop', time: '2天前', isFollowing: true }
])

const categoryStats = ref([
  { name: '前端开发', count: 28, percentage: 45, color: '#3B82F6' },
  { name: '后端技术', count: 22, percentage: 35, color: '#10B981' },
  { name: '数据库', count: 8, percentage: 13, color: '#F59E0B' },
  { name: '运维部署', count: 4, percentage: 7, color: '#8B5CF6' }
])

watch(timeRange, (newRange) => {
  loading.value = true
  setTimeout(() => {
    mockStats.value = JSON.parse(JSON.stringify(mockDataByRange[newRange]))
    loading.value = false
  }, 300)
})


const getGrowthType = (value) => {
  return value >= 0 ? 'success' : 'error'
}

const getGrowthIcon = (value) => {
  return value >= 0 ? TrendingUpOutline : TrendingDownOutline
}

const getRankStyle = (index) => {
  const styles = [
    { bg: 'linear-gradient(135deg, #FBBF24 0%, #F59E0B 100%)', color: '#fff', shadow: '0 4px 12px rgba(251, 191, 36, 0.4)' },
    { bg: 'linear-gradient(135deg, #9CA3AF 0%, #6B7280 100%)', color: '#fff', shadow: '0 4px 12px rgba(107, 114, 128, 0.3)' },
    { bg: 'linear-gradient(135deg, #FB923C 0%, #EA580C 100%)', color: '#fff', shadow: '0 4px 12px rgba(234, 88, 12, 0.4)' },
    { bg: 'linear-gradient(135deg, #E5E7EB 0%, #D1D5DB 100%)', color: '#6B7280', shadow: '0 2px 8px rgba(0, 0, 0, 0.1)' },
    { bg: 'linear-gradient(135deg, #F3F4F6 0%, #E5E7EB 100%)', color: '#9CA3AF', shadow: '0 2px 8px rgba(0, 0, 0, 0.1)' }
  ]
  return styles[index] || styles[4]
}

const statCards = computed(() => [
  {
    key: 'views',
    label: '总阅读',
    value: mockStats.value.overview.totalViews,
    growth: mockStats.value.overview.viewsGrowth,
    icon: EyeOutline,
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #06B6D4 100%)',
    bgGradient: 'linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(6, 182, 212, 0.1) 100%)',
    shadowColor: 'rgba(59, 130, 246, 0.25)'
  },
  {
    key: 'likes',
    label: '总点赞',
    value: mockStats.value.overview.totalLikes,
    growth: mockStats.value.overview.likesGrowth,
    icon: HeartOutline,
    gradient: 'linear-gradient(135deg, #EF4444 0%, #EC4899 100%)',
    bgGradient: 'linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(236, 72, 153, 0.1) 100%)',
    shadowColor: 'rgba(239, 68, 68, 0.25)'
  },
  {
    key: 'comments',
    label: '总评论',
    value: mockStats.value.overview.totalComments,
    growth: mockStats.value.overview.commentsGrowth,
    icon: ChatbubbleOutline,
    gradient: 'linear-gradient(135deg, #10B981 0%, #34D399 100%)',
    bgGradient: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(52, 211, 153, 0.1) 100%)',
    shadowColor: 'rgba(16, 185, 129, 0.25)'
  },
  {
    key: 'fans',
    label: '总粉丝',
    value: mockStats.value.overview.totalFans,
    growth: mockStats.value.overview.fansGrowth,
    icon: PeopleOutline,
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #A78BFA 100%)',
    bgGradient: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%)',
    shadowColor: 'rgba(139, 92, 246, 0.25)'
  }
])

const growthPeriodLabel = computed(() => {
  switch (timeRange.value) {
    case '7days':
      return '较上周'
    case '30days':
      return '较上月'
    case '90days':
      return '较上季'
    default:
      return '较上周'
  }
})

const goBack = () => {
  router.back()
}

const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

const goToUserHome = (id) => {
  router.push(`/user/${id}`)
}

const followFan = (fan) => {
  fan.isFollowing = !fan.isFollowing
  toast.success(fan.isFollowing ? '关注成功' : '已取消关注')
}

const renderIcon = (icon, props = {}) => {
  return h(NIcon, props, { default: () => h(icon) })
}

onMounted(() => {
  if (!isLoggedIn()) {
    router.push('/')
    return
  }
  setTimeout(() => {
    loading.value = false
  }, 500)
})
</script>

<template>
  <div class="analytics-container">
    <div class="analytics-header">
      <div class="header-bg-pattern"></div>
      <div class="header-circle header-circle-1"></div>
      <div class="header-circle header-circle-2"></div>
      
      <div class="header-content">
        <div class="header-left">
          <NButton quaternary circle size="large" class="back-btn" @click="goBack">
            <template #icon>
              <NIcon :component="ArrowBackOutline" :size="22" />
            </template>
          </NButton>
          <div class="header-title-group">
            <h1 class="header-title">
              <NGradientText type="danger">
                数据概览
              </NGradientText>
            </h1>
            <p class="header-subtitle">
              <NIcon :component="SparklesOutline" :size="14" style="margin-right: 4px" />
              分析你的内容表现与影响力
            </p>
          </div>
        </div>
        <div class="header-actions">
          <NSelect
            v-model:value="timeRange"
            :options="timeRangeOptions"
            size="medium"
            style="width: 130px"
            :consistent-menu-width="false"
          />
        </div>
      </div>
    </div>

    <div class="analytics-body">
      <NSpin :show="loading">
        <div class="stats-grid">
          <NCard
            v-for="stat in statCards"
            :key="stat.key"
            class="stat-card"
            :style="{ '--card-shadow': stat.shadowColor }"
            :bordered="false"
          >
            <div class="stat-card-inner" :style="{ background: stat.bgGradient }">
              <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
                <NIcon :component="stat.icon" :size="26" color="#fff" />
              </div>
              <div class="stat-content">
                <span class="stat-label">{{ stat.label }}</span>
                <div class="stat-value">
                  <NNumberAnimation
                    :from="0"
                    :to="stat.value"
                    :duration="1000"
                    :precision="0"
                  >
                    <template #default="{ value }">
                      <span class="stat-number">{{ formatNumber(value) }}</span>
                    </template>
                  </NNumberAnimation>
                </div>
                <div class="stat-growth">
                  <NIcon
                    :component="getGrowthIcon(stat.growth)"
                    :size="14"
                    :color="stat.growth >= 0 ? '#10B981' : '#EF4444'"
                  />
                  <span :style="{ color: stat.growth >= 0 ? '#10B981' : '#EF4444' }">
                    {{ stat.growth >= 0 ? '+' : '' }}{{ stat.growth }}%
                  </span>
                  <span class="growth-period">{{ growthPeriodLabel }}</span>
                </div>
              </div>
            </div>
          </NCard>
        </div>

        <div class="charts-section">
          <NCard class="chart-card trend-card" :bordered="false">
            <template #header>
              <div class="card-header">
                <div class="card-header-left">
                  <div class="card-icon-wrapper" style="background: linear-gradient(135deg, #3B82F6 0%, #06B6D4 100%)">
                    <NIcon :component="BarChartOutline" :size="20" color="#fff" />
                  </div>
                  <div>
                    <h3 class="card-title">访问趋势</h3>
                    <p class="card-subtitle">每日数据变化</p>
                  </div>
                </div>
              </div>
            </template>

            <div class="trend-chart">
              <div class="chart-bars">
                <div
                  v-for="(item, index) in mockStats.trendData.labels"
                  :key="index"
                  class="chart-bar-group"
                >
                  <NTooltip trigger="hover" placement="top" :delay="100">
                    <template #trigger>
                      <div class="bar-wrapper">
                        <div
                          class="bar bar-likes"
                          :style="{ height: Math.max((mockStats.trendData.likes[index] / Math.max(...mockStats.trendData.likes)) * 60, 4) + 'px' }"
                        ></div>
                        <div
                          class="bar bar-views"
                          :style="{ height: Math.max((mockStats.trendData.views[index] / Math.max(...mockStats.trendData.views)) * 120, 8) + 'px' }"
                        ></div>
                      </div>
                    </template>
                    <div class="tooltip-content">
                      <div class="tooltip-title">{{ item }}</div>
                      <div class="tooltip-row">
                        <span class="tooltip-dot views"></span>
                        <span>阅读: {{ mockStats.trendData.views[index].toLocaleString() }}</span>
                      </div>
                      <div class="tooltip-row">
                        <span class="tooltip-dot likes"></span>
                        <span>点赞: {{ mockStats.trendData.likes[index] }}</span>
                      </div>
                    </div>
                  </NTooltip>
                  <span class="bar-label">{{ item }}</span>
                </div>
              </div>
              
              <div class="chart-legend">
                <div class="legend-item">
                  <span class="legend-dot views"></span>
                  <span>阅读量</span>
                </div>
                <div class="legend-item">
                  <span class="legend-dot likes"></span>
                  <span>点赞数</span>
                </div>
              </div>
            </div>
          </NCard>

          <NCard class="chart-card category-card" :bordered="false">
            <template #header>
              <div class="card-header">
                <div class="card-header-left">
                  <div class="card-icon-wrapper" style="background: linear-gradient(135deg, #8B5CF6 0%, #A78BFA 100%)">
                    <NIcon :component="PieChartOutline" :size="20" color="#fff" />
                  </div>
                  <div>
                    <h3 class="card-title">文章分类</h3>
                    <p class="card-subtitle">各分类占比分布</p>
                  </div>
                </div>
              </div>
            </template>

            <div class="category-list">
              <div v-for="category in categoryStats" :key="category.name" class="category-item">
                <div class="category-header">
                  <div class="category-info">
                    <span class="category-dot" :style="{ backgroundColor: category.color }"></span>
                    <span class="category-name">{{ category.name }}</span>
                  </div>
                  <div class="category-stats">
                    <span class="category-count">{{ category.count }}篇</span>
                    <span class="category-percent">{{ category.percentage }}%</span>
                  </div>
                </div>
                <NProgress
                  type="line"
                  :percentage="category.percentage"
                  :color="category.color"
                  :rail-color="'#F3F4F6'"
                  :show-indicator="false"
                  :height="8"
                  :border-radius="4"
                />
              </div>
            </div>
          </NCard>
        </div>

        <div class="content-section">
          <NCard class="content-card articles-card" :bordered="false">
            <template #header>
              <div class="card-header">
                <div class="card-header-left">
                  <div class="card-icon-wrapper" style="background: linear-gradient(135deg, #F59E0B 0%, #EF4444 100%)">
                    <NIcon :component="FlameOutline" :size="20" color="#fff" />
                  </div>
                  <div>
                    <h3 class="card-title">热门文章</h3>
                    <p class="card-subtitle">阅读量最高的文章</p>
                  </div>
                </div>
                <NButton text type="primary">
                  查看全部
                  <template #icon>
                    <NIcon :component="ChevronForwardOutline" />
                  </template>
                </NButton>
              </div>
            </template>

            <div class="articles-list">
              <div
                v-for="(article, index) in topArticles"
                :key="article.id"
                class="article-item"
                @click="goToArticle(article.id)"
              >
                <div
                  class="article-rank"
                  :style="getRankStyle(index)"
                >
                  {{ index + 1 }}
                </div>
                <img
                  :src="article.cover"
                  :alt="article.title"
                  class="article-cover"
                />
                <div class="article-content">
                  <h4 class="article-title">{{ article.title }}</h4>
                  <div class="article-meta">
                    <span class="meta-item">
                      <NIcon :component="EyeOutline" :size="14" />
                      {{ formatNumber(article.views) }}
                    </span>
                    <span class="meta-item">
                      <NIcon :component="HeartOutline" :size="14" />
                      {{ article.likes }}
                    </span>
                    <span class="meta-item">
                      <NIcon :component="ChatbubbleOutline" :size="14" />
                      {{ article.comments }}
                    </span>
                  </div>
                </div>
                <div class="article-date">
                  <NIcon :component="TimeOutline" :size="14" />
                  {{ article.date }}
                </div>
              </div>
            </div>
          </NCard>

          <NCard class="content-card fans-card" :bordered="false">
            <template #header>
              <div class="card-header">
                <div class="card-header-left">
                  <div class="card-icon-wrapper" style="background: linear-gradient(135deg, #10B981 0%, #34D399 100%)">
                    <NIcon :component="PersonAddOutline" :size="20" color="#fff" />
                  </div>
                  <div>
                    <h3 class="card-title">新增粉丝</h3>
                    <p class="card-subtitle">最近关注的用户</p>
                  </div>
                </div>
              </div>
            </template>

            <div class="fans-list">
              <div
                v-for="fan in recentFans"
                :key="fan.id"
                class="fan-item"
              >
                <NAvatar
                  :src="fan.avatar"
                  :size="48"
                  round
                  class="fan-avatar"
                  @click="goToUserHome(fan.id)"
                />
                <div class="fan-info">
                  <span class="fan-name" @click="goToUserHome(fan.id)">{{ fan.name }}</span>
                  <span class="fan-time">{{ fan.time }}</span>
                </div>
                <NButton
                  :type="fan.isFollowing ? 'default' : 'primary'"
                  :tertiary="fan.isFollowing"
                  size="small"
                  round
                  @click="followFan(fan)"
                >
                  {{ fan.isFollowing ? '已关注' : '回关' }}
                </NButton>
              </div>
            </div>

            <NDivider style="margin: 16px 0" />

            <NButton block quaternary type="primary" class="view-all-btn">
              <template #icon>
                <NIcon :component="PeopleOutline" />
              </template>
              查看全部粉丝
            </NButton>
          </NCard>
        </div>
      </NSpin>
    </div>
  </div>
</template>

<style scoped>
.analytics-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #F8FAFC 0%, #FFFFFF 50%, #F1F5F9 100%);
}

.analytics-header {
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 0;
  overflow: hidden;
}

.header-bg-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.1;
  background-image: radial-gradient(circle at 25% 25%, white 1px, transparent 1px);
  background-size: 50px 50px;
}

.header-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.header-circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  right: -50px;
}

.header-circle-2 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: -50px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  color: white !important;
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.25) !important;
  transform: translateX(-2px);
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 24px;
  font-weight: 700;
  color: white;
  margin: 0;
}

.header-subtitle {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.analytics-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  margin-top: -20px;
  position: relative;
  z-index: 2;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  border-radius: 16px !important;
  box-shadow: 0 4px 20px var(--card-shadow);
  transition: all 0.3s ease;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px var(--card-shadow);
}

.stat-card-inner {
  padding: 20px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #64748B;
  font-weight: 500;
}

.stat-value {
  display: flex;
  align-items: baseline;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #1E293B;
  line-height: 1;
}

.stat-growth {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.growth-period {
  color: #94A3B8;
  font-size: 12px;
  margin-left: 4px;
}

.charts-section {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  border-radius: 16px !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
}

.card-subtitle {
  font-size: 12px;
  color: #94A3B8;
  margin: 2px 0 0 0;
}

.trend-chart {
  padding: 8px 0;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160px;
  gap: 8px;
  padding: 0 8px;
}

.chart-bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  cursor: pointer;
}

.bar {
  border-radius: 4px;
  transition: all 0.3s ease;
  min-width: 20px;
}

.bar-views {
  background: linear-gradient(180deg, #3B82F6 0%, #60A5FA 100%);
  width: 100%;
}

.bar-likes {
  background: linear-gradient(180deg, #10B981 0%, #34D399 100%);
  width: 10px;
}

.bar-wrapper:hover .bar {
  transform: scaleY(1.05);
  filter: brightness(1.1);
}

.bar-label {
  font-size: 11px;
  color: #64748B;
  font-weight: 500;
}

.tooltip-content {
  padding: 4px 0;
}

.tooltip-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #1E293B;
}

.tooltip-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
  margin-top: 4px;
}

.tooltip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.tooltip-dot.views {
  background: #3B82F6;
}

.tooltip-dot.likes {
  background: #10B981;
}

.chart-legend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748B;
}

.legend-dot {
  width: 12px;
  height: 8px;
  border-radius: 2px;
}

.legend-dot.views {
  background: linear-gradient(90deg, #3B82F6 0%, #60A5FA 100%);
}

.legend-dot.likes {
  background: linear-gradient(90deg, #10B981 0%, #34D399 100%);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.category-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-count {
  font-size: 13px;
  color: #64748B;
}

.category-percent {
  font-size: 13px;
  font-weight: 600;
  color: #1E293B;
  min-width: 40px;
  text-align: right;
}

.content-section {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
}

@media (max-width: 1024px) {
  .content-section {
    grid-template-columns: 1fr;
  }
}

.content-card {
  border-radius: 16px !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.content-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #FAFAFA;
}

.article-item:hover {
  background: #F1F5F9;
  transform: translateX(4px);
}

.article-rank {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.article-cover {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.article-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.article-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.article-item:hover .article-title {
  color: #3B82F6;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94A3B8;
}

.article-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94A3B8;
  flex-shrink: 0;
}

.fans-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.fan-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fan-avatar {
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.fan-avatar:hover {
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.fan-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.fan-name {
  font-size: 14px;
  font-weight: 500;
  color: #1E293B;
  cursor: pointer;
  transition: color 0.3s ease;
}

.fan-name:hover {
  color: #3B82F6;
}

.fan-time {
  font-size: 12px;
  color: #94A3B8;
}

.view-all-btn {
  font-weight: 500;
}

:deep(.n-card) {
  border-radius: 16px;
}

:deep(.n-card-header) {
  padding: 16px 20px;
}

:deep(.n-card__content) {
  padding: 20px;
}

:deep(.n-progress-line) {
  margin-bottom: 0;
}

:deep(.n-gradient-text) {
  font-weight: 700;
}
</style>

<style>
.dark .analytics-container {
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 50%, #0F172A 100%) !important;
}

.dark .stat-card {
  background: #1E293B !important;
}

.dark .stat-card-inner {
  background: rgba(30, 41, 59, 0.5) !important;
}

.dark .stat-label {
  color: #94A3B8 !important;
}

.dark .stat-number {
  color: #F1F5F9 !important;
}

.dark .growth-period {
  color: #64748B !important;
}

.dark .chart-card {
  background: #1E293B !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}

.dark .chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4) !important;
}

.dark .card-title {
  color: #F1F5F9 !important;
}

.dark .card-subtitle {
  color: #64748B !important;
}

.dark .bar-label {
  color: #94A3B8 !important;
}

.dark .tooltip-title {
  color: #F1F5F9 !important;
}

.dark .tooltip-row {
  color: #CBD5E1 !important;
}

.dark .chart-legend {
  border-top-color: #334155 !important;
}

.dark .legend-item {
  color: #CBD5E1 !important;
}

.dark .category-name {
  color: #E2E8F0 !important;
}

.dark .category-count {
  color: #94A3B8 !important;
}

.dark .category-percent {
  color: #F1F5F9 !important;
}

.dark .content-card {
  background: #1E293B !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}

.dark .content-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4) !important;
}

.dark .article-item {
  background: #334155 !important;
}

.dark .article-item:hover {
  background: #475569 !important;
}

.dark .article-title {
  color: #F1F5F9 !important;
}

.dark .meta-item {
  color: #64748B !important;
}

.dark .article-date {
  color: #64748B !important;
}

.dark .fan-name {
  color: #F1F5F9 !important;
}

.dark .fan-time {
  color: #64748B !important;
}

.dark .n-progress .n-progress-rail {
  background-color: #334155 !important;
}
</style>
