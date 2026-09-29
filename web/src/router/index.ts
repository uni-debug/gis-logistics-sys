import { createRouter, createWebHistory } from 'vue-router'
import type { Role } from '@/types'
import userLogin from '@/pages/user/Login.vue'
import userHome from '@/pages/user/Home.vue'
import userOrders from '@/pages/user/Orders.vue'
import userOrderDetail from '@/pages/user/OrderDetail.vue'
import userService from '@/pages/user/Service.vue'
import userNotices from '@/pages/user/Notices.vue'
import userProfile from '@/pages/user/Profile.vue'
import staffHome from '@/pages/staff/Home.vue'
import adminHome from '@/pages/admin/Home.vue'
import userRegister from '@/pages/user/Register.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
        { path: '/user/login', name: 'user-login', component: userLogin },
    { path: '/user/register', name: 'user-register', component: userRegister },
    { path: '/user', name: 'user-home', component: userHome, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/user/orders', name: 'user-orders', component: userOrders, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/user/orders/:id', name: 'user-order-detail', component: userOrderDetail, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/user/service', name: 'user-service', component: userService, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/user/notices', name: 'user-notices', component: userNotices, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/user/profile', name: 'user-profile', component: userProfile, meta: { role: 'USER' as Role, requiresAuth: true } },
    { path: '/staff', name: 'staff-home', component: staffHome, meta: { role: 'STAFF' as Role, requiresAuth: true } },
    { path: '/admin', name: 'admin-home', component: adminHome, meta: { role: 'ADMIN' as Role, requiresAuth: true } },
    { path: '/', redirect: '/user/login' },
  ],
})

router.beforeEach((to) => {
  const requiresAuth = to.meta.requiresAuth as boolean | undefined
  const token = localStorage.getItem('token')
  if (requiresAuth && !token) {
    return { name: 'user-login' }
  }
  // Role-based route guard (only applies to routes that declare a required role)
  const requiredRole = to.meta.role as Role | undefined
  if (requiredRole) {
    const role = localStorage.getItem('role') as Role | null
    if (role !== requiredRole) {
      // Not logged in yet, or wrong role -> send to login (no loop: login has no meta.role)
      return { name: 'user-login' }
    }
  }
  return true
})

export { router }

