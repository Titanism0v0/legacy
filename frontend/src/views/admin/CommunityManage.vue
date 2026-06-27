<template>
  <div class="admin-community">
    <h2>社区审核</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="帖子审核" name="posts">
        <div class="toolbar">
          <el-input
            v-model="postFilters.keyword"
            clearable
            placeholder="搜索标题、正文、命中原因"
            class="toolbar-input"
            @keyup.enter.native="loadPosts"
          />
          <el-select v-model="postFilters.status" clearable placeholder="审核状态" @change="loadPosts">
            <el-option label="待复审" value="PENDING_REVIEW" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已拦截" value="REJECTED" />
          </el-select>
          <el-select v-model="postFilters.postType" clearable placeholder="帖子类型" @change="loadPosts">
            <el-option label="求购帖" value="WANTED" />
            <el-option label="出售帖" value="FOR_SALE" />
            <el-option label="讨论帖" value="DISCUSSION" />
          </el-select>
          <el-button type="primary" @click="loadPosts">查询</el-button>
          <el-button @click="resetPostFilters">重置</el-button>
        </div>

        <el-alert
          title="列表已按风险优先排序，风险高或命中敏感词的帖子会高亮显示。"
          type="info"
          :closable="false"
          show-icon
          class="risk-hint"
        />

        <el-table :data="posts" v-loading="postsLoading" :row-class-name="resolvePostRowClass">
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="authorNickname" label="作者" width="120" />
          <el-table-column prop="postType" label="类型" width="100">
            <template slot-scope="scope">{{ getPostTypeLabel(scope.row.postType) }}</template>
          </el-table-column>
          <el-table-column prop="contentMode" label="模式" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.contentMode === 'TEXT_IMAGE' ? 'success' : 'info'">
                {{ scope.row.contentMode === 'TEXT_IMAGE' ? '文转图' : '普通帖' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="status" label="审核状态" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="statusTagType(scope.row.status)">
                {{ statusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险" width="140">
            <template slot-scope="scope">
              <el-tag size="mini" :type="riskTagType(scope.row.riskLevel)">
                {{ riskLabel(scope.row.riskLevel) }}
              </el-tag>
              <div class="score-line">分数: {{ formatScore(scope.row.aiScore) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="aiReason" label="命中原因" min-width="260" show-overflow-tooltip>
            <template slot-scope="scope">
              <span class="reason-text">{{ shortReason(scope.row.aiReason) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="commentCount" label="评论数" width="80" />
          <el-table-column prop="createTime" label="发布时间" min-width="160" />
          <el-table-column label="操作" width="260" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="viewPostDetail(scope.row.id)">查看</el-button>
              <el-button
                v-if="scope.row.status === 'PENDING_REVIEW'"
                type="text"
                size="small"
                class="approve-btn"
                @click="auditPost(scope.row, 'APPROVE')"
              >
                通过
              </el-button>
              <el-button
                v-if="scope.row.status === 'PENDING_REVIEW'"
                type="text"
                size="small"
                class="reject-btn"
                @click="auditPost(scope.row, 'REJECT')"
              >
                驳回
              </el-button>
              <el-button type="text" size="small" class="danger-btn" @click="deletePost(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager" v-if="postTotal > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :current-page.sync="postPage"
            :page-size="pageSize"
            :total="postTotal"
            @current-change="loadPosts"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="评论管理" name="comments">
        <div class="toolbar">
          <el-input
            v-model="commentFilters.keyword"
            clearable
            placeholder="搜索评论"
            class="toolbar-input"
            @keyup.enter.native="loadComments"
          />
          <el-input
            v-model="commentFilters.postId"
            clearable
            placeholder="帖子 ID（可选）"
            class="toolbar-input small"
          />
          <el-button type="primary" @click="loadComments">查询</el-button>
        </div>

        <el-table :data="comments" v-loading="commentsLoading">
          <el-table-column prop="postId" label="帖子 ID" width="90" />
          <el-table-column prop="authorNickname" label="作者" width="120" />
          <el-table-column prop="replyToNickname" label="回复对象" width="140" />
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="createTime" label="发布时间" min-width="160" />
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" class="danger-btn" @click="deleteComment(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager" v-if="commentTotal > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :current-page.sync="commentPage"
            :page-size="pageSize"
            :total="commentTotal"
            @current-change="loadComments"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { communityApi } from '@/api'

export default {
  name: 'AdminCommunityManage',
  data() {
    return {
      activeTab: 'posts',
      pageSize: 10,
      postsLoading: false,
      commentsLoading: false,
      posts: [],
      comments: [],
      postPage: 1,
      commentPage: 1,
      postTotal: 0,
      commentTotal: 0,
      postFilters: {
        keyword: '',
        postType: '',
        status: 'PENDING_REVIEW'
      },
      commentFilters: {
        keyword: '',
        postId: ''
      }
    }
  },
  created() {
    this.loadPosts()
    this.loadComments()
  },
  methods: {
    async loadPosts() {
      this.postsLoading = true
      try {
        const res = await communityApi.adminGetPosts({
          page: this.postPage,
          size: this.pageSize,
          keyword: this.postFilters.keyword || undefined,
          postType: this.postFilters.postType || undefined,
          status: this.postFilters.status || undefined
        })
        const data = res.data || res
        this.posts = [...(data.records || [])].sort(this.comparePosts)
        this.postTotal = data.total || 0
      } catch (e) {
        this.$message.error(e.message || '加载帖子失败')
      } finally {
        this.postsLoading = false
      }
    },
    async loadComments() {
      this.commentsLoading = true
      try {
        const res = await communityApi.adminGetComments({
          page: this.commentPage,
          size: this.pageSize,
          keyword: this.commentFilters.keyword || undefined,
          postId: this.commentFilters.postId || undefined
        })
        const data = res.data || res
        this.comments = data.records || []
        this.commentTotal = data.total || 0
      } catch (e) {
        this.$message.error(e.message || '加载评论失败')
      } finally {
        this.commentsLoading = false
      }
    },
    resetPostFilters() {
      this.postFilters.keyword = ''
      this.postFilters.postType = ''
      this.postFilters.status = 'PENDING_REVIEW'
      this.postPage = 1
      this.loadPosts()
    },
    viewPostDetail(postId) {
      this.$router.push(`/admin/community/${postId}`)
    },
    comparePosts(a, b) {
      const statusWeight = this.getStatusWeight(b.status) - this.getStatusWeight(a.status)
      if (statusWeight !== 0) return statusWeight
      const riskWeight = this.getRiskWeight(b.riskLevel) - this.getRiskWeight(a.riskLevel)
      if (riskWeight !== 0) return riskWeight
      const scoreA = Number(a.aiScore || 0)
      const scoreB = Number(b.aiScore || 0)
      if (scoreB !== scoreA) return scoreB - scoreA
      const timeA = new Date(a.createTime || 0).getTime()
      const timeB = new Date(b.createTime || 0).getTime()
      return timeB - timeA
    },
    getStatusWeight(status) {
      return { PENDING_REVIEW: 3, REJECTED: 2, PUBLISHED: 1 }[status] || 0
    },
    getRiskWeight(level) {
      return { HIGH: 3, MEDIUM: 2, LOW: 1 }[level] || 0
    },
    statusLabel(status) {
      return { PENDING_REVIEW: '待复审', REJECTED: '已拦截', PUBLISHED: '已发布' }[status] || (status || '-')
    },
    statusTagType(status) {
      return { PENDING_REVIEW: 'warning', REJECTED: 'danger', PUBLISHED: 'success' }[status] || 'info'
    },
    riskLabel(level) {
      return { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[level] || '未知'
    },
    riskTagType(level) {
      return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[level] || 'info'
    },
    formatScore(score) {
      if (score === undefined || score === null || score === '') return '-'
      const num = Number(score)
      return Number.isNaN(num) ? '-' : num.toFixed(3)
    },
    shortReason(reason) {
      if (!reason) return '-'
      if (reason.length <= 120) return reason
      return `${reason.slice(0, 120)}...`
    },
    resolvePostRowClass({ row }) {
      if (row.status === 'REJECTED') return 'row-rejected'
      if (row.status === 'PENDING_REVIEW' && row.riskLevel === 'HIGH') return 'row-high-risk'
      if (row.status === 'PENDING_REVIEW') return 'row-pending'
      return ''
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
    async auditPost(row, action) {
      let remark = ''
      if (action === 'APPROVE') {
        try {
          await this.$confirm('确认通过该帖子吗？', '审核通过', { type: 'warning' })
        } catch {
          return
        }
      } else {
        try {
          const { value } = await this.$prompt('请输入驳回原因（可选）', '驳回帖子', {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            inputPlaceholder: '例如：命中敏感词'
          })
          remark = value || ''
        } catch {
          return
        }
      }

      try {
        await communityApi.adminAuditPost(row.id, { action, remark })
        this.$message.success(action === 'APPROVE' ? '审核通过成功' : '驳回成功')
        this.loadPosts()
      } catch (e) {
        this.$message.error(e.message || '审核失败')
      }
    },
    async deletePost(id) {
      try {
        await this.$confirm('确定删除这篇帖子吗？', '提示', { type: 'warning' })
      } catch {
        return
      }
      try {
        await communityApi.adminDeletePost(id)
        this.$message.success('帖子已删除')
        this.loadPosts()
        this.loadComments()
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    },
    async deleteComment(id) {
      try {
        await this.$confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
      } catch {
        return
      }
      try {
        await communityApi.adminDeleteComment(id)
        this.$message.success('评论已删除')
        this.loadComments()
        this.loadPosts()
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    }
  }
}
</script>

<style scoped>
.admin-community {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-input {
  width: 280px;
}

.toolbar-input.small {
  width: 180px;
}

.risk-hint {
  margin-bottom: 12px;
}

.score-line {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.reason-text {
  color: var(--text-secondary);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.danger-btn,
.reject-btn {
  color: var(--danger-color);
}

.approve-btn {
  color: #67c23a;
}

::v-deep .el-table .row-high-risk > td {
  background: #fff1f0;
}

::v-deep .el-table .row-pending > td {
  background: #fffbe6;
}

::v-deep .el-table .row-rejected > td {
  background: #fff2f0;
}
</style>
