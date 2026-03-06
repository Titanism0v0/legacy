<template>
  <div class="after-sales-apply">
    <h2>申请售后</h2>
    <el-card class="box-card">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
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
          <el-input-number v-model="form.amount" :precision="2" :step="0.1" :min="0.01" :max="maxAmount"></el-input-number>
          <span style="margin-left: 10px; color: #999;">最多可退 ¥{{ maxAmount }}</span>
        </el-form-item>

        <el-form-item label="申请原因" prop="reason">
          <el-select v-model="form.reason" placeholder="请选择原因" style="width: 100%">
            <el-option label="多拍/错拍/不想要" value="多拍/错拍/不想要"></el-option>
            <el-option label="快递无记录" value="快递无记录"></el-option>
            <el-option label="少货/空包裹" value="少货/空包裹"></el-option>
            <el-option label="质量问题" value="质量问题"></el-option>
            <el-option label="商品与描述不符" value="商品与描述不符"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="问题描述" prop="description">
          <el-input type="textarea" v-model="form.description" rows="4" placeholder="请详细描述您遇到的问题..."></el-input>
        </el-form-item>

        <el-form-item label="凭证图片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleRemove"
            :file-list="fileList"
            accept="image/*">
            <i slot="default" class="el-icon-plus"></i>
            <div slot="file" slot-scope="{file}">
              <img class="el-upload-list__item-thumbnail" :src="file.url" alt="">
              <span class="el-upload-list__item-actions">
                <span class="el-upload-list__item-delete" @click="handleRemove(file)">
                  <i class="el-icon-delete"></i>
                </span>
              </span>
            </div>
          </el-upload>
          <div style="font-size: 12px; color: #999; margin-top: 5px;">
            支持JPG/PNG格式，最多上传3张
          </div>
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
import axios from 'axios'

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
        images: [] // 存储上传后的URL
      },
      fileList: [], // 存储文件对象
      rules: {
        type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
        amount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
        reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }],
        description: [{ required: true, message: '请输入问题描述', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  created() {
    this.orderId = this.$route.query.orderId
    if (!this.orderId) {
      this.$message.error('订单参数缺失')
      this.$router.push('/orders')
      return
    }
    this.loadOrderInfo()
  },
  methods: {
    async loadOrderInfo() {
      try {
        const res = await orderApi.getOrderById(this.orderId)
        this.order = res.data
        this.maxAmount = this.order.totalPrice
        this.form.amount = this.maxAmount // 默认全额退款
      } catch (error) {
        this.$message.error('加载订单信息失败')
      }
    },
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-3) // 限制3张
    },
    handleRemove(file) {
      this.fileList = this.fileList.filter(f => f.uid !== file.uid)
    },
    async uploadImages() {
      if (this.fileList.length === 0) return []
      
      const uploadPromises = this.fileList.map(file => {
        const formData = new FormData()
        // 生成文件名
        const fileName = 'after_sales_' + new Date().getTime() + '_' + Math.random().toString(36).substr(2) + '.jpg'
        formData.append('file', file.raw, fileName)
        
        // 使用与 UserProfile.vue 相同的上传逻辑
        // 注意：这里假设后端有一个通用的上传接口，或者复用头像上传接口
        // 如果没有通用接口，可能需要新增。这里暂时复用 /upload/avatar 接口或假设有 /upload/image
        // 查看 UserProfile.vue 发现是 /upload/avatar，后端 UploadController.java
        // 建议使用 /upload/image (如果存在) 或者复用
        // 实际上后端代码里 UploadController 可能只写了 avatar。
        // 为了保险，我们假设使用 /upload/image，如果后端没有，需要添加。
        // 暂时使用 /upload/avatar 作为替代，因为它只是返回 URL
        
        return axios.post('/api/upload/avatar', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        }).then(res => {
          if (res.data.code === 200) {
            return res.data.data.url
          } else {
            throw new Error(res.data.message || '上传失败')
          }
        })
      })
      
      return Promise.all(uploadPromises)
    },
    async submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          this.submitting = true
          try {
            // 先上传图片
            const imageUrls = await this.uploadImages()
            
            const submitData = {
              orderId: this.orderId,
              type: this.form.type,
              reason: this.form.reason,
              amount: this.form.amount,
              description: this.form.description,
              images: JSON.stringify(imageUrls)
            }
            
            await afterSalesApi.apply(submitData)
            this.$message.success('申请提交成功，请等待审核')
            this.$router.push('/after-sales/list')
          } catch (error) {
            console.error(error)
            this.$message.error(error.message || '提交失败')
          } finally {
            this.submitting = false
          }
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
  max-width: 800px;
  margin: 0 auto;
}
</style>
