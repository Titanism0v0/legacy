<template>
  <div class="payment-page">
    <div class="payment-container">
      <h2>Order Payment</h2>
      <div class="payment-info">
        <div class="info-item">
          <span class="label">Order</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">Amount</span>
          <span class="value amount">{{ formatPrice(amount, currency) }}</span>
        </div>
        <div class="info-item" v-if="receiverName">
          <span class="label">Receiver</span>
          <span class="value">{{ receiverName }}</span>
        </div>
        <div class="info-item">
          <span class="label">Txn Status</span>
          <span class="value">{{ paymentStatus.txnStatus || paymentInfo.txnStatus || paymentInfo.status || '-' }}</span>
        </div>
      </div>

      <div class="payment-tips">
        <p>{{ paymentInfo.paymentTip || 'Scan the QR code to complete payment.' }}</p>
        <p class="highlight">{{ formatPrice(amount, currency) }}</p>
        <p v-if="!paymentInfo.requiresPaymentProof">
          The page refreshes payment status automatically. If the Alipay callback cannot reach your local machine, keep this page open briefly after payment.
        </p>
      </div>

      <div class="receiver-qrcode" v-if="qrCodeImage">
        <img :src="qrCodeImage" alt="payment qr code" class="qrcode-image" />
      </div>

      <div v-if="paymentInfo.requiresPaymentProof" class="proof-box">
        <div class="proof-title">Payment proof</div>
        <div class="proof-row">
          <el-input v-model="paymentProof" placeholder="Upload transfer proof" />
          <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="uploadPaymentProof">
            <el-button size="small" :loading="uploading">Upload</el-button>
          </el-upload>
        </div>
        <img v-if="paymentProof" :src="paymentProof" class="proof-preview" />
      </div>

      <div class="payment-actions">
        <el-button
          v-if="paymentInfo.requiresPaymentProof"
          type="primary"
          @click="confirmPayment"
          :loading="confirming"
          size="large"
        >
          Submit Proof
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="refreshStatus(true)"
          :loading="statusLoading"
          size="large"
        >
          Refresh Status
        </el-button>
        <el-button @click="goBack" size="large">Back</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/utils/axios'
import { paymentApi, orderApi } from '../api'
import { formatPriceDisplay } from '@/utils/currency'

export default {
  name: 'Payment',
  data() {
    return {
      orderId: null,
      orderNo: '',
      amount: 0,
      currency: 'CNY',
      receiverName: '',
      qrCodeImage: '',
      paymentInfo: {},
      paymentStatus: {},
      paymentProof: '',
      confirming: false,
      uploading: false,
      statusLoading: false,
      pollTimer: null
    }
  },
  created() {
    const { orderId, orderNo, amount } = this.$route.query
    this.orderId = orderId ? parseInt(orderId, 10) : null
    this.orderNo = orderNo || ''
    this.amount = amount ? parseFloat(amount) : 0
    if (this.orderId) {
      this.loadOrderInfo()
    }
  },
  beforeDestroy() {
    this.stopPolling()
  },
  methods: {
    async loadOrderInfo() {
      try {
        const res = await paymentApi.prepay(this.orderId)
        const data = res.data || {}
        this.paymentInfo = data
        this.orderNo = data.orderNo || this.orderNo
        this.amount = data.amount || this.amount
        this.currency = data.currency || 'CNY'
        this.receiverName = data.receiverName || ''
        this.qrCodeImage = data.qrCodeImage || data.sellerPaymentQrUrl || ''
        this.paymentStatus = {
          txnStatus: data.txnStatus || data.status,
          orderStatus: 'PAYMENT_PROCESSING'
        }
        this.startPolling()
      } catch (error) {
        this.$message.error(error.message || 'Failed to load payment info')
      }
    },
    startPolling() {
      this.stopPolling()
      if (this.paymentInfo.requiresPaymentProof || !this.orderId) {
        return
      }
      this.pollTimer = setInterval(() => {
        this.refreshStatus(false)
      }, 3000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    async refreshStatus(showToast) {
      if (!this.orderId) {
        return
      }
      this.statusLoading = true
      try {
        const res = await paymentApi.getStatus(this.orderId)
        this.paymentStatus = res.data || {}
        if (this.paymentStatus.paid || this.paymentStatus.orderStatus === 'PENDING_SHIPMENT' || this.paymentStatus.orderStatus === 'SHIPPED' || this.paymentStatus.orderStatus === 'COMPLETED') {
          this.stopPolling()
          if (showToast !== false) {
            this.$message.success('Payment confirmed')
          }
          this.$router.push('/orders')
        }
      } catch (error) {
        if (showToast !== false) {
          this.$message.error(error.message || 'Failed to refresh payment status')
        }
      } finally {
        this.statusLoading = false
      }
    },
    async uploadPaymentProof(file) {
      const raw = file.raw
      if (!raw) return
      const formData = new FormData()
      formData.append('file', raw, `payment_proof_${Date.now()}.jpg`)
      this.uploading = true
      try {
        const res = await axios.post('/upload/payment-proof', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        this.paymentProof = res.data.url
        this.$message.success('Payment proof uploaded')
      } catch (error) {
        this.$message.error(error.message || 'Upload failed')
      } finally {
        this.uploading = false
      }
    },
    async confirmPayment() {
      if (!this.orderId) {
        this.$message.error('Order does not exist')
        return
      }
      if (!this.paymentProof) {
        this.$message.warning('Please upload payment proof first')
        return
      }
      this.confirming = true
      try {
        await orderApi.confirmPayment(this.orderId, { paymentProof: this.paymentProof })
        this.$message.success('Payment proof submitted')
        this.$router.push('/orders')
      } catch (error) {
        this.$message.error(error.message || 'Submit failed')
      } finally {
        this.confirming = false
      }
    },
    goBack() {
      this.stopPolling()
      this.$router.go(-1)
    },
    formatPrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || 'CNY', currency || 'CNY')
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
  min-width: 140px;
}
</style>
