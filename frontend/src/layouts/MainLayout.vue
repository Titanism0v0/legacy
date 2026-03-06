<template>
  <div class="main-layout">
    <el-header>
      <div class="header-content">
        <div class="logo" @click="$router.push('/home')">
          <h2>海外代购系统</h2>
        </div>
        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          router
          class="header-menu"
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item v-if="isAuthenticated && !isAdmin" index="/cart">购物车</el-menu-item>
          <el-menu-item v-if="isAuthenticated && !isAdmin" index="/orders">我的订单</el-menu-item>
          <el-menu-item v-if="isSeller && !isAdmin" index="/seller/products">我的商品</el-menu-item>
          <el-menu-item v-if="isSeller && !isAdmin" index="/seller/orders">售出订单</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/products">所有商品</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/orders">所有订单</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/users">用户管理</el-menu-item>
        </el-menu>
        <div class="user-info">
          <!-- 地区/货币选择 -->
          <el-select 
            v-model="currentCurrency" 
            placeholder="选择地区" 
            size="small" 
            class="currency-select"
            @change="handleCurrencyChange">
            <el-option
              v-for="c in currencies"
              :key="c.value"
              :label="c.label"
              :value="c.value">
            </el-option>
          </el-select>

          <template v-if="isAuthenticated">
            <el-dropdown @command="handleCommand">
              <span class="user-name-wrapper">
                <Avatar :name="user.nickname || user.username" :src="user.avatar" :size="32" />
                <span class="user-name">
                  {{ user.nickname || user.username }}
                  <i class="el-icon-arrow-down"></i>
                </span>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="address">收货地址</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="text" @click="$router.push('/login')">登录</el-button>
            <el-button type="text" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main>
      <router-view/>
    </el-main>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Avatar from '@/components/Avatar.vue'
import { currencies } from '@/utils/currency'

export default {
  name: 'MainLayout',
  components: {
    Avatar
  },
  data() {
    return {
      currencies,
      currentCurrency: this.$store.state.currency || 'CNH'
    }
  },
  computed: {
    ...mapGetters(['isAuthenticated', 'userRole']),
    user() {
      return this.$store.state.user
    },
    // Watch store currency changes (e.g. from login)
    storeCurrency() {
      return this.$store.state.currency
    },
    isSeller() {
      const role = this.userRole || (this.user && this.user.role)
      return role === 'SELLER' || role === 'ADMIN'
    },
    isAdmin() {
      const role = this.userRole || (this.user && this.user.role)
      return role === 'ADMIN'
    },
    activeMenu() {
      return this.$route.path
    }
  },
  mounted() {
    // 调试信息：检查用户角色
    console.log('MainLayout mounted - user:', this.user)
    console.log('MainLayout mounted - userRole:', this.userRole)
    console.log('MainLayout mounted - isSeller:', this.isSeller)
    console.log('MainLayout mounted - isAdmin:', this.isAdmin)
  },
  watch: {
    storeCurrency(newVal) {
      this.currentCurrency = newVal
    }
  },
  methods: {
    handleCurrencyChange(val) {
      this.$store.dispatch('setCurrency', val)
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.$store.dispatch('logout')
        this.$router.push('/login')
        this.$message.success('已退出登录')
      } else if (command === 'address') {
        if (this.$route.path !== '/address') {
          this.$router.push('/address')
        }
      } else if (command === 'profile') {
        if (this.$route.path !== '/profile') {
          this.$router.push('/profile')
        }
      }
    }
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: var(--bg-color);
  color: var(--text-color);
}

.el-header {
  background-color: var(--header-bg-color) !important;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-color);
  padding: 0;
  height: 60px !important;
}

.header-content {
  width: 95%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0;
}

.logo {
  cursor: pointer;
  margin-right: 40px;
  display: flex;
  align-items: center;
}

.logo h2 {
  color: var(--primary-color);
  margin: 0;
  font-weight: 600;
}

.header-menu {
  flex: 1;
  background-color: transparent !important;
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  color: var(--text-secondary) !important;
  border-bottom: none !important;
  background-color: transparent !important;
}

.header-menu .el-menu-item:hover,
.header-menu .el-menu-item.is-active,
.header-menu .el-menu-item:focus {
  color: var(--primary-color) !important;
  background-color: var(--primary-color-soft) !important;
}

.user-info {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.currency-select {
  width: 140px;
  margin-right: 20px;
}

.user-name-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-name-wrapper:hover {
  background-color: var(--primary-color-soft);
}

.user-name {
  color: var(--text-color);
  padding: 0 10px;
  display: flex;
  align-items: center;
}

.user-name-wrapper:hover .user-name {
  color: var(--primary-color);
}

.el-main {
  width: 95%;
  margin: 0 auto;
  padding: 20px 0;
  background: transparent;
}
</style>
