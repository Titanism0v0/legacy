import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'crossborder-guide',
        name: 'CrossBorderGuide',
        component: () => import('../views/CrossBorderGuide.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'seller/overview',
        name: 'SellerOverview',
        component: () => import('../views/seller/Overview.vue'),
        meta: { requiresAuth: true, role: ['SELLER'] }
      },
      {
        path: 'seller/products',
        name: 'SellerProducts',
        component: () => import('../views/seller/ProductManage.vue'),
        meta: { requiresAuth: true, role: ['SELLER'] }
      },
      {
        path: 'seller/orders',
        name: 'SellerOrders',
        component: () => import('../views/seller/OrderManage.vue'),
        meta: { requiresAuth: true, role: ['SELLER'] }
      },
      {
        path: 'seller/:id',
        name: 'SellerDetail',
        component: () => import('../views/SellerDetail.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/Cart.vue'),
        meta: { requiresAuth: true, role: ['USER'] }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/Orders.vue'),
        meta: { requiresAuth: true, role: ['USER'] }
      },
      {
        path: 'address',
        name: 'Address',
        component: () => import('../views/Address.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('../views/Payment.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/UserProfile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'after-sales/apply',
        name: 'AfterSalesApply',
        component: () => import('../views/AfterSalesApply.vue'),
        meta: { requiresAuth: true, role: ['USER', 'SELLER'] }
      },
      {
        path: 'after-sales/list',
        name: 'AfterSalesList',
        component: () => import('../views/AfterSalesList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'service',
        name: 'ServiceSupport',
        component: () => import('../views/ServiceSupport.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'admin/products',
        name: 'AdminProducts',
        component: () => import('../views/admin/ProductManage.vue'),
        meta: { requiresAuth: true, role: ['ADMIN'] }
      },
      {
        path: 'admin/orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/OrderManage.vue'),
        meta: { requiresAuth: true, role: ['ADMIN'] }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { requiresAuth: true, role: ['ADMIN'] }
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = store.state.token
  const user = store.state.user
  
  // 调试信息
  if (to.meta.role) {
    console.log('路由守卫 - 目标路由需要角色:', to.meta.role)
    console.log('路由守卫 - 当前用户:', user)
    console.log('路由守卫 - 用户角色:', user ? user.role : 'null')
  }
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.role) {
    // 检查用户角色
    const userRole = user ? user.role : null
    if (!user || !userRole || !to.meta.role.includes(userRole)) {
      console.log('路由守卫 - 权限不足，跳转到首页')
      next('/home')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
