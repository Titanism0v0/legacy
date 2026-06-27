<template>
  <div class="community-page">
    <section class="hero">
      <div>
        <p class="eyebrow">Community Exchange</p>
        <h1>求购、出售、讨论都可以在这里发布</h1>
        <p class="hero-text">
          社区帖子延续现有交易系统结构，同时支持普通图文与一键智能美化后的分享型封面展示。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="goPublish">发布帖子</el-button>
        <el-button plain @click="loadPosts">刷新列表</el-button>
      </div>
    </section>

    <section class="toolbar">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索标题或正文"
        class="keyword-input"
        @keyup.enter.native="handleFilter"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleFilter" />
      </el-input>
      <el-select v-model="filters.postType" clearable placeholder="帖子类型" @change="handleFilter">
        <el-option v-for="item in postTypes" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.categoryId" clearable placeholder="分类" @change="handleFilter">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </section>

    <section v-loading="loading" class="post-grid">
      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        title="社区内容暂时无法加载，请稍后重试"
      />
      <article v-for="post in posts" :key="post.id" class="post-card" @click="openPost(post.id)">
        <div v-if="getPrimaryImage(post)" class="post-visual image-mode">
          <img :src="getPrimaryImage(post)" :alt="getDisplayTitle(post)">
        </div>
        <div v-else class="post-visual text-mode" :style="getTemplateStyle(post.coverTemplate)">
          <span class="visual-badge">{{ getPostTypeLabel(post.postType) }}</span>
          <h3>{{ getDisplayTitle(post) }}</h3>
          <p>{{ getDisplayExcerpt(post) }}</p>
        </div>

        <div class="post-body">
          <div class="post-meta">
            <el-tag size="mini" :type="getPostTypeTagType(post.postType)">{{ getPostTypeLabel(post.postType) }}</el-tag>
            <span>{{ post.categoryName || '未分类' }}</span>
          </div>
          <h3>{{ getDisplayTitle(post) }}</h3>
          <p class="post-excerpt">{{ getDisplayExcerpt(post) }}</p>
          <div class="post-footer">
            <div class="author-info">
              <div class="author-trigger" @click.stop="openChatWithUser(post.authorId)">
                <Avatar :src="post.authorAvatar" :name="post.authorNickname" :size="28" />
              </div>
              <div>
                <strong class="author-link" @click.stop="openChatWithUser(post.authorId)">
                  {{ post.authorNickname || `用户 #${post.authorId}` }}
                </strong>
                <span>{{ formatDate(post.createTime) }}</span>
              </div>
            </div>
            <div class="post-stats">
              <span>{{ post.commentCount || 0 }} 条评论</span>
              <el-button
                v-if="post.canDelete"
                type="text"
                class="danger-btn"
                @click.stop="removePost(post.id)"
              >删除</el-button>
            </div>
          </div>
        </div>
      </article>
      <el-empty v-if="!loading && !loadError && posts.length === 0" description="还没有符合条件的帖子" />
    </section>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
import { categoryApi, communityApi } from '@/api'
import Avatar from '@/components/Avatar.vue'
import { getTextImageTemplate } from '@/components/community/textImageTemplates'

