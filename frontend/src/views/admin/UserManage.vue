<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名、昵称或邮箱"
        style="width: 320px; margin-right: 10px;"
        @keyup.enter.native="handleSearch"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
      </el-input>
    </div>

    <el-table :data="userList" v-loading="loading" style="width: 100%">
      <el-table-column label="头像" width="70">
        <template slot-scope="scope">
          <Avatar :name="scope.row.nickname || scope.row.username" :src="scope.row.avatar" :size="32" />
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="role" label="角色" width="100">
        <template slot-scope="scope">
          <el-tag :type="getRoleType(scope.row.role)" size="small">{{ getRoleText(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="kycStatus" label="商家资料" width="130">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.role === 'SELLER'" :type="getKycType(scope.row.kycStatus)" size="small">
            {{ scope.row.kycStatus || 'UNSUBMITTED' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.role === 'SELLER'"
            type="text"
            size="small"
            @click="viewKyc(scope.row)"
          >查看资料</el-button>
          <el-button
            v-if="scope.row.role === 'SELLER' && scope.row.kycStatus === 'PENDING'"
            type="text"
            size="small"
            style="color: var(--success-color);"
            @click="auditKyc(scope.row, 'APPROVE')"
          >通过</el-button>
          <el-button
            v-if="scope.row.role === 'SELLER' && scope.row.kycStatus === 'PENDING'"
            type="text"
            size="small"
            style="color: var(--danger-color);"
            @click="auditKyc(scope.row, 'REJECT')"
          >拒绝</el-button>
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
      class="pagination"
    />

    <el-dialog title="商家资料详情" :visible.sync="kycDialogVisible" width="720px">
      <div v-if="currentSeller">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商家">{{ currentSeller.nickname || currentSeller.username }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ currentSeller.kycStatus || 'UNSUBMITTED' }}</el-descriptions-item>
          <el-descriptions-item label="身份证明">
            <a v-if="kycDetail.identityDocUrl" :href="kycDetail.identityDocUrl" target="_blank">{{ kycDetail.identityDocUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="收款码">
            <div v-if="kycDetail.paymentQrUrl">
              <a :href="kycDetail.paymentQrUrl" target="_blank">{{ kycDetail.paymentQrUrl }}</a>
              <img :src="kycDetail.paymentQrUrl" class="payment-preview" />
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="货源说明">{{ kycDetail.sourceDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ currentSeller.kycRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <span slot="footer">
        <el-button @click="kycDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { userApi } from '../../api'
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'UserManage',
  components: { Avatar },
  data() {
    return {
      userList: [],
      loading: false,
      keyword: '',
      currentPage: 1,
      pageSize: 10,
      total: 0,
      kycDialogVisible: false,
      currentSeller: null,
      kycDetail: {
        identityDocUrl: '',
        paymentQrUrl: '',
        sourceDescription: ''
      }
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
        this.userList = res.data.records || []
        this.total = res.data.total || 0
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
        await this.$confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
        await userApi.deleteUser(id)
        this.$message.success('删除成功')
        this.loadUsers()
      } catch (error) {
        if (error !== 'cancel') this.$message.error(error.message || '删除失败')
      }
    },
    viewKyc(row) {
      this.currentSeller = row
      this.kycDetail = { identityDocUrl: '', paymentQrUrl: '', sourceDescription: '' }
      if (row.kycFiles) {
        try {
          const parsed = JSON.parse(row.kycFiles)
          this.kycDetail.identityDocUrl = parsed.identityDocUrl || ''
          this.kycDetail.paymentQrUrl = parsed.paymentQrUrl || ''
          this.kycDetail.sourceDescription = parsed.sourceDescription || ''
        } catch (e) {}
      }
      this.kycDialogVisible = true
    },
    async auditKyc(row, action) {
      let remark = ''
      try {
        const { value } = await this.$prompt(
          action === 'APPROVE' ? '请输入通过备注（可选）' : '请输入拒绝原因（建议填写）',
          action === 'APPROVE' ? '通过审核' : '拒绝审核',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '审核备注'
          }
        )
        remark = value || ''
      } catch (e) {
        return
      }

      try {
        await userApi.auditKyc({
          userId: row.id,
          action,
          remark
        })
        this.$message.success('审核成功')
        this.loadUsers()
      } catch (e) {
        this.$message.error(e.message || '审核失败')
      }
    },
    getRoleText(role) {
      const roleMap = {
        USER: '普通用户',
        SELLER: '商家',
        ADMIN: '管理员'
      }
      return roleMap[role] || role
    },
    getRoleType(role) {
      const typeMap = {
        USER: '',
        SELLER: 'success',
        ADMIN: 'warning'
      }
      return typeMap[role] || ''
    },
    getKycType(status) {
      if (status === 'APPROVED') return 'success'
      if (status === 'REJECTED') return 'danger'
      if (status === 'PENDING') return 'warning'
      return 'info'
    }
  }
}
</script>

<style scoped>
.user-manage {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
}

.pagination {
  text-align: center;
  margin-top: 20px;
}

.payment-preview {
  display: block;
  margin-top: 12px;
  max-width: 220px;
  border: 1px solid var(--border-color);
  padding: 8px;
  background: #fff;
}
</style>

