<template>
  <div class="community-detail" v-loading="loading">
    <el-button type="text" icon="el-icon-arrow-left" @click="$router.push(communityListPath)">返回社区</el-button>

    <div v-if="post" class="detail-grid">
      <article class="post-panel">
        <header class="post-header">
          <div class="author-block">
            <div class="avatar-trigger" @click.stop="openChatWithUser(post.authorId)">
              <Avatar :src="post.authorAvatar" :name="post.authorNickname" :size="54" />
            </div>
            <div>
              <h1>{{ post.title }}</h1>
              <div class="author-meta">
                <span class="author-link" @click.stop="openChatWithUser(post.authorId)">
                  {{ post.authorNickname || `用户 #${post.authorId}` }}
                </span>
                <span>{{ formatDate(post.createTime) }}</span>
                <span>{{ post.categoryName || '未分类' }}</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-tag :type="getPostTypeTagType(post.postType)">{{ getPostTypeLabel(post.postType) }}</el-tag>
            <el-button v-if="post.canDelete" type="text" class="danger-btn" @click="removePost">删除帖子</el-button>
          </div>
        </header>

        <div v-if="isTextImagePost && post.coverImage" class="text-image-panel">
          <img :src="post.coverImage" :alt="post.title" class="text-image-main">
          <div v-if="textImageMeta.tags.length || textImageMeta.emoji" class="text-image-meta">
            <span v-if="textImageMeta.emoji" class="emoji-pill">{{ textImageMeta.emoji }}</span>
            <el-tag v-for="tag in textImageMeta.tags" :key="tag" size="small" effect="plain">#{{ tag }}</el-tag>
          </div>
        </div>

        <template v-else>
          <div v-if="gallery.length" class="gallery">
            <img v-for="(image, index) in gallery" :key="`${image}-${index}`" :src="image" :alt="post.title">
          </div>
          <div
            v-else-if="post.coverImage"
            class="cover-card"
            :style="{ backgroundImage: `linear-gradient(rgba(8,15,25,.18), rgba(8,15,25,.55)), url(${post.coverImage})` }"
          >
            <span>{{ getPostTypeLabel(post.postType) }}</span>
            <h2>{{ post.title }}</h2>
            <p>{{ post.content }}</p>
          </div>

          <div class="post-content">{{ post.content }}</div>
        </template>

        <div class="cta-bar">
          <el-button type="primary" @click="contactAuthor">{{ contactButtonText }}</el-button>
          <el-button plain @click="$router.push(communityListPath)">继续逛社区</el-button>
        </div>
      </article>

      <aside class="comment-panel">
        <div class="comment-head">
          <h2>评论区</h2>
          <span>{{ comments.length }} 条一级评论</span>
        </div>

        <div class="comment-editor">
          <div v-if="replyingToText" class="replying-banner">
            正在回复 {{ replyingToText }}
            <el-button type="text" @click="cancelReply">取消</el-button>
          </div>
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            placeholder="说点什么吧"
          />
          <div class="comment-actions">
            <el-button type="primary" @click="submitComment">发表评论</el-button>
          </div>
        </div>

        <div class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-card">
            <div class="comment-main">
              <div class="avatar-trigger" @click.stop="openChatWithUser(comment.authorId)">
                <Avatar :src="comment.authorAvatar" :name="comment.authorNickname" :size="36" />
              </div>
              <div class="comment-body">
                <div class="comment-top">
                  <strong class="author-link" @click.stop="openChatWithUser(comment.authorId)">
                    {{ comment.authorNickname || `用户 #${comment.authorId}` }}
                  </strong>
                  <span>{{ formatDate(comment.createTime) }}</span>
                </div>
                <p>{{ comment.content }}</p>
                <div class="comment-links">
                  <el-button type="text" @click="startReply(comment, comment.authorNickname)">回复</el-button>
                  <el-button
                    v-if="comment.canDelete"
                    type="text"
                    class="danger-btn"
                    @click="removeComment(comment)"
                  >删除</el-button>
                </div>
              </div>
            </div>

            <div v-if="comment.replies && comment.replies.length" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="avatar-trigger" @click.stop="openChatWithUser(reply.authorId)">
                  <Avatar :src="reply.authorAvatar" :name="reply.authorNickname" :size="30" />
                </div>
                <div class="reply-body">
                  <div class="comment-top">
                    <strong class="author-link" @click.stop="openChatWithUser(reply.authorId)">
                      {{ reply.authorNickname || `用户 #${reply.authorId}` }}
                    </strong>
                    <span>{{ formatDate(reply.createTime) }}</span>
                  </div>
                  <p>
                    <template v-if="reply.replyToNickname">
                      回复
                      <span
                        :class="['reply-name', { 'reply-link': !!reply.replyToUserId }]"
                        @click.stop="openChatWithUser(reply.replyToUserId)"
                      >{{ reply.replyToNickname }}</span>：
                    </template>
                    {{ reply.content }}
                  </p>
                  <div class="comment-links">
                    <el-button type="text" @click="startReply(comment, reply.authorNickname, reply.authorId)">回复</el-button>
                    <el-button
                      v-if="reply.canDelete"
                      type="text"
                      class="danger-btn"
                      @click="removeComment(reply)"
                    >删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-if="!comments.length" description="还没有评论，来抢个沙发" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { communityApi } from '@/api'
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'CommunityDetail',
  components: { Avatar },
  data() {
    return {
      loading: false,
      post: null,
      comments: [],
      commentForm: {
        content: ''
      },
      replyState: {
        parentId: null,
        replyToUserId: null,
        replyToName: ''
      }
    }
  },
  computed: {
    postId() {
      return this.$route.params.id
    },
    communityListPath() {
      return this.$route.path.startsWith('/admin/') ? '/admin/community' : '/community'
    },
    gallery() {
      if (!this.post || !this.post.images || this.post.contentMode === 'TEXT_IMAGE') return []
      try {
        const parsed = typeof this.post.images === 'string' ? JSON.parse(this.post.images) : this.post.images
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return []
      }
    },
    isAuthenticated() {
      return this.$store.getters.isAuthenticated
    },
    isTextImagePost() {
      return this.post && this.post.contentMode === 'TEXT_IMAGE'
    },
    textImageMeta() {
      if (!this.post || !this.post.renderPayload) {
        return { tags: [], emoji: '' }
      }
      try {
        const parsed = typeof this.post.renderPayload === 'string'
          ? JSON.parse(this.post.renderPayload)
          : this.post.renderPayload
        const tags = parsed && parsed.layout && Array.isArray(parsed.layout.chips)
          ? parsed.layout.chips.filter(Boolean)
          : []
        const emojis = parsed && parsed.analysis && Array.isArray(parsed.analysis.recommendedEmoji)
          ? parsed.analysis.recommendedEmoji.filter(Boolean)
          : []
        return {
          tags,
          emoji: emojis[0] || ''
        }
      } catch (e) {
        return { tags: [], emoji: '' }
      }
    },
    contactButtonText() {
      if (!this.isAuthenticated) return '登录后联系作者'
      return '联系作者'
    },
    replyingToText() {
      return this.replyState.replyToName ? this.replyState.replyToName : ''
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const [postRes, commentsRes] = await Promise.all([
          communityApi.getPost(this.postId),
          communityApi.getComments(this.postId)
        ])
        this.post = postRes.data || postRes
        this.comments = commentsRes.data || commentsRes || []
      } catch (e) {
        this.$message.error(e.message || '加载帖子失败')
        this.$router.push(this.communityListPath)
      } finally {
        this.loading = false
      }
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
    formatDate(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    },
    openChatWithUser(userId) {
      if (!userId) return
      if (!this.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      const currentUser = this.$store.state.user
      if (currentUser && Number(currentUser.id) === Number(userId)) {
        this.$message.info('不能与自己私聊')
        return
      }
      this.$router.push({ path: '/chat', query: { peerUserId: userId } })
    },
    async submitComment() {
      if (!this.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      if (!this.commentForm.content.trim()) {
        this.$message.warning('请输入评论内容')
        return
      }
      try {
        await communityApi.createComment({
          postId: Number(this.postId),
          parentId: this.replyState.parentId,
          replyToUserId: this.replyState.replyToUserId,
          content: this.commentForm.content.trim()
        })
        this.$message.success('评论成功')
        this.commentForm.content = ''
        this.cancelReply()
        this.loadData()
      } catch (e) {
        this.$message.error(e.message || '评论失败')
      }
    },
    startReply(rootComment, replyToName, replyToUserId) {
      if (!this.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      this.replyState.parentId = rootComment.id
      this.replyState.replyToUserId = replyToUserId || rootComment.authorId
      this.replyState.replyToName = replyToName || rootComment.authorNickname
    },
    cancelReply() {
      this.replyState = {
        parentId: null,
        replyToUserId: null,
        replyToName: ''
      }
    },
    async removePost() {
      try {
        await this.$confirm('确定删除这篇帖子吗？', '提示', { type: 'warning' })
      } catch {
        return
      }
      try {
        await communityApi.deletePost(this.post.id)
        this.$message.success('帖子已删除')
        this.$router.push(this.communityListPath)
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    },
    async removeComment(comment) {
      try {
        await this.$confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
      } catch {
        return
      }
      try {
        await communityApi.deleteComment(comment.id)
        this.$message.success('评论已删除')
        this.loadData()
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    },
    contactAuthor() {
      if (!this.post || !this.post.authorId) return
      this.openChatWithUser(this.post.authorId)
    }
  }
}
</script>

<style scoped>
.community-detail {
  padding-top: 4px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.95fr);
  gap: 22px;
  margin-top: 12px;
}

.post-panel,
.comment-panel {
  background: var(--card-bg-color);
  border-radius: 28px;
  border: 1px solid rgba(127, 140, 141, 0.14);
  box-shadow: 0 16px 35px rgba(0, 0, 0, 0.06);
}

.post-panel {
  padding: 28px;
}

.comment-panel {
  padding: 24px;
  height: fit-content;
}

.post-header,
.author-block,
.header-actions,
.comment-head,
.comment-main,
.reply-item,
.comment-top,
.comment-links,
.author-meta,
.cta-bar {
  display: flex;
}

.post-header,
.comment-head {
  justify-content: space-between;
  gap: 14px;
}

.author-block {
  gap: 14px;
}

.author-block h1 {
  margin: 0 0 10px;
  font-size: 30px;
  line-height: 1.25;
}

.author-meta {
  flex-wrap: wrap;
  gap: 8px 14px;
  color: var(--text-secondary);
  font-size: 13px;
}

.avatar-trigger,
.author-link,
.reply-link {
  cursor: pointer;
}

.author-link,
.reply-link {
  transition: color 0.2s ease, text-decoration-color 0.2s ease;
  text-decoration: underline;
  text-decoration-color: transparent;
}

.author-link:hover,
.reply-link:hover {
  color: var(--primary-color);
  text-decoration-color: currentColor;
}

.header-actions {
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin: 24px 0;
}

.gallery img,
.cover-card {
  border-radius: 22px;
}

.gallery img {
  width: 100%;
  height: 240px;
  object-fit: cover;
}

.text-image-panel {
  margin: 24px 0 0;
}

.text-image-main {
  width: 100%;
  display: block;
  border-radius: 24px;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.12);
}

.text-image-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.emoji-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  padding: 0 14px;
  height: 32px;
  border-radius: 999px;
  background: rgba(39, 174, 96, 0.1);
  font-size: 20px;
}

.cover-card {
  min-height: 360px;
  margin: 24px 0;
  padding: 28px;
  color: #fff;
  background-size: cover;
  background-position: center;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.cover-card span {
  align-self: flex-start;
  margin-bottom: auto;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.cover-card h2 {
  margin: 0 0 12px;
  font-size: 32px;
}

.cover-card p,
.post-content,
.comment-body p,
.reply-body p {
  line-height: 1.85;
}

.post-content {
  margin-top: 24px;
  font-size: 15px;
  white-space: pre-wrap;
}

.cta-bar {
  gap: 12px;
  margin-top: 26px;
}

.comment-head {
  align-items: center;
  margin-bottom: 18px;
}

.comment-head h2 {
  margin: 0;
}

.comment-head span,
.comment-top span {
  color: var(--text-secondary);
  font-size: 12px;
}

.comment-editor {
  padding: 18px;
  border-radius: 22px;
  background: rgba(39, 174, 96, 0.06);
  margin-bottom: 18px;
}

.replying-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.comment-actions {
  text-align: right;
  margin-top: 10px;
}

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(127, 140, 141, 0.14);
  background: rgba(255, 255, 255, 0.55);
}

.comment-main,
.reply-item {
  gap: 12px;
}

.comment-body,
.reply-body {
  flex: 1;
}

.comment-top {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-body p,
.reply-body p {
  margin: 8px 0 10px;
}

.comment-links {
  gap: 14px;
}

.reply-list {
  margin-top: 14px;
  padding-left: 14px;
  border-left: 2px solid rgba(127, 140, 141, 0.16);
  display: grid;
  gap: 12px;
}

.reply-name,
.danger-btn {
  color: var(--danger-color);
}

@media (max-width: 1000px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