export default {
  name: 'Community',
  components: { Avatar },
  data() {
    return {
      loading: false,
      posts: [],
      categories: [],
      currentPage: 1,
      pageSize: 9,
      total: 0,
      loadError: false,
      filters: {
        keyword: '',
        postType: '',
        categoryId: null
      },
      postTypes: [
        { value: 'WANTED', label: '求购帖' },
        { value: 'FOR_SALE', label: '出售帖' },
        { value: 'DISCUSSION', label: '讨论帖' }
      ]
    }
  },
  created() {
    this.loadCategories()
    this.loadPosts()
  },
  methods: {
    async loadCategories() {
      try {
        const res = await categoryApi.getAllCategories()
        this.categories = res.data || []
      } catch (e) {
        this.categories = []
      }
    },
    async loadPosts() {
      this.loading = true
      this.loadError = false
      try {
        const res = await communityApi.getPosts({
          page: this.currentPage,
          size: this.pageSize,
          keyword: this.filters.keyword || undefined,
          postType: this.filters.postType || undefined,
          categoryId: this.filters.categoryId || undefined
        })
        const data = res.data || res
        this.posts = data.records || []
        this.total = data.total || 0
      } catch (e) {
        this.posts = []
        this.total = 0
        this.loadError = true
        this.$message.error(e.message || '加载帖子失败')
      } finally {
        this.loading = false
      }
    },
    handleFilter() {
      this.currentPage = 1
      this.loadPosts()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadPosts()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    openChatWithUser(userId) {
      if (!userId) return
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login', () => {}, () => {})
        return
      }
      const currentUser = this.$store.state.user
      if (currentUser && Number(currentUser.id) === Number(userId)) {
        this.$message.info('不能与自己私聊')
        return
      }
      this.$router.push({ path: '/chat', query: { peerUserId: userId } }, () => {}, () => {})
    },
    goPublish() {
      if (!this.$store.getters.isAuthenticated) {
        this.$message.warning('请先登录后再发布帖子')
        this.$router.push('/login', () => {}, () => {})
        return
      }
      const userRole = this.$store.getters.userRole
      if (!['USER', 'SELLER'].includes(userRole)) {
        this.$message.warning('当前账号暂不支持发布帖子')
        return
      }
      this.$router.push('/community/publish', () => {}, () => {})
    },
    openPost(id) {
      this.$router.push(`/community/${id}`)
    },
    async removePost(id) {
      try {
        await this.$confirm('确定删除这篇帖子吗？', '提示', { type: 'warning' })
      } catch {
        return
      }
      try {
        await communityApi.deletePost(id)
        this.$message.success('帖子已删除')
        this.loadPosts()
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    },
    getPrimaryImage(post) {
      if (post.contentMode === 'TEXT_IMAGE' && post.coverImage) {
        return post.coverImage
      }
      if (post.images) {
        try {
          const parsed = typeof post.images === 'string' ? JSON.parse(post.images) : post.images
          if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
        } catch (e) {}
      }
      return post.coverImage || ''
    },
    parseRenderPayload(post) {
      if (!post || !post.renderPayload) return null
      try {
        return typeof post.renderPayload === 'string' ? JSON.parse(post.renderPayload) : post.renderPayload
      } catch (e) {
        return null
      }
    },
    getDisplayTitle(post) {
      const payload = this.parseRenderPayload(post)
      const title = payload && payload.layout && payload.layout.displayTitle
      return title || post.title || '社区帖子'
    },
    getDisplayExcerpt(post) {
      const payload = this.parseRenderPayload(post)
      const paragraphs = payload && payload.layout && Array.isArray(payload.layout.paragraphs)
        ? payload.layout.paragraphs.filter(Boolean)
        : []
      const fallback = paragraphs.length ? paragraphs.join(' ') : (post.content || '')
      if (!fallback) return '点击进入查看帖子详情'
      return fallback.length > 80 ? `${fallback.slice(0, 80)}...` : fallback
    },
    getPostTypeLabel(type) {
      return (
        {
          WANTED: '求购帖',
          FOR_SALE: '出售帖',
          DISCUSSION: '讨论帖'
        }[type] || type
      )
    },
    getPostTypeTagType(type) {
      return (
        {
          WANTED: 'warning',
          FOR_SALE: 'success',
          DISCUSSION: 'info'
        }[type] || ''
      )
    },
    getTemplateStyle(templateId) {
      const template = getTextImageTemplate(templateId)
      return { backgroundImage: template.preview }
    },
    formatDate(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}-${day}`
    }
  }
}
</script>

<style scoped>
.community-page {
  padding: 10px 0 30px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-end;
  padding: 28px 32px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(46, 204, 113, 0.2), transparent 28%),
    radial-gradient(circle at left center, rgba(241, 196, 15, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(13, 59, 102, 0.96), rgba(26, 82, 118, 0.92));
  color: #fff;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  opacity: 0.75;
}

.hero h1 {
  margin: 0 0 12px;
  font-size: 32px;
  line-height: 1.2;
}

.hero-text {
  max-width: 680px;
  margin: 0;
  line-height: 1.7;
  opacity: 0.9;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1.6fr) 180px 180px;
  gap: 14px;
  margin-bottom: 22px;
}

.keyword-input {
  width: 100%;
}

.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.post-card {
  overflow: hidden;
  border-radius: 24px;
  background: var(--card-bg-color);
  border: 1px solid rgba(127, 140, 141, 0.14);
  box-shadow: 0 16px 35px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.1);
}

.post-visual {
  position: relative;
  height: 220px;
}

.image-mode img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.text-mode {
  padding: 22px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background-size: cover;
  background-position: center;
}

.visual-badge {
  display: inline-flex;
  align-self: flex-start;
  margin-bottom: auto;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
  font-size: 12px;
}

.text-mode h3 {
  margin: 0 0 10px;
  font-size: 26px;
  line-height: 1.2;
}

.text-mode p {
  margin: 0;
  line-height: 1.7;
  opacity: 0.92;
}

.post-body {
  padding: 20px;
}

.post-meta,
.post-footer,
.author-info,
.post-stats {
  display: flex;
  align-items: center;
}

.post-meta {
  gap: 10px;
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.post-body h3 {
  margin: 0 0 10px;
  font-size: 21px;
  line-height: 1.35;
}

.post-excerpt {
  margin: 0 0 18px;
  line-height: 1.8;
  color: var(--text-secondary);
  min-height: 52px;
}

.post-footer {
  justify-content: space-between;
  gap: 14px;
}

.author-info {
  gap: 10px;
}

.author-trigger,
.author-link {
  cursor: pointer;
}

.author-link {
  transition: color 0.2s ease, text-decoration-color 0.2s ease;
  text-decoration: underline;
  text-decoration-color: transparent;
}

.author-link:hover {
  color: var(--primary-color);
  text-decoration-color: currentColor;
}

.author-info strong,
.author-info span,
.post-stats span {
  display: block;
}

.author-info strong {
  font-size: 14px;
}

.author-info span,
.post-stats span {
  font-size: 12px;
  color: var(--text-secondary);
}

.post-stats {
  gap: 10px;
}

.danger-btn {
  color: var(--danger-color);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
