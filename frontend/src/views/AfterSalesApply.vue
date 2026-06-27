<template>
  <div class="after-sales-apply">
    <h2>申请售后</h2>
    <el-card class="box-card">
      <el-form :model="form" :rules="rules" ref="form" label-width="110px">
        <el-form-item label="关联订单">
          <span v-if="order">{{ order.orderNo }}</span>
          <span v-else>未知订单</span>
        </el-form-item>

        <el-form-item label="售后类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio label="REFUND_ONLY">仅退款</el-radio>
            <el-radio label="RETURN_GOODS">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="退款金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="2" :step="0.1" :min="0.01" :max="maxAmount" />
          <span class="inline-tip">最多可退 ¥{{ maxAmount }}</span>
        </el-form-item>

        <el-form-item label="申请原因" prop="reason">
          <el-select v-model="form.reason" placeholder="请选择原因" style="width: 100%">
            <el-option label="质量问题" value="质量问题" />
            <el-option label="商品与描述不符" value="商品与描述不符" />
            <el-option label="运输破损" value="运输破损" />
            <el-option label="少件/漏发" value="少件/漏发" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="问题描述" prop="description">
          <el-input type="textarea" v-model="form.description" rows="4" placeholder="请详细描述问题与诉求" />
        </el-form-item>

        <el-form-item label="证据文本" prop="evidenceText">
          <el-input type="textarea" v-model="form.evidenceText" rows="3" placeholder="可补充订单截图说明、时间线、沟通记录摘要等" />
        </el-form-item>

        <el-form-item label="视频证据URL">
          <el-input v-model="videoUrl" placeholder="可选：上传视频后填写链接，如 .mp4" />
        </el-form-item>

        <el-form-item label="证据图片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleRemove"
            :file-list="fileList"
            accept="image/*"
          >
            <i slot="default" class="el-icon-plus"></i>
          </el-upload>
          <div class="upload-tip">至少提供一种证据：图片 / 视频URL / 证据文本（>=10字）</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">提交申请</el-button>
          <el-button @click="$router.go(-1)">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { orderApi, afterSalesApi } from '../api'
import axios from '@/utils/axios'

export default {
  name: 'AfterSalesApply',
  data() {
    return {
      orderId: null,
      order: null,
      maxAmount: 0,
      form: {
        type: 'REFUND_ONLY',
        amount: 0,
        reason: '',
        description: '',
        evidenceText: ''
      },
      videoUrl: '',
      fileList: [],
      rules: {
        type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
        amount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
        reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }],
        description: [{ required: true, message: '请输入问题描述', trigger: 'blur' }],
        evidenceText: [{ min: 0, max: 1000, message: '证据文本过长', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  created() {
    this.orderId = this.$route.query.orderId
    if (!this.orderId) {
      this.$message.error('订单参数缺失')
      const role = this.$store && this.$store.state && this.$store.state.user ? this.$store.state.user.role : null
      this.$router.push(role === 'SELLER' ? '/seller/orders' : '/orders')
      return
    }
    this.loadOrderInfo()
  },
  methods: {
    async loadOrderInfo() {
      try {
        const res = await orderApi.getOrderById(this.orderId)
        this.order = res.data
        this.maxAmount = Number(this.order.totalPrice || 0)
        this.form.amount = this.maxAmount
      } catch (error) {
        this.$message.error('加载订单信息失败')
      }
    },
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-5)
    },
    handleRemove(file) {
      this.fileList = this.fileList.filter(f => f.uid !== file.uid)
    },
    async uploadImages() {
      if (this.fileList.length === 0) return []
      const uploadPromises = this.fileList.map(file => {
        const formData = new FormData()
        const fileName = `after_sales_${Date.now()}_${Math.random().toString(36).slice(2)}.jpg`
        formData.append('file', file.raw, fileName)
        return axios.post('/upload/product', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        }).then(res => {
          return res.data.url
        })
      })
      return Promise.all(uploadPromises)
    },
    async submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return
        this.submitting = true
        try {
          const imageUrls = await this.uploadImages()
          const evidenceUrls = [...imageUrls]
          if (this.videoUrl && this.videoUrl.trim()) evidenceUrls.push(this.videoUrl.trim())
          const evidenceText = (this.form.evidenceText || '').trim()

          if (evidenceUrls.length === 0 && evidenceText.length < 10) {
            this.$message.warning('请至少提供一种有效证据（图片/视频URL/10字以上文本）')
            this.submitting = false
            return
          }

          const submitData = {
            orderId: this.orderId,
            type: this.form.type,
            reason: this.form.reason,
            amount: this.form.amount,
            description: this.form.description,
            images: JSON.stringify(imageUrls.filter(url => /\.(png|jpg|jpeg|webp|gif)$/i.test(url))),
            evidenceUrls: JSON.stringify(evidenceUrls),
            evidenceText,
            evidenceType: evidenceUrls.length > 0 && evidenceText ? 'MIXED' : (evidenceUrls.length > 0 ? 'IMAGE' : 'TEXT')
          }
          await afterSalesApi.apply(submitData)
          this.$message.success('售后申请已提交')
          this.$router.push('/after-sales/list')
        } catch (error) {
          this.$message.error(error.message || '提交失败')
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>

<style scoped>
.after-sales-apply {
  padding: 20px;
}

.box-card {
  max-width: 860px;
  margin: 0 auto;
}

.inline-tip {
  margin-left: 10px;
  color: #999;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
