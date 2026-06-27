<template>
  <div class="admin-layout">
    <el-header class="admin-header">
      <div class="admin-header-inner">
        <div class="admin-title-group">
          <div class="admin-badge">ADMIN</div>
          <div>
            <div class="admin-title">{{ pageTitle }}</div>
            <div class="admin-subtitle">平台运营工作台</div>
          </div>
        </div>

        <div class="admin-actions">
          <el-button
            size="small"
            class="ghost-button"
            :disabled="isWorkbench"
            @click="goToWorkbench"
          >
            工作台
          </el-button>

          <el-button
            v-if="showBackToWorkbench"
            size="small"
            class="ghost-button"
            @click="goToWorkbench"
          >
            返回工作台
          </el-button>

          <el-button
            type="text"
            class="theme-switch"
            @click="toggleTheme"
            :title="isDarkMode ? '切换到浅色主题' : '切换到深色主题'"
          >
            <i :class="isDarkMode ? 'el-icon-sunny' : 'el-icon-moon'" />
          </el-button>

          <div class="admin-user">
            <Avatar :name="user.nickname || user.username" :src="user.avatar" :size="32" />
            <div class="admin-user-meta">
              <div class="admin-user-name">{{ user.nickname || user.username || '管理员' }}</div>
              <div class="admin-user-role">管理员</div>
            </div>
          </div>

          <el-button size="small" class="ghost-button" @click="goToProfile">个人中心</el-button>
          <el-button size="small" type="danger" @click="logout">退出登录</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="admin-main">
      <router-view />
    </el-main>
  </div>
</template>

<script>
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'AdminLayout',
  components: { Avatar },
  data() {
    return {
      isDarkMode: localStorage.getItem('theme') !== 'light'
    }
  },
  computed: {
    user() {
      return this.$store.state.user || {}
    },
    pageTitle() {
      return this.$route.meta && this.$route.meta.title ? this.$route.meta.title : '管理后台'
    },
    isWorkbench() {
      return this.$route.path === '/admin/workbench'
    },
    showBackToWorkbench() {
      return this.$route.path.startsWith('/admin/') && !this.isWorkbench
    }
  },
  mounted() {
    this.applyTheme()
  },
  methods: {
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      this.applyTheme()
    },
    applyTheme() {
      const theme = this.isDarkMode ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', theme)
      localStorage.setItem('theme', theme)
    },
    goToWorkbench() {
      if (!this.isWorkbench) {
        this.$router.push('/admin/workbench')
      }
    },
    goToProfile() {
      this.$router.push('/admin/profile')
    },
    logout() {
      this.$store.dispatch('logout')
      this.$router.push('/login')
      this.$message.success('已退出登录')
    }
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(51, 117, 224, 0.16), transparent 28%),
    linear-gradient(180deg, var(--bg-color) 0%, var(--bg-color) 100%);
  color: var(--text-color);
}

.admin-header {
  height: 72px !important;
  padding: 0;
  border-bottom: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--header-bg-color) 92%, transparent) !important;
  backdrop-filter: blur(14px);
}

.admin-header-inner {
  width: 95%;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--primary-color-soft);
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-title {
  color: var(--text-color);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.admin-subtitle {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.theme-switch {
  font-size: 20px;
  color: var(--text-color) !important;
}

.theme-switch:hover,
.theme-switch:focus {
  color: var(--primary-color) !important;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--card-bg-color) 92%, transparent);
}

.admin-user-meta {
  line-height: 1.2;
}

.admin-user-name {
  color: var(--text-color);
  font-size: 13px;
  font-weight: 600;
}

.admin-user-role {
  color: var(--text-secondary);
  font-size: 11px;
}

.ghost-button {
  background: transparent !important;
}

.admin-main {
  width: 95%;
  margin: 0 auto;
  padding: 20px 0;
}

@media (max-width: 960px) {
  .admin-header {
    height: auto !important;
  }

  .admin-header-inner {
    padding: 14px 0;
    flex-direction: column;
    align-items: stretch;
  }

  .admin-actions {
    flex-wrap: wrap;
  }

  .admin-user {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
