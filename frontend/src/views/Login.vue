<template>
  <div class="login-container">
    <div class="login-box">
      <h2>用户登录</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="loginForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">登录</el-button>
        </el-form-item>
        <el-form-item>
          <div class="text-center">
            <el-button type="text" @click="$router.push('/forgot-password')" style="padding: 0;">忘记密码？</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <div class="text-center">
            <span>还没有账号？</span>
            <el-button type="text" @click="$router.push('/register')">立即注册</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { userApi } from '../api'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            const res = await userApi.login(this.loginForm)
            
            // 使用 sessionStorage 存储 Token 和用户信息
            sessionStorage.setItem('token', res.data.token)
            sessionStorage.setItem('user', JSON.stringify(res.data.user))

            this.$store.dispatch('login', {
              token: res.data.token,
              user: res.data.user
            })

            this.$message.success('登录成功')
            const targetPath = res.data.user && res.data.user.role === 'ADMIN'
              ? '/admin/workbench'
              : '/home'
            this.$router.push(targetPath)
          } catch (error) {
            this.$message.error(error.message || '登录失败')
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
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-color);
}

.login-box {
  background: var(--card-bg-color);
  padding: 40px;
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-soft);
  width: 400px;
  color: var(--text-color);
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-color);
}

.text-center {
  text-align: center;
}
</style>
