<template>
  <div class="admin-order-manage">
    <h2>订单管理</h2>
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name=""></el-tab-pane>
      <el-tab-pane label="待付款" name="PENDING_PAYMENT"></el-tab-pane>
      <el-tab-pane label="待审核" name="PENDING_AUDIT"></el-tab-pane>
      <el-tab-pane label="待发货" name="PENDING_SHIPMENT"></el-tab-pane>
      <el-tab-pane label="已发货" name="SHIPPED"></el-tab-pane>
      <el-tab-pane label="交易成功" name="COMPLETED"></el-tab-pane>
    </el-tabs>
    
    <div style="margin-bottom: 20px;">
      <el-button type="danger" @click="batchDelete" :disabled="selectedOrders.length === 0">批量删除</el-button>
    </div>
    
    <el-table 
      :data="orderList" 
      v-loading="loading" 
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip></el-table-column>
      <el-table-column label="商品" min-width="200" show-overflow-tooltip>
        <template slot-scope="scope">
          <div style="display: flex; align-items: center;">
            <img :src="scope.row.productImage || '/placeholder.png'" style="width: 40px; height: 40px; margin-right: 10px; border-radius: 4px;" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="buyerNickname" label="买家" width="100" show-overflow-tooltip></el-table-column>
      <el-table-column prop="totalPrice" label="总价" width="100">
        <template slot-scope="scope">
          ¥{{ scope.row.totalPrice }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_AUDIT'"
            type="text"
            size="small"
            style="color: var(--success-color);"
            @click="audit(scope.row.id, true)"
          >通过</el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_AUDIT'"
            type="text"
            size="small"
            style="color: var(--danger-color);"
            @click="audit(scope.row.id, false)"
          >拒绝</el-button>
          <el-button type="text" size="small" style="color: var(--danger-color);" @click="deleteOrder(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 订单详情弹窗 -->
    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="600px">
      <div v-if="currentOrder" class="order-detail-card">
        <div class="info-section">
          <h4>基本信息</h4>
          <div class="info-row">
            <span>订单号：{{ currentOrder.orderNo }}</span>
            <span>状态：{{ getStatusText(currentOrder.status) }}</span>
          </div>
          <div class="info-row">
            <span>创建时间：{{ currentOrder.createTime }}</span>
            <span>运单号：{{ currentOrder.trackingNumber || '未发货' }}</span>
          </div>
        </div>
        
        <div class="info-section">
          <h4>商品信息</h4>
          <div class="product-preview">
            <img :src="currentOrder.productImage || '/placeholder.png'" />
            <div class="product-meta">
              <p class="title">{{ currentOrder.productTitle }}</p>
              <p class="price">¥{{ currentOrder.totalPrice }} <span class="quantity">x{{ currentOrder.quantity }}</span></p>
            </div>
          </div>
        </div>

        <div class="info-section">
          <h4>收货信息</h4>
          <div class="info-row full-width">
            <span>收货人：{{ currentOrder.receiverName }}</span>
          </div>
          <div class="info-row full-width">
            <span>联系电话：{{ currentOrder.receiverPhone }}</span>
          </div>
          <div class="info-row full-width">
            <span>收货地址：{{ currentOrder.fullAddress }}</span>
          </div>
        </div>

        <div class="info-section">
          <h4>买卖双方</h4>
          <div class="info-row">
            <span>买家：{{ currentOrder.buyerNickname }}</span>
            <span>卖家：{{ currentOrder.sellerNickname }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { orderApi } from '../../api'

export default {
  name: 'AdminOrderManage',
  data() {
    return {
      activeTab: '',
      orderList: [],
      loading: false,
      detailDialogVisible: false,
      currentOrder: null,
      selectedOrders: []
    }
  },
  created() {
    this.loadOrders()
  },
  methods: {
    viewDetail(order) {
      this.currentOrder = order
      this.detailDialogVisible = true
    },
    async loadOrders() {
      this.loading = true
      try {
        const res = await orderApi.getOrderList({ status: this.activeTab })
        this.orderList = res.data
      } catch (error) {
        this.$message.error('加载订单失败')
      } finally {
        this.loading = false
      }
    },
    handleTabClick() {
      this.loadOrders()
    },
    handleSelectionChange(val) {
      this.selectedOrders = val
    },
    async batchDelete() {
      try {
        await this.$confirm(`确定要删除选中的 ${this.selectedOrders.length} 个订单吗？`, '提示', {
          type: 'warning'
        })
        
        const ids = this.selectedOrders.map(item => item.id)
        await orderApi.batchDeleteOrders(ids)
        this.$message.success('批量删除成功')
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    getStatusText(status) {
      const statusMap = {
        'PENDING_PAYMENT': '待付款',
        'PENDING_AUDIT': '待审核',
        'PENDING_SHIPMENT': '待发货',
        'SHIPPED': '已发货',
        'COMPLETED': '交易成功',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = {
        'PENDING_PAYMENT': 'warning',
        'PENDING_AUDIT': 'warning',
        'PENDING_SHIPMENT': 'info',
        'SHIPPED': '',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
      }
      return typeMap[status] || ''
    },

    async audit(orderId, approved) {
      const action = approved ? 'APPROVE' : 'REJECT'
      let remark = ''
      try {
        const { value } = await this.$prompt(
          approved ? '请输入通过备注（可选）' : '请输入拒绝原因（建议填写）',
          approved ? '审核通过' : '审核拒绝',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: approved ? '备注（可选）' : '拒绝原因',
            inputValidator: (v) => {
              if (!approved && (!v || v.trim() === '')) return '请填写拒绝原因'
              return true
            }
          }
        )
        remark = value
      } catch (e) {
        return
      }

      try {
        await orderApi.auditOrder({ orderId, action, remark })
        this.$message.success('审核成功')
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || error.response?.data?.message || '审核失败')
      }
    },

    async deleteOrder(id) {
      try {
        await this.$confirm('确定要删除该订单吗？', '提示', {
          type: 'warning'
        })
        await orderApi.deleteOrder(id)
        this.$message.success('删除成功')
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.admin-order-manage {
  padding: 20px;
}

.order-detail-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-section h4 {
  margin: 0 0 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-color);
  font-size: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-row.full-width {
  flex-direction: column;
}

.product-preview {
  display: flex;
  align-items: center;
  background-color: var(--bg-color);
  padding: 10px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.product-preview img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 10px;
}

.product-meta .title {
  margin: 0 0 5px;
  font-size: 14px;
  color: var(--text-color);
}

.product-meta .price {
  margin: 0;
  font-size: 14px;
  color: var(--danger-color);
  font-weight: bold;
}

.product-meta .quantity {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 5px;
}
</style>
