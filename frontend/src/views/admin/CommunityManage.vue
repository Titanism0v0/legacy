<template>
  <div class="admin-community">
    <h2>社区管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="帖子管理" name="posts">
        <div class="toolbar">
          <el-input
            v-model="postFilters.keyword"
            clearable
            placeholder="搜索帖子"
            class="toolbar-input"
            @keyup.enter.native="loadPosts"
          />
          <el-select v-model="postFilters.postType" clearable placeholder="帖子类型" @change="loadPosts">
            <el-option label="求购帖" value="WANTED" />
            <el-option label="出售帖" value="FOR_SALE" />
            <el-option label="讨论帖" value="DISCUSSION" />
          </el-select>
          <el-button type="primary" @click="loadPosts">查询</el-button>
        </div>

        <el-table :data="posts" v-loading="postsLoading">
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="authorNickname" label="作者" width="120" />
          <el-table-column prop="postType" label="类型" width="110">
            <template slot-scope="scope">{{ getPostTypeLabel(scope.row.postType) }}</template>
          </el-table-column>
          <el-table-column prop="contentMode" label="模式" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.contentMode === 'TEXT_IMAGE' ? 'success' : 'info'">
                {{ scope.row.contentMode === 'TEXT_IMAGE' ? '文转图' : '普通帖' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="commentCount" label="评论数" width="90" />
          <el-table-column prop="createTime" label="发布时间" min-width="160" />
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="$router.push(`/community/${scope.row.id}`)">查看</el-button>
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
        postType: ''
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
          postType: this.postFilters.postType || undefined
        })
        const data = res.data || res
        this.posts = data.records || []
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
    getPostTypeLabel(type) {
      return (
        {
          WANTED: '求购帖',
          FOR_SALE: '出售帖',
          DISCUSSION: '讨论帖'
        }[type] || type
      )
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
}

.toolbar-input {
  width: 260px;
}

.toolbar-input.small {
  width: 180px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.danger-btn {
  color: var(--danger-color);
}
</style>
