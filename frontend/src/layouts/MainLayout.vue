<template>
  <div class="main-layout">
    <el-header>
      <div class="header-content">
        <div class="logo" @click="$router.push('/home')">
          <h2>海外代购系统</h2>
        </div>

        <el-menu mode="horizontal" :default-active="activeMenu" router class="header-menu">
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/crossborder-guide">跨境说明</el-menu-item>
          <el-menu-item index="/community">社区</el-menu-item>

          <el-menu-item v-if="isBuyer" index="/cart">购物车</el-menu-item>
          <el-menu-item v-if="isBuyer" index="/orders">我的订单</el-menu-item>
          <el-menu-item v-if="showChatNav" index="/chat">
            <el-badge :value="chatUnreadTotal" :hidden="chatUnreadTotal === 0" :max="99">
              <span>消息</span>
            </el-badge>
          </el-menu-item>

          <el-menu-item v-if="isSeller && !isAdmin" index="/seller/overview">销售总览</el-menu-item>
          <el-menu-item v-if="isSeller && !isAdmin" index="/seller/products">我的商品</el-menu-item>
          <el-menu-item v-if="isSeller && !isAdmin" index="/seller/orders">售出订单</el-menu-item>
          <el-menu-item v-if="isAuthenticated" index="/after-sales/list">{{ isAdmin ? '售后管理' : '售后记录' }}</el-menu-item>

          <el-menu-item v-if="isAdmin" index="/admin/workbench">管理工作台</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/products">所有商品</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/orders">所有订单</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/users">用户管理</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/community">社区管理</el-menu-item>
        </el-menu>

        <div class="user-info">
          <el-button
            type="text"
            class="theme-switch"
            @click="toggleTheme"
            :title="isDarkMode ? '切换到浅色主题' : '切换到深色主题'"
          >
            <i :class="isDarkMode ? 'el-icon-sunny' : 'el-icon-moon'" />
          </el-button>

          <el-select
            v-model="currentCurrency"
            placeholder="选择币种"
            size="small"
            class="currency-select"
            @change="handleCurrencyChange"
          >
            <el-option v-for="c in currencies" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>

          <template v-if="isAuthenticated">
            <el-dropdown @command="handleCommand">
              <span class="user-name-wrapper">
                <Avatar :name="user.nickname || user.username" :src="user.avatar" :size="32" />
                <span class="user-name">
                  {{ user.nickname || user.username }}
                  <i class="el-icon-arrow-down" />
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
      <router-view />
    </el-main>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import { currencies } from '@/utils/currency'
import Avatar from '@/components/Avatar.vue'
import { connect, onMessage, offMessage, disconnect } from '@/utils/chatSocket'

export default {
  name: 'MainLayout',
  components: { Avatar },
  data() {
    return {
      currencies,
      currentCurrency: this.$store.state.currency || 'CNH',
      isDarkMode: localStorage.getItem('theme') !== 'light',
      _chatPollTimer: null,
      _onSocketMessage: null
    }
  },
  computed: {
    ...mapGetters(['isAuthenticated', 'userRole']),
    ...mapState(['chatUnreadTotal']),
    user() {
      return this.$store.state.user || {}
    },
    isAdmin() {
      return this.userRole === 'ADMIN'
    },
    isBuyer() {
      return this.isAuthenticated && this.userRole === 'USER'
    },
    isSeller() {
      return this.userRole === 'SELLER' || this.userRole === 'ADMIN'
    },
    showChatNav() {
      return this.isAuthenticated && !this.isAdmin && (this.userRole === 'USER' || this.userRole === 'SELLER')
    },
    activeMenu() {
      if (this.$route.path.startsWith('/community')) return '/community'
      if (this.$route.path.startsWith('/admin/community')) return '/admin/community'
      return this.$route.path
    }
  },
  watch: {
    showChatNav(val) {
      if (val) this.setupChatUnread()
      else this.teardownChatUnread()
    },
    '$store.state.currency'(v) {
      this.currentCurrency = v
    }
  },
  mounted() {
    this.applyTheme()
    this.setupChatUnread()
  },
  beforeDestroy() {
    this.teardownChatUnread()
  },
  methods: {
    handleCurrencyChange(currency) {
      this.$store.dispatch('setCurrency', currency)
    },
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      this.applyTheme()
    },
    applyTheme() {
      const theme = this.isDarkMode ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', theme)
      localStorage.setItem('theme', theme)
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.teardownChatUnread()
        this.$store.dispatch('logout')
        this.$router.push('/login')
        this.$message.success('已退出登录')
        return
      }
      if (command === 'address') {
        this.$router.push('/address')
        return
      }
      if (command === 'profile') {
        this.$router.push('/profile')
      }
    },
    setupChatUnread() {
      if (!this.showChatNav || this._onSocketMessage) {
        return
      }
      this.$store.dispatch('refreshChatUnread').catch(() => {})
      connect()
      this._onSocketMessage = (msg) => {
        const uid = this.user && this.user.id
        if (!msg || msg.type !== 'CHAT' || !uid) return
        if (Number(msg.toUserId) !== Number(uid)) return
        this.$store.dispatch('refreshChatUnread').catch(() => {})
      }
      onMessage(this._onSocketMessage)
      this._chatPollTimer = setInterval(() => {
        this.$store.dispatch('refreshChatUnread').catch(() => {})
      }, 60000)
    },
    teardownChatUnread() {
      if (this._onSocketMessage) {
        offMessage(this._onSocketMessage)
        this._onSocketMessage = null
      }
      if (this._chatPollTimer) {
        clearInterval(this._chatPollTimer)
        this._chatPollTimer = null
      }
      disconnect()
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
}

.logo {
  cursor: pointer;
  margin-right: 24px;
}

.logo h2 {
  margin: 0;
  color: var(--primary-color);
}

.header-menu {
  flex: 1;
  background-color: transparent !important;
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  color: var(--text-secondary) !important;
}

.header-menu .el-menu-item.is-active,
.header-menu .el-menu-item:hover {
  color: var(--primary-color) !important;
}

.user-info {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.theme-switch {
  margin-right: 12px;
  font-size: 20px;
  color: var(--text-color) !important;
}

.theme-switch:hover,
.theme-switch:focus {
  color: var(--primary-color) !important;
}

.currency-select {
  width: 140px;
  margin-right: 16px;
}

.user-name-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-name {
  padding-left: 8px;
}

.el-main {
  width: 95%;
  margin: 0 auto;
  padding: 20px 0;
}
</style>
