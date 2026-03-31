<template>
  <div class="legal-page">
    <el-card class="legal-card" v-loading="loading">
      <div class="title-row">
        <h2>{{ docTitle }}</h2>
        <el-tag size="small" type="info">{{ versionText }}</el-tag>
      </div>

      <p class="hint">以下内容用于注册前阅读确认，最终解释以官方法律法规与平台公示版本为准。</p>

      <div v-for="(item, index) in sections" :key="index" class="section-item">
        <span class="index">{{ index + 1 }}.</span>
        <span>{{ item }}</span>
      </div>

      <div class="action-row">
        <el-link type="primary" :href="'/legal/sources'" target="_blank">查看官方依据</el-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import { legalApi } from '../api'

export default {
  name: 'LegalDoc',
  data() {
    return {
      loading: false,
      legal: null
    }
  },
  computed: {
    type() {
      return this.$route.params.type === 'privacy' ? 'privacy' : 'terms'
    },
    clause() {
      if (!this.legal || !this.legal.clauses) return null
      return this.legal.clauses[this.type] || null
    },
    docTitle() {
      if (this.clause && this.clause.title) return this.clause.title
      return this.type === 'privacy' ? '隐私政策' : '用户协议/责任边界'
    },
    sections() {
      if (this.clause && Array.isArray(this.clause.sections)) return this.clause.sections
      return []
    },
    versionText() {
      if (!this.legal) return '版本: -'
      const v = this.type === 'privacy' ? this.legal.privacyVersion : this.legal.termsVersion
      return `版本: ${v || '-'}`
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const res = await legalApi.getCurrent()
        this.legal = res.data || {}
      } catch (e) {
        this.$message.error('加载条款失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.legal-page {
  max-width: 900px;
  margin: 32px auto;
  padding: 0 16px;
}

.legal-card {
  border-radius: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.title-row h2 {
  margin: 0;
}

.hint {
  color: var(--text-secondary);
  margin-bottom: 18px;
}

.section-item {
  display: flex;
  gap: 8px;
  line-height: 1.8;
  margin-bottom: 6px;
}

.index {
  color: var(--primary-color);
  min-width: 20px;
}

.action-row {
  margin-top: 18px;
}
</style>
