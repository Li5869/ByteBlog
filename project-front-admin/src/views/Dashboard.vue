<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import * as echarts from 'echarts'
import {dashboardApi} from '@/utils/request'
import {formatAbsoluteDate, formatNumber} from '@/utils/format'

const stats = ref([
  { name: '文章总数', value: '0', icon: 'Document', color: 'blue', change: '0%' },
  { name: '用户总数', value: '0', icon: 'User', color: 'green', change: '0%' },
  { name: '评论总数', value: '0', icon: 'ChatDotRound', color: 'purple', change: '0%' },
  { name: '问答总数', value: '0', icon: 'QuestionFilled', color: 'orange', change: '0%' }
])

const recentArticles = ref([])

const colorClasses = {
  blue: 'bg-blue-500',
  green: 'bg-green-500',
  purple: 'bg-purple-500',
  orange: 'bg-orange-500'
}

const months = ref(['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'])

const chartData = ref({
  articles: [],
  users: [],
  comments: [],
  questions: []
})

const fetchStatistics = async () => {
  try {
    const data = await dashboardApi.getStatistics()
    stats.value = [
      { name: '文章总数', value: formatNumber(data.articlesTotal), icon: 'Document', color: 'blue', change: data.articlesChange },
      { name: '用户总数', value: formatNumber(data.usersTotal), icon: 'User', color: 'green', change: data.usersChange },
      { name: '评论总数', value: formatNumber(data.commentsTotal), icon: 'ChatDotRound', color: 'purple', change: data.commentsChange },
      { name: '问答总数', value: formatNumber(data.questionsTotal), icon: 'QuestionFilled', color: 'orange', change: data.questionsChange }
    ]
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchTrends = async () => {
  try {
    const data = await dashboardApi.getTrends()
    months.value = data.months
    chartData.value = {
      articles: data.articles,
      users: data.users,
      comments: data.comments,
      questions: data.questions
    }
    // 重新初始化图表
    initBarChart()
    initLineChart()
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}

const fetchRecentArticles = async () => {
  try {
    const data = await dashboardApi.getRecentArticles(5)
    recentArticles.value = data.map(article => ({
      id: article.id,
      title: article.title,
      status: article.status,
      views: article.views,
      date: formatAbsoluteDate(article.createdAt)
    }))
  } catch (error) {
    console.error('获取最近文章失败:', error)
  }
}

let barChart = null
let lineChart = null

const initBarChart = () => {
  const chartDom = document.getElementById('barChart')
  if (!chartDom) return
  
  barChart = echarts.init(chartDom)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['文章数', '问答数'],
      bottom: 0,
      textStyle: {
        color: '#6B7280'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months.value,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      axisLabel: {
        color: '#6B7280'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6B7280'
      },
      splitLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      }
    },
    series: [
      {
        name: '文章数',
        type: 'bar',
        data: chartData.value.articles,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3B82F6' },
            { offset: 1, color: '#60A5FA' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '问答数',
        type: 'bar',
        data: chartData.value.questions,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#F97316' },
            { offset: 1, color: '#FB923C' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
  
  barChart.setOption(option)
}

const initLineChart = () => {
  const chartDom = document.getElementById('lineChart')
  if (!chartDom) return
  
  lineChart = echarts.init(chartDom)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['用户数', '评论数'],
      bottom: 0,
      textStyle: {
        color: '#6B7280'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months.value,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      axisLabel: {
        color: '#6B7280'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6B7280'
      },
      splitLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      }
    },
    series: [
      {
        name: '用户数',
        type: 'line',
        smooth: true,
        data: chartData.value.users,
        lineStyle: {
          color: '#10B981',
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
          ])
        },
        itemStyle: {
          color: '#10B981'
        }
      },
      {
        name: '评论数',
        type: 'line',
        smooth: true,
        data: chartData.value.comments,
        lineStyle: {
          color: '#8B5CF6',
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.3)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.05)' }
          ])
        },
        itemStyle: {
          color: '#8B5CF6'
        }
      }
    ]
  }
  
  lineChart.setOption(option)
}

const handleResize = () => {
  barChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  // 先获取数据
  await Promise.all([
    fetchStatistics(),
    fetchTrends(),
    fetchRecentArticles()
  ])
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">首页</h1>
      <p class="text-sm text-gray-500 dark:text-gray-400">欢迎回来，管理员</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div
        v-for="stat in stats"
        :key="stat.name"
        class="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
      >
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 dark:text-gray-400">{{ stat.name }}</p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white mt-1">{{ stat.value }}</p>
            <p class="text-xs text-green-500 mt-1">{{ stat.change }} 较上月</p>
          </div>
          <div :class="[colorClasses[stat.color]]" class="w-12 h-12 rounded-lg flex items-center justify-center">
            <el-icon :size="24" class="text-white">
              <component :is="stat.icon" />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">文章与问答统计</h2>
        <div id="barChart" class="w-full h-80"></div>
      </div>
      
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">用户与评论统计</h2>
        <div id="lineChart" class="w-full h-80"></div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">最近文章</h2>
        </div>
        <div class="divide-y divide-gray-200 dark:divide-gray-700">
          <div
            v-for="article in recentArticles"
            :key="article.id"
            class="px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ article.title }}</p>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">{{ article.date }}</p>
              </div>
              <div class="flex items-center gap-3 ml-4">
                <span
                  :class="[
                    'px-2 py-1 text-xs rounded-full',
                    article.status === 'published'
                      ? 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400'
                      : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                  ]"
                >
                  {{ article.status === 'published' ? '已发布' : '草稿' }}
                </span>
                <span class="text-xs text-gray-500 dark:text-gray-400">{{ article.views }} 阅读</span>
              </div>
            </div>
          </div>
        </div>
        <div class="px-6 py-3 border-t border-gray-200 dark:border-gray-700">
          <RouterLink
            to="/articles"
            class="text-sm text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300 font-medium"
          >
            查看全部 →
          </RouterLink>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">快捷操作</h2>
        </div>
        <div class="p-6 grid grid-cols-2 gap-4">
          <RouterLink
            to="/categories"
            class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <div class="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
              <el-icon :size="20" class="text-blue-500">
                <Collection />
              </el-icon>
            </div>
            <span class="text-sm font-medium text-gray-900 dark:text-white">管理分类</span>
          </RouterLink>
          <RouterLink
            to="/tags"
            class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <div class="w-10 h-10 bg-orange-100 dark:bg-orange-900/30 rounded-lg flex items-center justify-center">
              <el-icon :size="20" class="text-orange-500">
                <PriceTag />
              </el-icon>
            </div>
            <span class="text-sm font-medium text-gray-900 dark:text-white">管理标签</span>
          </RouterLink>
          <RouterLink
            to="/comments"
            class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <div class="w-10 h-10 bg-green-100 dark:bg-green-900/30 rounded-lg flex items-center justify-center">
              <el-icon :size="20" class="text-green-500">
                <ChatDotRound />
              </el-icon>
            </div>
            <span class="text-sm font-medium text-gray-900 dark:text-white">审核评论</span>
          </RouterLink>
          <RouterLink
            to="/users"
            class="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <div class="w-10 h-10 bg-purple-100 dark:bg-purple-900/30 rounded-lg flex items-center justify-center">
              <el-icon :size="20" class="text-purple-500">
                <User />
              </el-icon>
            </div>
            <span class="text-sm font-medium text-gray-900 dark:text-white">用户管理</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
