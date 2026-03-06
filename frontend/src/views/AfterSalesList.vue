<template>
  <div class="after-sales-list">
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <h2>售后记录</h2>
      <el-button type="primary" size="small" @click="$router.push('/orders')">申请售后</el-button>
    </div>
    
    <el-tabs v-model="activeStatus" @tab-click="loadList">
      <el-tab-pane label="全部" name=""></el-tab-pane>
      <el-tab-pane label="处理中" name="PENDING"></el-tab-pane>
      <el-tab-pane label="已通过" name="APPROVED"></el-tab-pane>
      <el-tab-pane label="已拒绝" name="REJECTED"></el-tab-pane>
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
        <p v-if="currentDetail.auditRemark"><strong>审核备注：</strong>{{ currentDetail.auditRemark }}</p>
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
      currentDetail: null
    }
  },
  created() {
    this.loadList()
  },
  methods: {
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
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'COMPLETED': '已完成'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'COMPLETED': 'info'
      }
      return map[status] || ''
    },
    viewDetail(row) {
      this.currentDetail = row
      this.detailVisible = true
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
