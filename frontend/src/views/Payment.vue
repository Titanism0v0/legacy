<template>
  <div class="payment-page">
    <div class="payment-container">
      <h2>订单支付</h2>
      <div class="payment-info">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付金额：</span>
          <span class="value amount">{{ formatPrice(amount) }}</span>
        </div>
        <div class="info-item" v-if="receiverName">
          <span class="label">收款人：</span>
          <span class="value">{{ receiverName }}</span>
        </div>
        <div class="info-item" v-if="receiverWechat">
          <span class="label">微信号：</span>
          <span class="value">{{ receiverWechat }}</span>
        </div>
      </div>
      
      <div class="payment-tips">
        <p>请使用微信扫描下方收款码进行转账</p>
        <p style="color: #f56c6c; font-weight: bold; font-size: 18px;">转账金额：{{ formatPrice(amount) }}</p>
        <p style="color: #999; font-size: 12px;">转账时请在备注中填写订单号：{{ orderNo }}</p>
      </div>
      
      <!-- 收款码显示区域 -->
      <div class="receiver-qrcode" v-if="receiverQRCodeImage">
        <img 
          :src="receiverQRCodeImage" 
          alt="收款码"
          style="width: 250px; height: 250px; border: 1px solid #ddd; padding: 10px; background: #fff; display: block; margin: 20px auto;"
        />
      </div>
      
      <div class="payment-actions">
        <el-button type="primary" @click="confirmPayment" :loading="confirming" size="large">
          我已支付
        </el-button>
        <el-button @click="goBack" size="large">返回</el-button>
      </div>
      
      <div class="payment-note">
        <p style="color: #999; font-size: 12px; margin-top: 20px;">
          提示：转账完成后，请点击"我已支付"按钮确认，系统将自动更新订单状态
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { orderApi } from '../api'

export default {
  name: 'Payment',
  data() {
    return {
      orderId: null,
      orderNo: '',
      amount: 0,
      receiverName: '',
      receiverWechat: '',
      receiverQRCodeImage: null,
      confirming: false
    }
  },
  created() {
    // 从URL参数获取订单信息
    const { orderId, orderNo, amount } = this.$route.query
    this.orderId = orderId ? parseInt(orderId) : null
    this.orderNo = orderNo || ''
    this.amount = amount ? parseFloat(amount) : 0
    
    // 如果有orderId，获取更多信息
    if (this.orderId) {
      this.loadOrderInfo()
    }
  },
  methods: {
    async loadOrderInfo() {
      try {
        // 获取支付二维码信息（包含收款码）
        const qrRes = await orderApi.getPaymentQRCode(this.orderId)
        const qrData = qrRes.data
        
        this.orderNo = qrData.orderNo
        this.amount = qrData.amount
        this.receiverName = qrData.receiverName || ''
        this.receiverWechat = qrData.receiverWechat || ''
        this.receiverQRCodeImage = qrData.receiverQRCodeImage || null
      } catch (error) {
        // 如果获取支付二维码失败，尝试直接获取订单信息
        try {
          const res = await orderApi.getOrderById(this.orderId)
          const order = res.data
          this.orderNo = order.orderNo
          this.amount = order.totalPrice
        } catch (e) {
          this.$message.error('加载订单信息失败')
        }
      }
    },
    async confirmPayment() {
      if (!this.orderId) {
        this.$message.error('订单ID不存在')
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
      
      this.confirming = true
      try {
        await orderApi.confirmPayment(this.orderId, { paymentProof })
        this.$message.success('支付确认成功')
        // 跳转到订单页面
        this.$router.push('/orders')
      } catch (error) {
        this.$message.error(error.message || error.response?.data?.message || '支付确认失败')
      } finally {
        this.confirming = false
      }
    },
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>

<style scoped>
.payment-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.payment-container {
  background: var(--bg-color);
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.payment-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-color);
}

.payment-info {
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: var(--text-secondary);
  font-size: 14px;
}

.info-item .value {
  color: var(--text-color);
  font-size: 14px;
  font-weight: 500;
}

.info-item .value.amount {
  color: var(--danger-color);
  font-size: 20px;
  font-weight: bold;
}

.payment-tips {
  background: var(--bg-color);
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 30px;
  text-align: center;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}

.payment-tips p {
  margin: 8px 0;
}

.payment-actions {
  text-align: center;
  margin-bottom: 20px;
}

.payment-actions .el-button {
  margin: 0 10px;
  min-width: 120px;
}

.payment-note {
  text-align: center;
  color: var(--text-secondary);
}
</style>
