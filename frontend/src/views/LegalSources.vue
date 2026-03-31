<template>
  <div class="legal-page">
    <el-card class="legal-card" v-loading="loading">
      <h2>法律真实性官方依据</h2>
      <p class="hint">以下链接均来自中国官方站点，注册前建议阅读；条款解释以最新官方公布内容为准。</p>

      <el-table :data="references" border>
        <el-table-column prop="title" label="法规名称" min-width="260" />
        <el-table-column prop="agency" label="发布机关" width="140" />
        <el-table-column prop="publishDate" label="发布时间" width="120" />
        <el-table-column label="来源域名" width="170">
          <template slot-scope="scope">{{ scope.row.domain || '-' }}</template>
        </el-table-column>
        <el-table-column label="官方链接" width="120" align="center">
          <template slot-scope="scope">
            <el-link type="primary" :href="scope.row.url" target="_blank">打开</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { legalApi } from '../api'

export default {
  name: 'LegalSources',
  data() {
    return {
      loading: false,
      references: []
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
        const data = res.data || {}
        this.references = data.officialReferences || []
      } catch (e) {
        this.$message.error('加载官方依据失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.legal-page {
  max-width: 980px;
  margin: 32px auto;
  padding: 0 16px;
}

.legal-card {
  border-radius: 12px;
}

.hint {
  color: var(--text-secondary);
  margin: 10px 0 16px;
}
</style>
