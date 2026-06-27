<template>
  <div class="orders">
    <h2>My Orders</h2>
    <el-tabs v-model="activeTab" @tab-click="loadOrders">
      <el-tab-pane label="All" name="" />
      <el-tab-pane label="Pending Payment" name="PENDING_PAYMENT" />
      <el-tab-pane label="Paying" name="PAYMENT_PROCESSING" />
      <el-tab-pane label="Pending Audit" name="PENDING_AUDIT" />
      <el-tab-pane label="Pending Shipment" name="PENDING_SHIPMENT" />
      <el-tab-pane label="Purchasing" name="PURCHASING" />
      <el-tab-pane label="Cross-border" name="INTL_SHIPPING" />
      <el-tab-pane label="Customs" name="CUSTOMS_CLEARANCE" />
      <el-tab-pane label="Domestic" name="DOMESTIC_SHIPPING" />
      <el-tab-pane label="Shipped" name="SHIPPED" />
      <el-tab-pane label="Completed" name="COMPLETED" />
    </el-tabs>

    <el-table :data="orderList" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="Order No." width="190" />
      <el-table-column label="Product" min-width="180">
        <template slot-scope="scope">
          <div class="product-cell">
            <img :src="scope.row.productImage || '/placeholder.svg'" class="product-img" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Fee Details" width="260">
        <template slot-scope="scope">
          <div class="fee-line">Subtotal: {{ formatOrderAmount(scope.row, scope.row.subtotalPrice) }}</div>
          <div class="fee-line">Intl shipping: {{ formatOrderAmount(scope.row, scope.row.internationalShippingFeeSnapshot) }}</div>
          <div class="fee-line">Insurance: {{ formatOrderAmount(scope.row, scope.row.insuranceFeeSnapshot) }}</div>
          <div class="fee-line">Tax: {{ formatOrderAmount(scope.row, scope.row.taxEstimatedAmount) }}</div>
          <div class="fee-line total">Total: {{ formatOrderAmount(scope.row, scope.row.totalPrice) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="customsClearanceStatus" label="Customs" width="120" />
      <el-table-column prop="status" label="Status" width="140">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="refundStatus" label="Refund" width="130">
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
      <el-table-column label="Actions" min-width="340">
        <template slot-scope="scope">
          <el-button size="small" @click="viewDetail(scope.row)">Detail</el-button>
          <el-button size="small" type="info" @click="contactPeer(scope.row)">
            {{ isSellerRole ? 'Contact Buyer' : 'Contact Seller' }}
          </el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PAYMENT_PROCESSING'"
            type="primary"
            size="small"
            @click="payOrder(scope.row.id)"
          >
            Pay
          </el-button>
          <el-button
            v-if="scope.row.status === 'SHIPPED'"
            type="success"
            size="small"
            @click="confirmReceipt(scope.row.id)"
          >
            Confirm Receipt
          </el-button>
          <el-button
            v-if="scope.row.status === 'COMPLETED' || scope.row.status === 'SHIPPED'"
            size="small"
            @click="applyAfterSales(scope.row.id)"
          >
            After-sales
          </el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PAYMENT_PROCESSING' || scope.row.status === 'PENDING_SHIPMENT'"
            type="danger"
            size="small"
            @click="cancelOrder(scope.row.id)"
          >
            Cancel
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="Payment" :visible.sync="paymentDialogVisible" width="460px" center>
      <div class="pay-box">
        <p>Order: {{ paymentInfo.orderNo }}</p>
        <p class="pay-amount">Amount: {{ formatLiteralPrice(paymentInfo.amount, paymentInfo.currency || 'CNY') }}</p>
        <p class="pay-tip">{{ paymentInfo.paymentTip || 'Scan the QR code to complete payment.' }}</p>
        <img v-if="paymentInfo.qrCodeImage" :src="paymentInfo.qrCodeImage" class="pay-qrcode" />
        <p v-else class="pay-empty">No QR code is available.</p>
        <p v-if="paymentInfo.receiverName" class="seller-line">Receiver: {{ paymentInfo.receiverName }}</p>
        <p v-if="paymentStatusText" class="status-line">Txn status: {{ paymentStatusText }}</p>
        <p v-if="paymentStatus.orderStatus" class="status-line">Order status: {{ paymentStatus.orderStatus }}</p>

        <div v-if="paymentInfo.requiresPaymentProof" class="proof-box">
          <div class="proof-header">Payment proof</div>
          <div class="proof-row">
            <el-input v-model="paymentProofUrl" placeholder="Upload transfer proof" />
            <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="uploadPaymentProof">
              <el-button size="small" :loading="paymentProofUploading">Upload</el-button>
            </el-upload>
          </div>
          <img v-if="paymentProofUrl" :src="paymentProofUrl" class="proof-preview" />
        </div>

        <p v-else class="polling-hint">
          The page checks payment status automatically. If your local callback is not reachable, keep this dialog open for a few seconds after payment.
        </p>
      </div>
      <span slot="footer">
        <el-button @click="closePaymentDialog">Close</el-button>
        <el-button
          v-if="paymentInfo.requiresPaymentProof"
          type="primary"
          :loading="paymentConfirming"
          @click="confirmPaymentClick"
        >
          Submit Proof
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="paymentStatusLoading"
          @click="refreshPaymentStatus(true)"
        >
          Refresh Status
        </el-button>
      </span>
    </el-dialog>

    <el-dialog title="Order Detail" :visible.sync="detailVisible" width="760px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="Order No.">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ getStatusText(currentOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="Payment Currency">{{ currentOrder.paymentCurrencySnapshot || 'CNY' }}</el-descriptions-item>
        <el-descriptions-item label="Tax Mode">
          {{ currentOrder.taxModeSnapshot === 'CBEC_PREFERENTIAL' ? 'CBEC preferential' : 'General trade estimate' }}
        </el-descriptions-item>
        <el-descriptions-item label="Receiver">{{ currentOrder.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="Phone">{{ currentOrder.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="Address" :span="2">{{ currentOrder.fullAddress }}</el-descriptions-item>
        <el-descriptions-item label="Subtotal">{{ formatOrderAmount(currentOrder, currentOrder.subtotalPrice) }}</el-descriptions-item>
        <el-descriptions-item label="Intl shipping">{{ formatOrderAmount(currentOrder, currentOrder.internationalShippingFeeSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="Insurance">{{ formatOrderAmount(currentOrder, currentOrder.insuranceFeeSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="Tax base">
          {{ formatOrderAmount(currentOrder, (Number(currentOrder.subtotalPrice || 0) + Number(currentOrder.shippingFeeSnapshot || 0))) }}
        </el-descriptions-item>
        <el-descriptions-item label="Tariff">{{ formatOrderAmount(currentOrder, currentOrder.tariffAmountSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="VAT">{{ formatOrderAmount(currentOrder, currentOrder.vatAmountSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="Consumption Tax">{{ formatOrderAmount(currentOrder, currentOrder.consumptionTaxAmountSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="Tax">{{ formatOrderAmount(currentOrder, currentOrder.taxEstimatedAmount) }}</el-descriptions-item>
        <el-descriptions-item label="Total">{{ formatOrderAmount(currentOrder, currentOrder.totalPrice) }}</el-descriptions-item>
        <el-descriptions-item label="Customs">{{ currentOrder.customsClearanceStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Refund">{{ currentOrder.refundStatus || '-' }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="detailVisible = false">Close</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axios from '@/utils/axios'
import { formatPriceDisplay } from '@/utils/currency'
import { orderApi, paymentApi } from '../api'

export default {
  name: 'Orders',
  data() {
    return {
      activeTab: '',
      orderList: [],
      loading: false,
      paymentDialogVisible: false,
      paymentInfo: {},
      paymentStatus: {},
      paymentStatusLoading: false,
      paymentPollTimer: null,
      currentPaymentOrderId: null,
      paymentConfirming: false,
      paymentProofUploading: false,
      paymentProofUrl: '',
      detailVisible: false,
      currentOrder: null
    }
  },
  computed: {
    isSellerRole() {
      const user = this.$store.state.user
      return user && user.role === 'SELLER'
    },
    paymentStatusText() {
      return this.paymentStatus.txnStatus || this.paymentInfo.txnStatus || this.paymentInfo.status || ''
    }
  },
  created() {
    this.loadOrders()
  },
  beforeDestroy() {
    this.stopPaymentPolling()
  },
  methods: {
    async loadOrders() {
      this.loading = true
      try {
        const res = await orderApi.getOrderList({ status: this.activeTab })
        this.orderList = res.data || []
      } catch (error) {
        this.orderList = []
        this.$message.error(error.message || 'Failed to load orders')
      } finally {
        this.loading = false
      }
    },
    getStatusText(status) {
      const statusMap = {
        PENDING_PAYMENT: 'Pending Payment',
        PAYMENT_PROCESSING: 'Paying',
        PENDING_AUDIT: 'Pending Audit',
        PENDING_SHIPMENT: 'Pending Shipment',
        PURCHASING: 'Purchasing',
        PURCHASED: 'Purchased',
        INTL_SHIPPING: 'Cross-border Shipping',
        CUSTOMS_CLEARANCE: 'Customs Clearance',
        WAREHOUSE_INSPECTION: 'Warehouse Inspection',
        DOMESTIC_SHIPPING: 'Domestic Shipping',
        SHIPPED: 'Shipped',
        COMPLETED: 'Completed',
        CANCELLED: 'Cancelled',
        REJECTED: 'Rejected'
      }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = {
        PENDING_PAYMENT: 'warning',
        PAYMENT_PROCESSING: 'warning',
        PENDING_AUDIT: 'warning',
        PENDING_SHIPMENT: 'info',
        PURCHASING: 'info',
        PURCHASED: 'info',
        INTL_SHIPPING: '',
        CUSTOMS_CLEARANCE: 'warning',
        WAREHOUSE_INSPECTION: 'warning',
        DOMESTIC_SHIPPING: '',
        SHIPPED: '',
        COMPLETED: 'success',
        CANCELLED: 'danger',
        REJECTED: 'danger'
      }
      return typeMap[status] || ''
    },
    async payOrder(id) {
      try {
        const res = await paymentApi.prepay(id)
        this.paymentInfo = res.data || {}
        this.paymentStatus = {
          txnStatus: this.paymentInfo.txnStatus || this.paymentInfo.status,
          orderStatus: 'PAYMENT_PROCESSING'
        }
        this.currentPaymentOrderId = id
        this.paymentProofUrl = ''
        this.paymentDialogVisible = true
        this.startPaymentPolling()
      } catch (error) {
        this.$message.error(error.message || 'Failed to create payment order')
      }
    },
    startPaymentPolling() {
      this.stopPaymentPolling()
      if (this.paymentInfo.requiresPaymentProof || !this.currentPaymentOrderId) {
        return
      }
      this.paymentPollTimer = setInterval(() => {
        this.refreshPaymentStatus(false)
      }, 3000)
    },
    stopPaymentPolling() {
      if (this.paymentPollTimer) {
        clearInterval(this.paymentPollTimer)
        this.paymentPollTimer = null
      }
    },
    async refreshPaymentStatus(showToast) {
      if (!this.currentPaymentOrderId) {
        return
      }
      this.paymentStatusLoading = true
      try {
        const res = await paymentApi.getStatus(this.currentPaymentOrderId)
        this.paymentStatus = res.data || {}
        if (this.paymentStatus.paid || this.paymentStatus.orderStatus === 'PENDING_SHIPMENT' || this.paymentStatus.orderStatus === 'SHIPPED' || this.paymentStatus.orderStatus === 'COMPLETED') {
          this.stopPaymentPolling()
          if (showToast !== false) {
            this.$message.success('Payment confirmed')
          }
          this.closePaymentDialog()
          this.loadOrders()
        }
      } catch (error) {
        if (showToast !== false) {
          this.$message.error(error.message || 'Failed to refresh payment status')
        }
      } finally {
        this.paymentStatusLoading = false
      }
    },
    async uploadPaymentProof(file) {
      const raw = file.raw
      if (!raw) return
      const formData = new FormData()
      formData.append('file', raw, `payment_proof_${Date.now()}.jpg`)
      this.paymentProofUploading = true
      try {
        const res = await axios.post('/upload/payment-proof', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        this.paymentProofUrl = res.data.url
        this.$message.success('Payment proof uploaded')
      } catch (error) {
        this.$message.error(error.message || 'Upload failed')
      } finally {
        this.paymentProofUploading = false
      }
    },
    async confirmPaymentClick() {
      if (!this.currentPaymentOrderId) return
      if (!this.paymentProofUrl) {
        this.$message.warning('Please upload payment proof first')
        return
      }
      this.paymentConfirming = true
      try {
        await orderApi.confirmPayment(this.currentPaymentOrderId, {
          paymentProof: this.paymentProofUrl
        })
        this.$message.success('Payment proof submitted')
        this.closePaymentDialog()
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || 'Failed to submit payment proof')
      } finally {
        this.paymentConfirming = false
      }
    },
    closePaymentDialog() {
      this.stopPaymentPolling()
      this.paymentDialogVisible = false
      this.currentPaymentOrderId = null
      this.paymentInfo = {}
      this.paymentStatus = {}
      this.paymentProofUrl = ''
    },
    async confirmReceipt(id) {
      try {
        await orderApi.confirmReceipt(id)
        this.$message.success('Receipt confirmed')
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || 'Operation failed')
      }
    },
    async cancelOrder(id) {
      try {
        await this.$confirm('Cancel this order?', 'Confirm', { type: 'warning' })
        await orderApi.cancelOrder(id)
        this.$message.success('Order cancelled')
        this.activeTab = ''
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || 'Operation failed')
        }
      }
    },
    applyAfterSales(id) {
      this.$router.push({ path: '/after-sales/apply', query: { orderId: id } })
    },
    contactPeer(order) {
      const user = this.$store.state.user
      if (!user || !order) return
      const peerUserId = user.role === 'SELLER' ? order.buyerId : order.sellerId
      if (!peerUserId) {
        this.$message.warning('Peer user was not found')
        return
      }
      this.$router.push({ path: '/chat', query: { peerUserId } })
    },
    viewDetail(order) {
      this.currentOrder = order
      this.detailVisible = true
    },
    formatOrderAmount(order, amount) {
      const currency = order && order.paymentCurrencySnapshot ? order.paymentCurrencySnapshot : 'CNY'
      return this.formatLiteralPrice(amount, currency)
    },
    formatLiteralPrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || 'CNY', currency || 'CNY')
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

.seller-line,
.status-line,
.polling-hint {
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
