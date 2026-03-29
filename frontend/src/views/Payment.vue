<template>
  <div class="payment-page">
    <div class="payment-container">
      <h2>订单付款</h2>
      <div class="payment-info">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付金额：</span>
          <span class="value amount">{{ formatPrice(amount) }}</span>
        </div>
        <div class="info-item">
          <span class="label">收款商家：</span>
          <span class="value">{{ receiverName || '商家' }}</span>
        </div>
      </div>

      <div class="payment-tips">
        <p>请使用微信或支付宝扫描商家收款码完成转账。</p>
        <p class="highlight">付款金额：{{ formatPrice(amount) }}</p>
        <p>转账完成后上传付款截图，系统将进入管理员审核。</p>
      </div>

      <div class="receiver-qrcode" v-if="qrCodeImage">
        <img :src="qrCodeImage" alt="收款码" class="qrcode-image" />
      </div>

      <div class="proof-box">
        <div class="proof-title">付款凭证</div>
        <div class="proof-row">
          <el-input v-model="paymentProof" placeholder="上传后自动填写付款截图地址" />
          <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="uploadPaymentProof">
            <el-button size="small" :loading="uploading">上传</el-button>
          </el-upload>
        </div>
        <img v-if="paymentProof" :src="paymentProof" class="proof-preview" />
      </div>

      <div class="payment-actions">
        <el-button type="primary" @click="confirmPayment" :loading="confirming" size="large">
          我已付款
        </el-button>
        <el-button @click="goBack" size="large">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { orderApi } from '../api'

export default {
  name: 'Payment',
  data() {
    return {
      orderId: null,
      orderNo: '',
      amount: 0,
      receiverName: '',
      qrCodeImage: '',
      paymentProof: '',
      confirming: false,
      uploading: false
    }
  },
  created() {
    const { orderId, orderNo, amount } = this.$route.query
    this.orderId = orderId ? parseInt(orderId) : null
    this.orderNo = orderNo || ''
    this.amount = amount ? parseFloat(amount) : 0
    if (this.orderId) {
      this.loadOrderInfo()
    }
  },
  methods: {
    async loadOrderInfo() {
      try {
        const qrRes = await orderApi.getPaymentQRCode(this.orderId)
        const qrData = qrRes.data || {}
        this.orderNo = qrData.orderNo || this.orderNo
        this.amount = qrData.amount || this.amount
        this.receiverName = qrData.receiverName || ''
        this.qrCodeImage = qrData.qrCodeImage || qrData.sellerPaymentQrUrl || ''
      } catch (error) {
        this.$message.error(error.message || '加载付款信息失败')
      }
    },
    async uploadPaymentProof(file) {
      const raw = file.raw
      if (!raw) return
      const formData = new FormData()
      formData.append('file', raw, `payment_proof_${Date.now()}.jpg`)
      this.uploading = true
      try {
        const res = await axios.post('/api/upload/payment-proof', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            Authorization: `Bearer ${this.$store.state.token}`
          }
        })
        if (res.data.code === 200) {
          this.paymentProof = res.data.data.url
          this.$message.success('付款凭证上传成功')
        } else {
          this.$message.error(res.data.message || '上传失败')
        }
      } catch (error) {
        this.$message.error('付款凭证上传失败')
      } finally {
        this.uploading = false
      }
    },
    async confirmPayment() {
      if (!this.orderId) {
        this.$message.error('订单不存在')
        return
      }
      if (!this.paymentProof) {
        this.$message.warning('请先上传付款凭证')
        return
      }
      this.confirming = true
      try {
        await orderApi.confirmPayment(this.orderId, { paymentProof: this.paymentProof })
        this.$message.success('付款信息已提交，等待管理员审核')
        this.$router.push('/orders')
      } catch (error) {
        this.$message.error(error.message || '提交失败')
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
  background: linear-gradient(135deg, #f5f7fa 0%, #e4ecf7 100%);
  padding: 20px;
}

.payment-container {
  background: var(--bg-color);
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  max-width: 560px;
  width: 100%;
}

.payment-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-color);
}

.payment-info {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.label {
  color: var(--text-secondary);
}

.value.amount {
  color: var(--danger-color);
  font-size: 20px;
  font-weight: bold;
}

.payment-tips {
  margin-bottom: 24px;
  padding: 16px;
  background: #f7fbff;
  border: 1px solid #dbe8f5;
  border-radius: 6px;
  text-align: center;
}

.payment-tips p {
  margin: 8px 0;
}

.highlight {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.receiver-qrcode {
  text-align: center;
  margin-bottom: 24px;
}

.qrcode-image {
  width: 260px;
  max-width: 100%;
  border: 1px solid #ddd;
  padding: 10px;
  background: #fff;
}

.proof-box {
  margin-bottom: 24px;
}

.proof-title {
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

.payment-actions {
  text-align: center;
}

.payment-actions .el-button {
  margin: 0 10px;
  min-width: 120px;
}
</style>

