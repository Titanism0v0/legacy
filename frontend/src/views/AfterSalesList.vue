<template>
  <div class="after-sales-list">
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <h2>售后记录</h2>
      <el-button type="primary" size="small" @click="goOrders">申请售后</el-button>
    </div>
    
    <el-tabs v-model="activeStatus" @tab-click="loadList">
      <el-tab-pane label="全部" name=""></el-tab-pane>
      <el-tab-pane label="待卖家处理" name="PENDING"></el-tab-pane>
      <el-tab-pane label="卖家已拒绝" name="SELLER_REJECTED"></el-tab-pane>
      <el-tab-pane label="平台仲裁中" name="ADMIN_ARBITRATING"></el-tab-pane>
      <el-tab-pane label="已通过" name="APPROVED"></el-tab-pane>
      <el-tab-pane label="已拒绝" name="REJECTED"></el-tab-pane>
      <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
    </el-tabs>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="申请ID" width="100"></el-table-column>
      <el-table-column prop="orderId" label="关联订单ID" width="100"></el-table-column>
      <el-table-column prop="type" label="类型" width="120">
        <template slot-scope="scope">
          {{ scope.row.type === 'REFUND_ONLY' ? '仅退款' : '退货退款' }}
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="退款金额" width="120">
        <template slot-scope="scope">
          ¥{{ scope.row.amount }}
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" width="150"></el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="180">
        <template slot-scope="scope">
          {{ new Date(scope.row.createTime).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button
            v-if="canSellerApproveReject(scope.row.status)"
            size="small"
            type="success"
            @click="openSellerDecision(scope.row, 'APPROVE')"
          >
            同意
          </el-button>
          <el-button
            v-if="canSellerApproveReject(scope.row.status)"
            size="small"
            type="danger"
            @click="openSellerDecision(scope.row, 'REJECT')"
          >
            拒绝
          </el-button>
          <el-button
            v-if="canUserRequestArbitration(scope.row.status)"
            size="small"
            type="warning"
            @click="confirmRequestArbitration(scope.row)"
          >
            申请平台介入
          </el-button>
          <el-button
            v-if="canAdminArbitrate(scope.row.status)"
            size="small"
            type="warning"
            @click="openAdminArbitrate(scope.row)"
          >
            仲裁
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @current-change="handlePageChange"
      :current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; text-align: center;">
    </el-pagination>

    <!-- 详情弹窗 -->
    <el-dialog title="售后详情" :visible.sync="detailVisible" width="500px">
      <div v-if="currentDetail">
        <p><strong>申请ID：</strong>{{ currentDetail.id }}</p>
        <p><strong>类型：</strong>{{ currentDetail.type === 'REFUND_ONLY' ? '仅退款' : '退货退款' }}</p>
        <p><strong>金额：</strong>¥{{ currentDetail.amount }}</p>
        <p><strong>原因：</strong>{{ currentDetail.reason }}</p>
        <p><strong>描述：</strong>{{ currentDetail.description }}</p>
        <p><strong>状态：</strong>{{ getStatusText(currentDetail.status) }}</p>
        <p v-if="currentDetail.sellerId"><strong>卖家ID：</strong>{{ currentDetail.sellerId }}</p>
        <p v-if="currentDetail.auditRemark"><strong>审核备注：</strong>{{ currentDetail.auditRemark }}</p>
        <p v-if="currentDetail.responsibility"><strong>责任方：</strong>{{ getResponsibilityText(currentDetail.responsibility) }}</p>
        <p v-if="currentDetail.arbitrationResult"><strong>仲裁结论：</strong>{{ currentDetail.arbitrationResult }}</p>
        <div v-if="currentDetail.images">
          <p><strong>凭证：</strong></p>
          <div style="display: flex; gap: 10px; flex-wrap: wrap;">
            <img 
              v-for="(img, index) in parseImages(currentDetail.images)" 
              :key="index" 
              :src="img" 
              style="width: 100px; height: 100px; object-fit: cover; border: 1px solid #eee;"
            />
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <!-- 卖家处理弹窗 -->
    <el-dialog :title="sellerDecision === 'APPROVE' ? '同意售后' : '拒绝售后'" :visible.sync="sellerDecisionVisible" width="520px">
      <div v-if="sellerDecisionRow">
        <p><strong>申请ID：</strong>{{ sellerDecisionRow.id }}</p>
        <el-form label-position="top">
          <el-form-item :label="sellerDecision === 'APPROVE' ? '同意说明（可选）' : '拒绝原因（建议填写）'">
            <el-input
              type="textarea"
              v-model="sellerRemark"
              :rows="4"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="sellerDecisionVisible = false">取消</el-button>
        <el-button :type="sellerDecision === 'APPROVE' ? 'success' : 'danger'" @click="confirmSellerDecision">
          {{ sellerDecision === 'APPROVE' ? '确认同意' : '确认拒绝' }}
        </el-button>
      </span>
    </el-dialog>

    <!-- 平台仲裁弹窗 -->
    <el-dialog title="平台仲裁售后" :visible.sync="adminArbitrateVisible" width="620px">
      <div v-if="adminArbitrateRow">
        <p><strong>申请ID：</strong>{{ adminArbitrateRow.id }}</p>
        <el-form label-position="top">
          <el-form-item label="责任方（可选）">
            <el-select v-model="adminResponsibility" placeholder="可选，不填则不记录">
              <el-option label="买家" value="BUYER" />
              <el-option label="卖家" value="SELLER" />
              <el-option label="物流" value="LOGISTICS" />
              <el-option label="平台" value="PLATFORM" />
              <el-option label="未知" value="UNKNOWN" />
            </el-select>
          </el-form-item>
          <el-form-item label="仲裁结论（可选）">
            <el-input
              type="textarea"
              v-model="adminResult"
              :rows="3"
              placeholder="请输入仲裁结论或补充说明"
            />
          </el-form-item>
          <el-form-item label="最终状态">
            <el-select v-model="adminFinalStatus" placeholder="不填默认通过（APPROVED）">
              <el-option label="不填（默认通过）" :value="''" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="adminArbitrateVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmAdminArbitrate">提交仲裁</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { afterSalesApi } from '../api'

export default {
  name: 'AfterSalesList',
  data() {
    return {
      activeStatus: '',
      list: [],
      total: 0,
      page: 1,
      size: 10,
      loading: false,
      detailVisible: false,
      currentDetail: null,

      // 卖家处理（同意/拒绝）
      sellerDecisionVisible: false,
      sellerDecisionRow: null,
      sellerDecision: 'APPROVE',
      sellerRemark: '',

      // 平台仲裁
      adminArbitrateVisible: false,
      adminArbitrateRow: null,
      adminResponsibility: '',
      adminResult: '',
      adminFinalStatus: ''
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    getCurrentRole() {
      const user = this.$store && this.$store.state ? this.$store.state.user : null
      return user ? user.role : null
    },
    canSellerApproveReject(status) {
      const role = this.getCurrentRole()
      return role === 'SELLER' && status === 'PENDING'
    },
    canUserRequestArbitration(status) {
      const role = this.getCurrentRole()
      return role === 'USER' && status === 'SELLER_REJECTED'
    },
    canAdminArbitrate(status) {
      const role = this.getCurrentRole()
      return role === 'ADMIN' && status === 'ADMIN_ARBITRATING'
    },
    goOrders() {
      const role = (this.$store && this.$store.state && this.$store.state.user) ? this.$store.state.user.role : null
      const target = role === 'ADMIN' ? '/admin/orders' : '/orders'
      this.$router.push(target).catch(() => {})
    },
    async loadList() {
      this.loading = true
      try {
        const res = await afterSalesApi.getList({
          page: this.page,
          size: this.size,
          status: this.activeStatus
        })
        this.list = res.data.records
        this.total = res.data.total
      } catch (error) {
        this.$message.error('加载列表失败')
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.page = page
      this.loadList()
    },
    getStatusText(status) {
      const map = {
        'PENDING': '待卖家处理',
        'SELLER_REJECTED': '卖家已拒绝',
        'ADMIN_ARBITRATING': '平台仲裁中',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'COMPLETED': '已完成'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        'PENDING': 'warning',
        'SELLER_REJECTED': 'danger',
        'ADMIN_ARBITRATING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'COMPLETED': 'info'
      }
      return map[status] || ''
    },
    getResponsibilityText(code) {
      const map = {
        BUYER: '买家',
        SELLER: '卖家',
        LOGISTICS: '物流',
        PLATFORM: '平台',
        UNKNOWN: '未知'
      }
      return map[code] || code
    },
    viewDetail(row) {
      this.currentDetail = row
      this.detailVisible = true
    },
    openSellerDecision(row, decision) {
      this.sellerDecisionRow = row
      this.sellerDecision = decision
      this.sellerRemark = ''
      this.sellerDecisionVisible = true
    },
    async confirmSellerDecision() {
      if (!this.sellerDecisionRow) return
      try {
        await afterSalesApi.sellerDecision({
          id: this.sellerDecisionRow.id,
          decision: this.sellerDecision,
          remark: this.sellerRemark
        })
        this.$message.success(this.sellerDecision === 'APPROVE' ? '已同意' : '已拒绝')
        this.sellerDecisionVisible = false
        this.loadList()
      } catch (e) {
        this.$message.error(e.message || '提交失败')
      }
    },
    async confirmRequestArbitration(row) {
      try {
        await this.$confirm('确定要申请平台介入吗？申请后将进入平台仲裁流程。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (_) {
        return
      }
      try {
        await afterSalesApi.requestArbitration({ id: row.id })
        this.$message.success('已申请平台介入')
        this.loadList()
      } catch (e) {
        this.$message.error(e.message || '申请失败')
      }
    },
    openAdminArbitrate(row) {
      this.adminArbitrateRow = row
      this.adminResponsibility = ''
      this.adminResult = ''
      this.adminFinalStatus = ''
      this.adminArbitrateVisible = true
    },
    async confirmAdminArbitrate() {
      if (!this.adminArbitrateRow) return
      try {
        await afterSalesApi.arbitrate({
          id: this.adminArbitrateRow.id,
          responsibility: this.adminResponsibility,
          result: this.adminResult,
          finalStatus: this.adminFinalStatus
        })
        this.$message.success('仲裁提交成功')
        this.adminArbitrateVisible = false
        this.loadList()
      } catch (e) {
        this.$message.error(e.message || '仲裁失败')
      }
    },
    parseImages(jsonStr) {
      try {
        return JSON.parse(jsonStr) || []
      } catch (e) {
        return []
      }
    }
  }
}
</script>

<style scoped>
.after-sales-list {
  padding: 20px;
}
</style>
