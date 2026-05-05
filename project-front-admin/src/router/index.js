import {createRouter, createWebHistory} from 'vue-router'
import AdminLayout from '../components/AdminLayout.vue'
import {isLoggedIn} from '../utils/request.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'articles',
        name: 'Articles',
        component: () => import('../views/ArticleList.vue')
      },
      {
        path: 'articles/:id',
        name: 'ArticleDetail',
        component: () => import('../views/ArticleDetail.vue')
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/UserList.vue')
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('../views/CommentList.vue')
      },
      {
        path: 'comments/:id',
        name: 'CommentDetail',
        component: () => import('../views/CommentDetail.vue')
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('../views/CategoryManage.vue')
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('../views/TagManage.vue')
      },
      {
        path: 'columns',
        name: 'Columns',
        component: () => import('../views/ColumnList.vue')
      },
      {
        path: 'columns/:id',
        name: 'ColumnDetail',
        component: () => import('../views/ColumnDetail.vue')
      },
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('../views/QuestionList.vue')
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('../views/NotificationManage.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/AdminProfile.vue')
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('../views/AdminLog.vue')
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('../views/KnowledgeManage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const loggedIn = isLoggedIn()
  
  if (to.meta.requiresAuth && !loggedIn) {
    next('/login')
  } else if (to.path === '/login' && loggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
