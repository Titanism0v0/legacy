<template>
  <div class="user-profile">
    <h2>个人中心</h2>

    <div class="profile-container">
      <el-card class="box-card profile-card">
        <div slot="header">
          <span>基础信息</span>
        </div>
        <div class="user-info">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              action=""
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarFileChange"
            >
              <Avatar v-if="userForm.avatar" :name="userForm.nickname || userForm.username" :src="userForm.avatar" :size="120" />
              <i v-else class="el-icon-plus avatar-uploader-icon" />
            </el-upload>
            <div class="avatar-tip">点击上传头像</div>
          </div>

          <el-form :model="userForm" label-width="90px" class="info-form">
            <el-form-item label="用户名"><span>{{ userForm.username }}</span></el-form-item>
            <el-form-item label="昵称"><el-input v-model="userForm.nickname" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="userForm.phone" /></el-form-item>
            <el-form-item label="国家/地区">
              <el-select v-model="userForm.country" style="width: 100%;">
                <el-option label="中国(CNY)" value="CNY" />
                <el-option label="美国(USD)" value="USD" />
                <el-option label="日本(JPY)" value="JPY" />
                <el-option label="欧洲(EUR)" value="EUR" />
                <el-option label="英国(GBP)" value="GBP" />
                <el-option label="韩国(KRW)" value="KRW" />
                <el-option label="加拿大(CAD)" value="CAD" />
                <el-option label="澳大利亚(AUD)" value="AUD" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <div class="right-section">
        <el-card class="box-card action-card" shadow="hover" @click.native="$router.push('/address')">
          <div class="card-content">
            <i class="el-icon-location-outline icon" />
            <div class="text">
              <h3>地址管理</h3>
              <p>管理你的收货地址</p>
            </div>
            <i class="el-icon-arrow-right arrow" />
          </div>
        </el-card>

        <el-card class="box-card action-card" shadow="hover" @click.native="$router.push('/service')">
          <div class="card-content">
            <i class="el-icon-service icon" />
            <div class="text">
              <h3>售后与客服</h3>
              <p>查看售后申请和在线沟通</p>
            </div>
            <i class="el-icon-arrow-right arrow" />
          </div>
        </el-card>
      </div>
    </div>

    <el-card v-if="isSeller" class="kyc-card">
      <div slot="header" class="kyc-header">
        <span>商家资料审核</span>
        <el-tag :type="kycTagType">{{ userForm.kycStatus || 'UNSUBMITTED' }}</el-tag>
      </div>
      <el-alert
        type="info"
        :closable="false"
        title="当前支付方式为商家收款码转账。请上传身份证明、收款码和货源说明，管理员审核通过后可上架商品。"
        class="kyc-alert"
      />
      <el-form :model="kycForm" label-width="130px">
        <el-form-item label="身份证明">
          <div class="upload-row">
            <el-input v-model="kycForm.identityDocUrl" placeholder="上传后自动填写，或手动粘贴 URL" />
            <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="file => uploadKycFile(file, 'identityDocUrl')">
              <el-button size="small">上传</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="收款码">
          <div class="upload-row">
            <el-input v-model="kycForm.paymentQrUrl" placeholder="上传微信/支付宝收款码图片地址" />
            <el-upload action="" :show-file-list="false" :auto-upload="false" :on-change="file => uploadKycFile(file, 'paymentQrUrl')">
              <el-button size="small">上传</el-button>
            </el-upload>
          </div>
          <img v-if="kycForm.paymentQrUrl" :src="kycForm.paymentQrUrl" class="payment-preview" />
        </el-form-item>
        <el-form-item label="货源说明">
          <el-input
            type="textarea"
            v-model="kycForm.sourceDescription"
            :rows="3"
            placeholder="请说明货源渠道、采购地、是否有小票或进货凭据"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" v-model="kycRemark" :rows="2" placeholder="可选，补充店铺和发货说明" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="kycSubmitting" @click="submitKyc">提交审核</el-button>
          <span class="kyc-tip">管理员通过后，买家下单时会看到你的收款码并扫码支付。</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { userApi } from '../api'
import Avatar from '@/components/Avatar.vue'
import axios from 'axios'

