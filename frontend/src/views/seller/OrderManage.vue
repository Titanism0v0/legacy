<template>
  <div class="order-manage">
    <h2>订单管理</h2>
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="PENDING_PAYMENT" />
      <el-tab-pane label="支付处理中" name="PAYMENT_PROCESSING" />
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="待发货" name="PENDING_SHIPMENT" />
      <el-tab-pane label="采购中" name="PURCHASING" />
      <el-tab-pane label="跨境运输" name="INTL_SHIPPING" />
      <el-tab-pane label="清关中" name="CUSTOMS_CLEARANCE" />
      <el-tab-pane label="国内配送" name="DOMESTIC_SHIPPING" />
      <el-tab-pane label="交易成功" name="COMPLETED" />
    </el-tabs>

    <el-table :data="orderList" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
      <el-table-column label="商品" min-width="220" show-overflow-tooltip>
        <template slot-scope="scope">
          <div class="product-cell">
            <img :src="scope.row.productImage || '/placeholder.svg'" class="product-img" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="buyerNickname" label="买家" width="110" show-overflow-tooltip />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="totalPrice" label="总价" width="120">
        <template slot-scope="scope">
          {{ formatOrderAmount(scope.row, scope.row.totalPrice) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="trackingNumber" label="运单号" width="170">
        <template slot-scope="scope">
          <el-input
            v-if="scope.row.status === 'PENDING_SHIPMENT'"
            v-model="scope.row.trackingNumber"
            placeholder="请输入运单号"
            size="small"
          />
          <span v-else>{{ scope.row.trackingNumber || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button type="text" size="small" @click="contactBuyer(scope.row)">联系买家</el-button>
          <el-button
            v-if="scope.row.status === 'PENDING_SHIPMENT'"
            type="text"
            size="small"
            @click="shipOrder(scope.row)"
          >
            发货
          </el-button>
          <el-button
            v-if="canAdvance(scope.row)"
            type="text"
            size="small"
            @click="advanceOrder(scope.row)"
          >
            推进履约
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="760px">
      <div v-if="currentOrder" class="order-detail-card">
        <div class="info-section">
          <h4>基本信息</h4>
          <div class="info-row">
            <span>订单号：{{ currentOrder.orderNo }}</span>
            <span>状态：{{ getStatusText(currentOrder.status) }}</span>
          </div>
          <div class="info-row">
            <span>支付币种：{{ currentOrder.paymentCurrencySnapshot || 'CNY' }}</span>
            <span>计税口径：{{ currentOrder.taxModeSnapshot === 'CBEC_PREFERENTIAL' ? '跨境零售进口优惠口径' : '一般贸易估算口径' }}</span>
          </div>
        </div>

        <div class="info-section">
          <h4>商品信息</h4>
          <div class="product-preview">
            <img :src="currentOrder.productImage || '/placeholder.svg'" />
            <div class="product-meta">
              <p class="title">{{ currentOrder.productTitle }}</p>
              <p class="price">{{ formatOrderAmount(currentOrder, currentOrder.totalPrice) }} <span class="quantity">x{{ currentOrder.quantity }}</span></p>
            </div>
          </div>
        </div>

        <div class="info-section">
          <h4>费用拆分</h4>
          <div class="info-row">
            <span>商品小计：{{ formatOrderAmount(currentOrder, currentOrder.subtotalPrice) }}</span>
            <span>国际运费：{{ formatOrderAmount(currentOrder, currentOrder.internationalShippingFeeSnapshot) }}</span>
          </div>
          <div class="info-row">
            <span>保险费：{{ formatOrderAmount(currentOrder, currentOrder.insuranceFeeSnapshot) }}</span>
            <span>关税：{{ formatOrderAmount(currentOrder, currentOrder.tariffAmountSnapshot) }}</span>
          </div>
          <div class="info-row">
            <span>进口增值税：{{ formatOrderAmount(currentOrder, currentOrder.vatAmountSnapshot) }}</span>
            <span>进口消费税：{{ formatOrderAmount(currentOrder, currentOrder.consumptionTaxAmountSnapshot) }}</span>
          </div>
          <div class="info-row">
            <span>税费合计：{{ formatOrderAmount(currentOrder, currentOrder.taxEstimatedAmount) }}</span>
            <span>订单合计：{{ formatOrderAmount(currentOrder, currentOrder.totalPrice) }}</span>
          </div>
        </div>

        <div class="info-section">
          <h4>收货信息</h4>
          <div class="info-row full-width"><span>收货人：{{ currentOrder.receiverName }}</span></div>
          <div class="info-row full-width"><span>联系电话：{{ currentOrder.receiverPhone }}</span></div>
          <div class="info-row full-width"><span>收货地址：{{ currentOrder.fullAddress }}</span></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatPriceDisplay } from '@/utils/currency'
import { orderApi } from '../../api'

export default {
  name: 'SellerOrderManage',
  data() {
    return {
      activeTab: '',
      orderList: [],
      loading: false,
      detailDialogVisible: false,
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
        const data = res && (res.data !== undefined ? res.data : res)
        this.orderList = Array.isArray(data) ? data : []
      } catch (error) {
        this.orderList = []
        this.$message.error(error.message || '加载订单失败')
      } finally {
        this.loading = false
      }
    },
    handleTabClick() {
      this.loadOrders()
    },
    viewDetail(order) {
      this.currentOrder = order
      this.detailDialogVisible = true
    },
    getStatusText(status) {
      const statusMap = {
        PENDING_PAYMENT: '待付款',
        PAYMENT_PROCESSING: '支付处理中',
        PENDING_AUDIT: '待审核',
        PENDING_SHIPMENT: '待发货',
        PURCHASING: '代购采购中',
        PURCHASED: '已采购',
        INTL_SHIPPING: '跨境运输中',
        CUSTOMS_CLEARANCE: '清关处理中',
        WAREHOUSE_INSPECTION: '入库验货中',
        DOMESTIC_SHIPPING: '国内配送中',
        SHIPPED: '已发货',
        COMPLETED: '交易成功',
        CANCELLED: '已取消'
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
        CANCELLED: 'danger'
      }
      return typeMap[status] || ''
    },
    async shipOrder(order) {
      if (!order.trackingNumber) {
        this.$message.warning('请输入运单号')
        return
      }
      try {
        await orderApi.shipOrder({
          orderId: order.id,
          trackingNumber: order.trackingNumber
        })
        this.$message.success('发货成功')
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    getNextStatus(status) {
      const nextMap = {
        PENDING_SHIPMENT: 'PURCHASING',
        PURCHASING: 'PURCHASED',
        PURCHASED: 'INTL_SHIPPING',
        INTL_SHIPPING: 'CUSTOMS_CLEARANCE',
        CUSTOMS_CLEARANCE: 'WAREHOUSE_INSPECTION',
        WAREHOUSE_INSPECTION: 'DOMESTIC_SHIPPING',
        DOMESTIC_SHIPPING: 'SHIPPED'
      }
      return nextMap[status] || ''
    },
    canAdvance(order) {
      return order && !!this.getNextStatus(order.status)
    },
    async advanceOrder(order) {
      const targetStatus = this.getNextStatus(order.status)
      if (!targetStatus) return
      const targetText = this.getStatusText(targetStatus)
      const payload = {
        orderId: order.id,
        targetStatus
      }

      try {
        if (targetStatus === 'INTL_SHIPPING') {
          const { value } = await this.$prompt('请输入跨境物流单号', `推进到${targetText}`, {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputValue: order.crossborderTrackingNumber || '',
            inputValidator: (v) => !!(v && v.trim()) || '跨境物流单号不能为空'
          })
          payload.crossborderTrackingNumber = value
        } else if (targetStatus === 'DOMESTIC_SHIPPING' || targetStatus === 'SHIPPED') {
          const { value } = await this.$prompt('请输入国内物流单号', `推进到${targetText}`, {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputValue: order.domesticTrackingNumber || order.trackingNumber || '',
            inputValidator: (v) => !!(v && v.trim()) || '国内物流单号不能为空'
          })
          payload.domesticTrackingNumber = value
        } else {
          await this.$confirm(`确认将订单推进到「${targetText}」？`, '履约推进', { type: 'warning' })
        }
        await orderApi.advanceFulfillment(payload)
        this.$message.success('履约状态已更新')
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '履约推进失败')
        }
      }
    },
    contactBuyer(order) {
      if (!order || !order.buyerId) {
        this.$message.warning('未找到买家信息')
        return
      }
      this.$router.push({
        path: '/chat',
        query: { peerUserId: order.buyerId }
      })
    },
    formatOrderAmount(order, amount) {
      const currency = order && order.paymentCurrencySnapshot ? order.paymentCurrencySnapshot : 'CNY'
      return formatPriceDisplay(amount || 0, currency, currency)
    }
  }
}
</script>

<style scoped>
.order-manage {
  padding: 20px;
}

.product-cell {
  display: flex;
  align-items: center;
}

.product-img {
  width: 40px;
  height: 40px;
  margin-right: 10px;
  border-radius: 4px;
  object-fit: cover;
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
  gap: 12px;
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
