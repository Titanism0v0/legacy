<template>
  <div class="orders">
    <h2>我的订单</h2>
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name=""></el-tab-pane>
      <el-tab-pane label="待付款" name="PENDING_PAYMENT"></el-tab-pane>
      <el-tab-pane label="待发货" name="PENDING_SHIPMENT"></el-tab-pane>
      <el-tab-pane label="已发货" name="SHIPPED"></el-tab-pane>
      <el-tab-pane label="交易成功" name="COMPLETED"></el-tab-pane>
    </el-tabs>
    
    <el-table :data="orderList" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="200"></el-table-column>
      <el-table-column label="商品" width="200">
        <template slot-scope="scope">
          <div style="display: flex; align-items: center;">
            <img :src="scope.row.productImage || '/placeholder.png'" style="width: 60px; height: 60px; margin-right: 10px;" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
      <el-table-column prop="totalPrice" label="总价" width="100">
        <template slot-scope="scope">
          {{ formatPrice(scope.row.totalPrice) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="trackingNumber" label="运单号" width="150"></el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="scope">
          <el-button size="small" @click="viewDetail(scope.row.id)">查看详情</el-button>
          <el-button 
            v-if="scope.row.status === 'PENDING_PAYMENT'" 
            type="primary" 
            size="small" 
            @click="payOrder(scope.row.id)"
          >
            支付
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
            v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PENDING_SHIPMENT'" 
            type="danger" 
            size="small" 
            @click="cancelOrder(scope.row.id)"
          >
            取消订单
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 支付对话框 -->
    <el-dialog
      title="微信支付"
      :visible.sync="paymentDialogVisible"
      width="400px"
      center
    >
      <div style="text-align: center;">
        <p style="font-size: 16px; margin-bottom: 10px; color: var(--text-color);">订单号：{{ paymentInfo.orderNo }}</p>
        <p style="font-size: 18px; color: var(--danger-color); font-weight: bold; margin-bottom: 15px;">
          支付金额：¥{{ paymentInfo.amount }}
        </p>
        <p style="margin-bottom: 15px; color: var(--text-secondary);">
          <span v-if="paymentInfo.receiverQRCodeImage">请使用微信扫描下方收款码转账</span>
          <span v-else>请使用微信扫描下方二维码打开支付页面</span>
        </p>
        <div style="margin: 20px 0;">
          <!-- 优先显示个人收款码，如果没有则显示支付链接二维码 -->
          <img 
            v-if="paymentInfo.receiverQRCodeImage" 
            :src="paymentInfo.receiverQRCodeImage" 
            style="width: 250px; height: 250px; border: 1px solid var(--border-color); padding: 10px; background: #fff; display: block; margin: 0 auto; border-radius: 4px;" 
            alt="收款码"
          />
          <img 
            v-else-if="paymentInfo.qrCodeImage" 
            :src="paymentInfo.qrCodeImage" 
            style="width: 250px; height: 250px; border: 1px solid var(--border-color); padding: 10px; background: #fff; display: block; margin: 0 auto; border-radius: 4px;" 
            alt="支付二维码"
          />
          <p v-else style="color: var(--text-secondary); text-align: center;">未配置收款码</p>
        </div>
        <p style="margin-top: 15px; color: var(--text-secondary); font-size: 12px;">
          转账后请点击"我已支付"按钮确认
        </p>
        <p v-if="paymentInfo.receiverName" style="margin-top: 10px; color: #666; font-size: 14px;">
          收款人：{{ paymentInfo.receiverName }}
        </p>
        <p v-if="paymentInfo.receiverWechat" style="margin-top: 5px; color: #666; font-size: 14px;">
          微信号：{{ paymentInfo.receiverWechat }}
        </p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPaymentClick" :loading="paymentConfirming">
          我已支付
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { orderApi, addressApi } from '../api'

export default {
  name: 'Orders',
  data() {
    return {
      activeTab: '',
      orderList: [],
      loading: false,
      paymentDialogVisible: false,
      paymentInfo: {},
      currentPaymentOrderId: null,
      paymentConfirming: false
    }
  },
  created() {
    // 检查是否是立即购买跳转过来的
    const { productId, quantity } = this.$route.query
    if (productId && quantity) {
      this.handleBuyNow(productId, quantity)
    } else {
      this.loadOrders()
    }
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
    handleTabClick() {
      this.loadOrders()
    },
    getStatusText(status) {
      const statusMap = {
        'PENDING_PAYMENT': '待付款',
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
        'PENDING_SHIPMENT': 'info',
        'SHIPPED': '',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
      }
      return typeMap[status] || ''
    },
    async payOrder(id) {
      try {
        // 获取支付二维码
        const qrRes = await orderApi.getPaymentQRCode(id)
        const qrData = qrRes.data
        
        // 显示支付对话框
        this.paymentInfo = qrData
        this.currentPaymentOrderId = id
        this.paymentDialogVisible = true
      } catch (error) {
        this.$message.error(error.message || error.response?.data?.message || '获取支付二维码失败')
      }
    },
    async confirmPaymentClick() {
      if (!this.currentPaymentOrderId) {
        return
      }
      
      // 要求用户确认
      try {
        await this.$confirm(
          '请确认您已经完成转账。\n\n提示：建议上传转账截图作为支付凭证，方便后续核对。',
          '确认支付',
          {
            confirmButtonText: '我已支付',
            cancelButtonText: '取消',
            type: 'warning',
            center: true
          }
        )
      } catch {
        // 用户取消
        return
      }
      
      // 询问是否上传支付凭证
      let paymentProof = null
      try {
        const { value } = await this.$prompt('请输入支付凭证（转账截图URL），或直接点击确定跳过', '支付凭证（可选）', {
          confirmButtonText: '确定',
          cancelButtonText: '跳过',
          inputPlaceholder: '转账截图URL（可选）',
          inputValidator: (value) => {
            if (!value || value.trim() === '') {
              return true // 允许为空
            }
            // 简单验证URL格式
            if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/upload/')) {
              return true
            }
            return '请输入有效的URL'
          }
        })
        paymentProof = value && value.trim() !== '' ? value.trim() : null
      } catch {
        // 用户跳过或取消，继续执行
      }
      
      this.paymentConfirming = true
      try {
        await orderApi.confirmPayment(this.currentPaymentOrderId, { paymentProof })
        this.$message.success('支付确认成功')
        this.paymentDialogVisible = false
        this.paymentInfo = {}
        this.currentPaymentOrderId = null
        this.loadOrders()
      } catch (error) {
        this.$message.error(error.message || error.response?.data?.message || '支付确认失败')
      } finally {
        this.paymentConfirming = false
      }
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
        await this.$confirm('确定要取消订单吗？', '提示', {
          type: 'warning'
        })
        await orderApi.cancelOrder(id)
        this.$message.success('取消订单成功')
        // 切换到全部标签页，确保能看到取消后的订单状态
        this.activeTab = ''
        this.loadOrders()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '操作失败')
        }
      }
    },
    viewDetail(id) {
      // 查看订单详情
      this.$message.info('订单详情功能开发中')
    },
    applyAfterSales(id) {
      this.$router.push({
        path: '/after-sales/apply',
        query: { orderId: id }
      })
    },
    async handleBuyNow(productId, quantity) {
      // 立即购买：需要先选择收货地址，然后创建订单
      this.loading = true
      try {
        // 先获取收货地址列表
        const addressRes = await addressApi.getAddressList()
        const addresses = addressRes.data
        
        if (!addresses || addresses.length === 0) {
          this.$message.warning('请先添加收货地址')
          this.$router.push('/address')
          return
        }
        
        // 如果有默认地址，使用默认地址；否则使用第一个地址
        const defaultAddress = addresses.find(addr => addr.isDefault === 1) || addresses[0]
        
        // 创建订单
        const orderRes = await orderApi.createOrder({
          productId: parseInt(productId),
          addressId: defaultAddress.id,
          quantity: parseInt(quantity)
        })
        
        this.$message.success('订单创建成功，请支付')
        
        // 清除query参数，避免刷新时重复创建
        this.$router.replace({ path: '/orders' })
        
        // 加载订单列表
        this.loadOrders()
      } catch (error) {
        console.error('立即购买失败:', error)
        this.$message.error(error.message || error.response?.data?.message || '创建订单失败')
        // 如果失败，也加载订单列表
        this.loadOrders()
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.orders {
  padding: 20px;
}
</style>