export default {
  name: 'UserProfile',
  components: { Avatar },
  data() {
    return {
      userForm: {
        id: null,
        username: '',
        nickname: '',
        email: '',
        phone: '',
        country: '',
        avatar: '',
        role: '',
        kycStatus: '',
        kycFiles: ''
      },
      kycForm: {
        identityDocUrl: '',
        paymentQrUrl: '',
        sourceDescription: ''
      },
      kycRemark: '',
      kycSubmitting: false
    }
  },
  computed: {
    ...mapState(['user']),
    isSeller() {
      return this.userForm.role === 'SELLER'
    },
    kycTagType() {
      const status = this.userForm.kycStatus
      if (status === 'APPROVED') return 'success'
      if (status === 'REJECTED') return 'danger'
      if (status === 'PENDING') return 'warning'
      return 'info'
    }
  },
  created() {
    this.initUserData()
  },
  methods: {
    initUserData() {
      if (this.user) {
        this.userForm = { ...this.user }
        this.fillKycForm()
      }
    },
    fillKycForm() {
      if (!this.userForm.kycFiles) return
      try {
        const parsed = JSON.parse(this.userForm.kycFiles)
        this.kycForm.identityDocUrl = parsed.identityDocUrl || ''
        this.kycForm.paymentQrUrl = parsed.paymentQrUrl || ''
        this.kycForm.sourceDescription = parsed.sourceDescription || ''
      } catch (e) {}
    },
    async handleAvatarFileChange(file) {
      const raw = file.raw
      const formData = new FormData()
      formData.append('file', raw, `avatar_${Date.now()}.jpg`)
      try {
        const res = await axios.post('/api/upload/avatar', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            Authorization: `Bearer ${this.$store.state.token}`
          }
        })
        if (res.data.code === 200) {
          this.userForm.avatar = res.data.data.url
          this.$message.success('头像上传成功，请保存资料')
        } else {
          this.$message.error(res.data.message || '上传失败')
        }
      } catch (e) {
        this.$message.error('头像上传失败')
      }
    },
    async uploadKycFile(file, field) {
      const raw = file.raw
      const formData = new FormData()
      formData.append('file', raw, `kyc_${Date.now()}_${Math.random().toString(36).slice(2)}.jpg`)
      try {
        const res = await axios.post('/api/upload/kyc', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            Authorization: `Bearer ${this.$store.state.token}`
          }
        })
        if (res.data.code === 200) {
          this.kycForm[field] = res.data.data.url
          this.$message.success('材料上传成功')
        } else {
          this.$message.error(res.data.message || '上传失败')
        }
      } catch (e) {
        this.$message.error('上传失败')
      }
    },
    async updateProfile() {
      try {
        const formData = { ...this.userForm }
        await userApi.updateUser(formData)
        this.$message.success('保存成功')
        this.$store.commit('SET_USER', formData)
      } catch (error) {
        this.$message.error('保存失败: ' + error.message)
      }
    },
    async submitKyc() {
      if (!this.kycForm.identityDocUrl || !this.kycForm.paymentQrUrl || !this.kycForm.sourceDescription) {
        this.$message.warning('请完整填写身份证明、收款码和货源说明')
        return
      }
      this.kycSubmitting = true
      try {
        await userApi.submitKyc({
          kycFiles: JSON.stringify(this.kycForm),
          remark: this.kycRemark
        })
        this.$message.success('资料已提交，等待管理员审核')
        const infoRes = await userApi.getUserInfo()
        this.userForm = { ...infoRes.data }
        this.$store.commit('SET_USER', infoRes.data)
      } catch (e) {
        this.$message.error(e.message || '提交失败')
      } finally {
        this.kycSubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
}

.profile-container {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.profile-card {
  flex: 2;
  min-width: 420px;
}

.right-section {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-info {
  display: flex;
  gap: 30px;
}

.avatar-section {
  text-align: center;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
}

.avatar-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.info-form {
  flex: 1;
}

.action-card {
  cursor: pointer;
}

.card-content {
  display: flex;
  align-items: center;
}

.icon {
  font-size: 32px;
  color: #409eff;
  margin-right: 15px;
}

.text h3 {
  margin: 0 0 5px 0;
}

.text p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.arrow {
  margin-left: auto;
  color: #c0c4cc;
}

.kyc-card {
  margin-top: 20px;
}

.kyc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kyc-alert {
  margin-bottom: 16px;
}

.upload-row {
  display: flex;
  gap: 8px;
}

.payment-preview {
  margin-top: 12px;
  max-width: 220px;
  border: 1px solid var(--border-color);
  padding: 8px;
  background: #fff;
}

.kyc-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>

