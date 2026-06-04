import {createApp} from 'vue'
import {createRouter, createWebHistory} from 'vue-router'
import {createPinia} from 'pinia'
import App from './App.vue'
import './style.css'
import {clearAuth, isLoggedIn} from './utils/request.js'
import {marked} from 'marked'

// 全局 Markdown 配置（仅设置一次，避免多组件重复调用）
marked.setOptions({ breaks: true, gfm: true })

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('./views/Home.vue')
  },
  {
    path: '/blog',
    name: 'Blog',
    component: () => import('./views/Blog.vue')
  },
  {
    path: '/article/:id',
    name: 'Article',
    component: () => import('./views/Article.vue')
  },
  {
    path: '/ai-chat',
    name: 'AIChat',
    component: () => import('./views/AIChat.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('./views/SearchResults.vue')
  },
  {
    path: '/mine',
    name: 'Mine',
    component: () => import('./views/Mine.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/edit',
    name: 'EditProfile',
    component: () => import('./views/EditProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/mine/analytics',
    name: 'UserAnalytics',
    component: () => import('./views/UserAnalytics.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('./views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/create-article',
    name: 'CreateArticle',
    component: () => import('./views/CreateArticle.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-writing',
    name: 'AIWriting',
    component: () => import('./views/AIWriting.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/writing-tasks',
    name: 'WritingTasks',
    component: () => import('./views/WritingTasks.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserHome',
    component: () => import('./views/UserHome.vue')
  },
  {
    path: '/browse-history',
    name: 'BrowseHistory',
    component: () => import('./views/BrowseHistory.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/columns',
    name: 'ColumnList',
    component: () => import('./views/ColumnList.vue')
  },
  {
    path: '/column/:id',
    name: 'ColumnDetail',
    component: () => import('./views/ColumnDetail.vue')
  },
  {
    path: '/my-columns',
    name: 'MyColumns',
    component: () => import('./views/MyColumns.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/column/:id/articles',
    name: 'ColumnArticleManage',
    component: () => import('./views/ColumnArticleManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/points',
    name: 'Points',
    component: () => import('./views/Points.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/points/rank',
    name: 'PointsRank',
    component: () => import('./views/PointsRank.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/points/logs',
    name: 'PointsLogs',
    component: () => import('./views/PointsLogs.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-coupons',
    name: 'MyCoupons',
    component: () => import('./views/MyCoupons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/coupon-zone',
    name: 'CouponZone',
    component: () => import('./views/CouponZone.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

/**
 * 路由守卫：校验登录状态 + JWT 过期时间
 */
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    if (!isLoggedIn()) {
      next({ name: 'Home', query: { login: 'true', redirect: to.fullPath } })
      return
    }
    // 轻量级 JWT 过期校验（无需请求后端）
    const token = localStorage.getItem('token')
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        if (payload.exp && payload.exp * 1000 < Date.now()) {
          clearAuth()
          next({ name: 'Home', query: { login: 'true', redirect: to.fullPath } })
          return
        }
      } catch {
        // Token 格式异常，视为过期
        clearAuth()
        next({ name: 'Home', query: { login: 'true', redirect: to.fullPath } })
        return
      }
    }
  }
  next()
})

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 全局错误处理：捕获未被组件处理的异常
app.config.errorHandler = (err, instance, info) => {
  console.error('[全局异常]', err, info)
}

app.mount('#app')
