<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <h2>找回密码</h2>
      <el-form :model="contactForm" :rules="contactRules" ref="contactForm" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="contactForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="contactForm.email" placeholder="请输入注册时填写的邮箱"></el-input>
        </el-form-item>
        <el-form-item label="注册手机" prop="phone">
          <el-input v-model="contactForm.phone" placeholder="请输入注册时填写的手机号"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input type="password" v-model="contactForm.newPassword" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input type="password" v-model="contactForm.confirmPassword" placeholder="请再次输入新密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleResetByContact" :loading="loading" style="width: 100%">重置密码</el-button>
        </el-form-item>
      </el-form>
      <div class="text-center" style="margin-top: 20px;">
        <el-button type="text" @click="$router.push('/login')">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { userApi } from '../api'

export default {
  name: 'ForgotPassword',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.contactForm.newPassword) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    return {
      contactForm: {
        username: '',
        email: '',
        phone: '',
        newPassword: '',
        confirmPassword: ''
      },
      contactRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        email: [{ type: 'email', required: true, message: '请输入正确的邮箱地址', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, required: true, message: '请输入正确的手机号', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    async handleResetByContact() {
      this.$refs.contactForm.validate(async (valid) => {
        if (valid) {
          if (this.contactForm.newPassword !== this.contactForm.confirmPassword) {
            this.$message.error('两次输入密码不一致')
            return
          }
          
          this.loading = true
          try {
            await userApi.resetPasswordByContact({
              username: this.contactForm.username,
              email: this.contactForm.email,
              phone: this.contactForm.phone,
              newPassword: this.contactForm.newPassword
            })
            this.$message.success('密码重置成功，请使用新密码登录')
            this.$router.push('/login')
          } catch (error) {
            this.$message.error(error.message || '密码重置失败')
          } finally {
            this.loading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-color);
}

.forgot-password-box {
  background: var(--card-bg-color);
  padding: 40px;
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-soft);
  width: 600px;
  max-width: 90%;
  color: var(--text-color);
}

.forgot-password-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-color);
}

.text-center {
  text-align: center;
}
</style>
