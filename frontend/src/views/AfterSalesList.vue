<template>
  <div class="after-sales-list">
    <div class="header-row">
      <h2>售后记录</h2>
      <el-button type="primary" size="small" @click="goOrders">申请售后</el-button>
    </div>

    <el-tabs v-model="activeStatus" @tab-click="loadList">
      <el-tab-pane label="全部" name=""></el-tab-pane>
      <el-tab-pane label="待处理" name="PENDING"></el-tab-pane>
      <el-tab-pane label="商家已拒绝" name="SELLER_REJECTED"></el-tab-pane>
      <el-tab-pane label="平台仲裁中" name="ADMIN_ARBITRATING"></el-tab-pane>
      <el-tab-pane label="已通过" name="APPROVED"></el-tab-pane>
      <el-tab-pane label="已拒绝" name="REJECTED"></el-tab-pane>
      <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
    </el-tabs>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="申请ID" width="90" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
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
      <el-table-column prop="reason" label="原因" min-width="120" />
      <el-table-column label="规则/AI" min-width="220">
        <template slot-scope="scope">
          <div>规则：{{ scope.row.ruleDecision || '-' }}</div>
          <div>AI：{{ scope.row.aiSuggestion || '-' }}（{{ scope.row.aiScore || '-' }}）</div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260">
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
      class="pagination"
    />

    <el-dialog title="售后详情" :visible.sync="detailVisible" width="780px">
      <div v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID">{{ currentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">{{ currentDetail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentDetail.type === 'REFUND_ONLY' ? '仅退款' : '退货退款' }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentDetail.amount }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(currentDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="责任方">{{ getResponsibilityText(currentDetail.responsibility) }}</el-descriptions-item>
          <el-descriptions-item label="规则判定">{{ currentDetail.ruleDecision || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规则理由">{{ currentDetail.ruleReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AI建议">{{ currentDetail.aiSuggestion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AI评分">{{ currentDetail.aiScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">{{ currentDetail.reason }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentDetail.description }}</el-descriptions-item>
          <el-descriptions-item label="证据文本" :span="2">{{ currentDetail.evidenceText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ currentDetail.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="仲裁结论" :span="2">{{ currentDetail.arbitrationResult || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="evidence-block" v-if="parseImages(currentDetail.evidenceUrls).length > 0">
          <h4>证据附件</h4>
          <div class="evidence-list">
            <a v-for="(url, idx) in parseImages(currentDetail.evidenceUrls)" :key="idx" :href="url" target="_blank">{{ url }}</a>
          </div>
        </div>

        <div class="log-block">
          <h4>处理日志</h4>
          <el-timeline>
            <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="formatTime(log.createTime)"
            >
              [{{ log.operatorRole }}] {{ log.action }} - {{ log.detail }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="sellerDecision === 'APPROVE' ? '同意售后' : '拒绝售后'" :visible.sync="sellerDecisionVisible" width="520px">
      <el-form label-position="top">
        <el-form-item :label="sellerDecision === 'APPROVE' ? '同意说明（可选）' : '拒绝原因（建议填写）'">
          <el-input type="textarea" v-model="sellerRemark" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="sellerDecisionVisible = false">取消</el-button>
        <el-button :type="sellerDecision === 'APPROVE' ? 'success' : 'danger'" @click="confirmSellerDecision">确认</el-button>
      </span>
    </el-dialog>

    <el-dialog title="平台仲裁" :visible.sync="adminArbitrateVisible" width="620px">
      <el-form label-position="top">
        <el-form-item label="责任方">
          <el-select v-model="adminResponsibility" placeholder="可选">
            <el-option label="买家" value="BUYER" />
            <el-option label="卖家" value="SELLER" />
            <el-option label="物流" value="LOGISTICS" />
            <el-option label="平台" value="PLATFORM" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="仲裁结论">
          <el-input type="textarea" v-model="adminResult" :rows="3" />
        </el-form-item>
        <el-form-item label="最终状态">
          <el-select v-model="adminFinalStatus" placeholder="不填默认为 APPROVED">
            <el-option label="默认(通过)" :value="''" />
            <el-option label="通过" value="APPROVED" />
            <el-option label="拒绝" value="REJECTED" />
            <el-option label="完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
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
      logs: [],
      sellerDecisionVisible: false,
      sellerDecisionRow: null,
      sellerDecision: 'APPROVE',
      sellerRemark: '',
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
      return this.$store?.state?.user?.role || null
    },
    canSellerApproveReject(status) {
      return this.getCurrentRole() === 'SELLER' && status === 'PENDING'
    },
    canUserRequestArbitration(status) {
      return this.getCurrentRole() === 'USER' && status === 'SELLER_REJECTED'
    },
    canAdminArbitrate(status) {
      return this.getCurrentRole() === 'ADMIN' && (status === 'ADMIN_ARBITRATING' || status === 'PENDING' || status === 'SELLER_RESPONDED')
    },
    goOrders() {
      const role = this.getCurrentRole()
      const target = role === 'ADMIN' ? '/admin/orders' : (role === 'SELLER' ? '/seller/orders' : '/orders')
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
        this.list = res.data.records || []
        this.total = res.data.total || 0
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
        PENDING: '待处理',
        SELLER_REJECTED: '商家已拒绝',
        ADMIN_ARBITRATING: '平台仲裁中',
        SELLER_RESPONDED: '商家已响应',
        APPROVED: '已通过',
        REJECTED: '已拒绝',
        COMPLETED: '已完成'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        PENDING: 'warning',
        SELLER_REJECTED: 'danger',
        ADMIN_ARBITRATING: 'warning',
        SELLER_RESPONDED: 'info',
        APPROVED: 'success',
        REJECTED: 'danger',
        COMPLETED: 'success'
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
      return map[code] || '-'
    },
    async viewDetail(row) {
      this.currentDetail = row
      this.detailVisible = true
      this.logs = []
      try {
        const logRes = await afterSalesApi.getLogs(row.id)
        this.logs = logRes.data || []
      } catch (e) {}
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
        this.$message.success('处理成功')
        this.sellerDecisionVisible = false
        this.loadList()
      } catch (e) {
        this.$message.error(e.message || '提交失败')
      }
    },
    async confirmRequestArbitration(row) {
      try {
        await this.$confirm('确认申请平台介入吗？', '提示', {
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
      if (!jsonStr) return []
      try {
        const parsed = JSON.parse(jsonStr)
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return []
      }
    },
    formatTime(v) {
      if (!v) return ''
      return new Date(v).toLocaleString()
    }
  }
}
</script>

<style scoped>
.after-sales-list {
  padding: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.evidence-block,
.log-block {
  margin-top: 16px;
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
