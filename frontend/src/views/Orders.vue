<template>
  <div class="orders">
    <h2>我的订单</h2>
    <el-tabs v-model="activeTab" @tab-click="loadOrders">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="PENDING_PAYMENT" />
      <el-tab-pane label="支付处理中" name="PAYMENT_PROCESSING" />
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="待发货" name="PENDING_SHIPMENT" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="交易成功" name="COMPLETED" />
    </el-tabs>

    <el-table :data="orderList" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="190" />
      <el-table-column label="商品" min-width="180">
        <template slot-scope="scope">
          <div class="product-cell">
            <img :src="scope.row.productImage || '/placeholder.png'" class="product-img" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="费用明细" width="220">
        <template slot-scope="scope">
          <div class="fee-line">小计：¥{{ scope.row.subtotalPrice || '-' }}</div>
          <div class="fee-line">税费：¥{{ scope.row.taxEstimatedAmount || '-' }}</div>
          <div class="fee-line">运费：¥{{ scope.row.shippingFeeSnapshot || '-' }}</div>
          <div class="fee-line total">合计：¥{{ scope.row.totalPrice }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="customsClearanceStatus" label="清关状态" width="120" />
      <el-table-column prop="status" label="订单状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="refundStatus" label="退款状态" width="120">
        <template slot-scope="scope">
          <el-tag
            v-if="scope.row.refundStatus"
            :type="scope.row.refundStatus === 'REFUND_APPROVED' ? 'success' : (scope.row.refundStatus === 'REFUND_REJECTED' ? 'danger' : 'info')"
            size="mini"
          >
            {{ scope.row.refundStatus }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="340">
        <template slot-scope="scope">
          <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button size="small" type="info" @click="contactPeer(scope.row)">
            {{ isSellerRole ? '联系买家' : '联系卖家' }}
          </el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PAYMENT_PROCESSING'"
            type="primary"
            size="small"
            @click="payOrder(scope.row.id)"
          >
            去支付
          </el-button>
          <el-button
            v-if="scope.row.status === 'SHIPPED'"
            type="success"
            size="small"
            @click="confirmReceipt(scope.row.id)"
          >
            确认收货
          </el-button>
          <el-button
            v-if="scope.row.status === 'COMPLETED' || scope.row.status === 'SHIPPED'"
            size="small"
            @click="applyAfterSales(scope.row.id)"
          >
            申请售后
          </el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PAYMENT_PROCESSING' || scope.row.status === 'PENDING_SHIPMENT'"
            type="danger"
            size="small"
            @click="cancelOrder(scope.row.id)"
          >
            取消订单
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="扫码付款" :visible.sync="paymentDialogVisible" width="460px" center>
      <div class="pay-box">
        <p>订单号：{{ paymentInfo.orderNo }}</p>
        <p class="pay-amount">支付金额：¥{{ paymentInfo.amount }}</p>
        <p class="pay-tip">请扫码向商家转账，完成后上传付款凭证。</p>
        <img v-if="paymentInfo.qrCodeImage" :src="paymentInfo.qrCodeImage" class="pay-qrcode" />
        <p v-else class="pay-empty">商家暂未配置收款码</p>
        <p class="seller-line">收款方：{{ paymentInfo.receiverName || '商家' }}</p>
        <div class="proof-box">
          <div class="proof-header">付款凭证</div>
          <div class="proof-row">
            <el-input v-model="paymentProofUrl" placeholder="上传后自动填写付款截图地址" />
            <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="uploadPaymentProof">
              <el-button size="small" :loading="paymentProofUploading">上传</el-button>
            </el-upload>
          </div>
          <img v-if="paymentProofUrl" :src="paymentProofUrl" class="proof-preview" />
        </div>
      </div>
      <span slot="footer">
        <el-button @click="closePaymentDialog">取消</el-button>
        <el-button type="primary" :loading="paymentConfirming" @click="confirmPaymentClick">我已付款</el-button>
      </span>
    </el-dialog>

    <el-dialog title="订单详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ currentOrder.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ currentOrder.fullAddress }}</el-descriptions-item>
        <el-descriptions-item label="小计">¥{{ currentOrder.subtotalPrice }}</el-descriptions-item>
        <el-descriptions-item label="税费">¥{{ currentOrder.taxEstimatedAmount }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ currentOrder.shippingFeeSnapshot }}</el-descriptions-item>
        <el-descriptions-item label="总价">¥{{ currentOrder.totalPrice }}</el-descriptions-item>
        <el-descriptions-item label="清关">{{ currentOrder.customsClearanceStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款">{{ currentOrder.refundStatus || '-' }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'
import { orderApi } from '../api'

export default {
  name: 'Orders',
  computed: {
    isSellerRole() {
      const u = this.$store.state.user
      return u && u.role === 'SELLER'
    }
  },
  data() {
    return {
      activeTab: '',
      orderList: [],
      loading: false,
      paymentDialogVisible: false,
      paymentInfo: {},
      currentPaymentOrderId: null,
      paymentConfirming: false,
      paymentProofUploading: false,
      paymentProofUrl: '',
      detailVisible: false,
      currentOrder: null
    }
  },
  created() {
    this.loadOrders()
  },
  methods: {
    async loadOrders() {
      this.loading = true
      try {
        const res = await orderApi.getOrderList({ status: this.activeTab })
        this.orderList = res.data || []
      } catch (error) {
        this.$message.error('加载订单失败')
        this.orderList = []
      } finally {
        this.loading = false
      }
    },
    getStatusText(status) {
      const statusMap = {
        PENDING_PAYMENT: '待付款',
        PAYMENT_PROCESSING: '支付处理中',
        PENDING_AUDIT: '待审核',
        PENDING_SHIPMENT: '待发货',
        SHIPPED: '已发货',
        COMPLETED: '交易成功',
        CANCELLED: '已取消',
        REJECTED: '审核拒绝'
      }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = {
        PENDING_PAYMENT: 'warning',
        PAYMENT_PROCESSING: 'warning',
        PENDING_AUDIT: 'warning',
        PENDING_SHIPMENT: 'info',
        SHIPPED: '',
        COMPLETED: 'success',
        CANCELLED: 'danger',
        REJECTED: 'danger'
      }
      return typeMap[status] || ''
    },
    async payOrder(id) {
      try {
        const res = await orderApi.getPaymentQRCode(id)
        this.paymentInfo = res.data || {}
        this.currentPaymentOrderId = id
        this.paymentProofUrl = ''
        this.paymentDialogVisible = true
      } catch (error) {
        this.$message.error(error.message || '获取收款码失败')
      }
    },
    async uploadPaymentProof(file) {
      const raw = file.raw
      if (!raw) return
      const formData = new FormData()
      formData.append('file', raw, `payment_proof_${Date.now()}.jpg`)
      this.paymentProofUploading = true
      try {
        const res = await axios.post('/api/upload/payment-proof', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            Authorization: `Bearer ${this.$store.state.token}`
          }
        })
        if (res.data.code === 200) {
          this.paymentProofUrl = res.data.data.url
          this.$message.success('付款凭证上传成功')
        } else {
          this.$message.error(res.data.message || '上传失败')
        }
      } catch (error) {
        this.$message.error('付款凭证上传失败')
      } finally {
        this.paymentProofUploading = false
      }
    },
    async confirmPaymentClick() {
      if (!this.currentPaymentOrderId) return
      if (!this.paymentProofUrl) {
        this.$message.warning('请先上传付款凭证')
        return
      }
      this.paymentConfirming = true
      try {
        await orderApi.confirmPayment(this.currentPaymentOrderId, {
          paymentProof: this.paymentProofUrl
        })
        this.$message.success('付款信息已提交，等待管理员审核')
        this.closePaymentDialog()
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || '提交付款信息失败')
      } finally {
        this.paymentConfirming = false
      }
    },
    closePaymentDialog() {
      this.paymentDialogVisible = false
      this.currentPaymentOrderId = null
      this.paymentInfo = {}
      this.paymentProofUrl = ''
    },
    async confirmReceipt(id) {
      try {
        await orderApi.confirmReceipt(id)
        this.$message.success('确认收货成功')
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async cancelOrder(id) {
      try {
        await this.$confirm('确定要取消订单吗？', '提示', { type: 'warning' })
        await orderApi.cancelOrder(id)
        this.$message.success('取消订单成功')
        this.activeTab = ''
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') this.$message.error(error.message || '操作失败')
      }
    },
    applyAfterSales(id) {
      this.$router.push({ path: '/after-sales/apply', query: { orderId: id } })
    },
    contactPeer(order) {
      const u = this.$store.state.user
      if (!u || !order) return
      if (u.role === 'SELLER') {
        if (!order.buyerId) {
          this.$message.warning('未找到买家信息')
          return
        }
        this.$router.push({ path: '/chat', query: { peerUserId: order.buyerId } })
      } else {
        if (!order.sellerId) {
          this.$message.warning('未找到卖家信息')
          return
        }
        this.$router.push({ path: '/chat', query: { peerUserId: order.sellerId } })
      }
    },
    viewDetail(order) {
      this.currentOrder = order
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.orders {
  padding: 20px;
}

.product-cell {
  display: flex;
  align-items: center;
}

.product-img {
  width: 60px;
  height: 60px;
  margin-right: 10px;
  object-fit: cover;
}

.fee-line {
  line-height: 1.5;
  font-size: 12px;
}

.fee-line.total {
  font-weight: bold;
}

.pay-box {
  text-align: center;
}

.pay-amount {
  color: var(--danger-color);
  font-size: 18px;
  font-weight: 600;
}

.pay-tip {
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.pay-qrcode {
  width: 250px;
  height: 250px;
  border: 1px solid var(--border-color);
  padding: 10px;
  background: #fff;
}

.pay-empty {
  color: var(--danger-color);
}

.seller-line {
  margin-top: 12px;
  color: var(--text-secondary);
}

.proof-box {
  margin-top: 16px;
  text-align: left;
}

.proof-header {
  margin-bottom: 8px;
  font-weight: 600;
}

.proof-row {
  display: flex;
  gap: 8px;
}

.proof-preview {
  display: block;
  margin-top: 12px;
  max-width: 220px;
  border: 1px solid var(--border-color);
  padding: 8px;
  background: #fff;
}
</style>
