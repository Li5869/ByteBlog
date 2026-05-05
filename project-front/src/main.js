import {createApp} from 'vue'
import {createRouter, createWebHistory} from 'vue-router'
import {createPinia} from 'pinia'
import App from './App.vue'
import './style.css'
import {isLoggedIn} from './utils/request.js'

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
    path: '/qa',
    name: 'QA',
    component: () => import('./views/QA.vue')
  },
  {
    path: '/question/:id',
    name: 'QuestionDetail',
    component: () => import('./views/QuestionDetail.vue')
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
    path: '/create-question',
    name: 'CreateQuestion',
    component: () => import('./views/CreateQuestion.vue'),
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

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    next({ name: 'Home', query: { login: 'true', redirect: to.fullPath } })
  } else {
    next()
  }
})

const app = createApp(App)
app.use(createPinia())
app.use(router)

app.mount('#app')
