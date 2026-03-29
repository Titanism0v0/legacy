<template>
  <div class="register-container">
    <div class="register-box">
      <h2>用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerForm" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="registerForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="国家/地区" prop="country">
          <el-select v-model="registerForm.country" placeholder="请选择国家/地区" style="width: 100%">
            <el-option
              v-for="c in currencies"
              :key="c.value"
              :label="c.label"
              :value="c.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="注册类型" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio label="USER">普通用户</el-radio>
            <el-radio label="SELLER">商家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" style="width: 100%">注册</el-button>
        </el-form-item>
        <el-form-item>
          <div class="text-center">
            <span>已有账号？</span>
            <el-button type="text" @click="$router.push('/login')">立即登录</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { userApi } from '../api'
import { currencies } from '../utils/currency'

export default {
  name: 'Register',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    return {
      currencies,
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        email: '',
        phone: '',
        role: 'USER',
        country: 'CNY' // Default country/currency
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
        country: [{ required: true, message: '请选择国家/地区', trigger: 'change' }]
      },
      loading: false
    }
  },
  methods: {
    handleRegister() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            // 准备发送的数据
            const registerData = {
              username: this.registerForm.username,
              password: this.registerForm.password,
              nickname: this.registerForm.nickname,
              email: this.registerForm.email,
              phone: this.registerForm.phone,
              role: this.registerForm.role || 'USER',
              country: this.registerForm.country
            }
            
            await userApi.register(registerData)
            this.$message.success('注册成功，请登录')
            this.$router.push('/login')
          } catch (error) {
            this.$message.error(error.message || '注册失败')
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
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-color);
}

.register-box {
  background: var(--card-bg-color);
  padding: 40px;
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-soft);
  width: 500px;
  color: var(--text-color);
}

.register-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-color);
}

.text-center {
  text-align: center;
}
</style>
