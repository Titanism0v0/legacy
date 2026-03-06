<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    <div class="search-bar" style="margin-bottom: 20px;">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名、昵称或邮箱"
        style="width: 300px; margin-right: 10px;"
        @keyup.enter.native="handleSearch"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch" class="search-btn"></el-button>
      </el-input>
    </div>
    
    <el-table :data="userList" v-loading="loading" style="width: 100%">
      <el-table-column label="头像" width="70">
        <template slot-scope="scope">
          <Avatar :name="scope.row.nickname || scope.row.username" :src="scope.row.avatar" :size="32" />
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip></el-table-column>
      <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip></el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip></el-table-column>
      <el-table-column prop="phone" label="手机号" width="120"></el-table-column>
      <el-table-column prop="role" label="角色" width="100">
        <template slot-scope="scope">
          <el-tag :type="getRoleType(scope.row.role)" size="small">{{ getRoleText(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160"></el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" style="color: var(--danger-color);" @click="deleteUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-if="total > 0"
      @current-change="handlePageChange"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="text-align: center; margin-top: 20px;"
    ></el-pagination>
  </div>
</template>

<script>
import { userApi } from '../../api'
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'UserManage',
  components: {
    Avatar
  },
  data() {
    return {
      userList: [],
      loading: false,
      keyword: '',
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      this.loading = true
      try {
        const res = await userApi.getUserList({
          page: this.currentPage,
          size: this.pageSize,
          keyword: this.keyword
        })
        this.userList = res.data.records
        this.total = res.data.total
      } catch (error) {
        this.$message.error('加载用户失败')
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadUsers()
    },
    handleSearch() {
      this.currentPage = 1
      this.loadUsers()
    },
    async deleteUser(id) {
      try {
        await this.$confirm('确定要删除该用户吗？', '提示', {
          type: 'warning'
        })
        await userApi.deleteUser(id)
        this.$message.success('删除成功')
        this.loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    getRoleText(role) {
      const roleMap = {
        'USER': '普通用户',
        'SELLER': '卖家',
        'ADMIN': '管理员'
      }
      return roleMap[role] || role
    },
    getRoleType(role) {
      const typeMap = {
        'USER': '',
        'SELLER': 'success',
        'ADMIN': 'warning'
      }
      return typeMap[role] || ''
    }
  }
}
</script>

<style scoped>
/* Custom style for search button in input append slot */
/* Use deep selector to override Element UI default styles */
::v-deep .el-input-group__append {
  background-color: var(--input-bg-color) !important;
  border-color: var(--border-color) !important;
  padding: 0;
}

.search-btn {
  background-color: var(--input-bg-color) !important;
  color: var(--primary-color) !important;
  border: 1px solid var(--border-color) !important;
  border-left: none !important;
  border-radius: 0 4px 4px 0 !important;
}

.search-btn:hover {
  background-color: var(--primary-color-soft) !important;
}

.user-manage {
  padding: 20px;
}
</style>
