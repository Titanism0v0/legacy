<template>
  <div class="register-container">
    <div class="register-box">
      <h2>用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerForm" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="registerForm.password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="国家/地区" prop="country">
          <el-select v-model="registerForm.country" placeholder="请选择国家/地区" style="width: 100%">
            <el-option
              v-for="c in currencies"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="注册类型" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio label="USER">普通用户</el-radio>
            <el-radio label="SELLER">商家</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="agreeTerms" class="agreement-item">
          <el-checkbox v-model="registerForm.agreeTerms">我已阅读并同意《用户协议/责任边界》</el-checkbox>
          <div class="agreement-links">
            <a :href="termsDocUrl" target="_blank" rel="noopener noreferrer">查看条款</a>
            <a :href="termsRefUrl" target="_blank" rel="noopener noreferrer">官方依据</a>
          </div>
        </el-form-item>

        <el-form-item prop="agreePrivacy" class="agreement-item">
          <el-checkbox v-model="registerForm.agreePrivacy">我已阅读并同意《隐私政策》</el-checkbox>
          <div class="agreement-links">
            <a :href="privacyDocUrl" target="_blank" rel="noopener noreferrer">查看条款</a>
            <a :href="privacyRefUrl" target="_blank" rel="noopener noreferrer">官方依据</a>
          </div>
        </el-form-item>

        <div class="legal-hint">
          法律真实性提示：法规来源可在官方站点核验（{{ sourceDomains }}），并以官方发布文本为准。
          <a href="/legal/sources" target="_blank" rel="noopener noreferrer">查看全部官方来源</a>
        </div>

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
import { userApi, legalApi } from '../api'
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
    const validateAgreeTerms = (rule, value, callback) => {
      if (value !== true) {
        callback(new Error('请先阅读并同意用户协议/责任边界'))
      } else {
        callback()
      }
    }
    const validateAgreePrivacy = (rule, value, callback) => {
      if (value !== true) {
        callback(new Error('请先阅读并同意隐私政策'))
      } else {
        callback()
      }
    }
    return {
      currencies,
      loading: false,
      legal: {
        termsVersion: 'v1.0',
        privacyVersion: 'v1.0',
        officialReferences: []
      },
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        email: '',
        phone: '',
        role: 'USER',
        country: 'CNY',
        agreeTerms: false,
        agreePrivacy: false
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
        country: [{ required: true, message: '请选择国家/地区', trigger: 'change' }],
        agreeTerms: [{ validator: validateAgreeTerms, trigger: 'change' }],
        agreePrivacy: [{ validator: validateAgreePrivacy, trigger: 'change' }]
      }
    }
  },
  computed: {
    termsDocUrl() {
      return '/legal/terms'
    },
    privacyDocUrl() {
      return '/legal/privacy'
    },
    termsRefUrl() {
      return this.findRefUrl('电子商务法')
    },
    privacyRefUrl() {
      return this.findRefUrl('个人信息保护法')
    },
    sourceDomains() {
      const domains = (this.legal.officialReferences || [])
        .map(item => item.domain)
        .filter(Boolean)
      if (!domains.length) return 'npc.gov.cn / gov.cn / cac.gov.cn'
      return Array.from(new Set(domains)).slice(0, 4).join(' / ')
    }
  },
  created() {
    this.loadLegal()
  },
  methods: {
    async loadLegal() {
      try {
        const res = await legalApi.getCurrent()
        this.legal = res.data || this.legal
      } catch (error) {
        this.$message.warning('法律条款加载失败，已使用默认版本')
      }
    },
    findRefUrl(keyword) {
      const refs = this.legal.officialReferences || []
      const hit = refs.find(item => (item.title || '').includes(keyword))
      return hit && hit.url ? hit.url : '/legal/sources'
    },
    handleRegister() {
      this.$refs.registerForm.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const registerData = {
            username: this.registerForm.username,
            password: this.registerForm.password,
            nickname: this.registerForm.nickname,
            email: this.registerForm.email,
            phone: this.registerForm.phone,
            role: this.registerForm.role || 'USER',
            country: this.registerForm.country,
            agreeTerms: this.registerForm.agreeTerms,
            agreePrivacy: this.registerForm.agreePrivacy,
            termsVersion: this.legal.termsVersion,
            privacyVersion: this.legal.privacyVersion
          }

          await userApi.register(registerData)
          this.$message.success('注册成功，请登录')
          this.$router.push('/login')
        } catch (error) {
          this.$message.error(error.message || '注册失败')
        } finally {
          this.loading = false
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
  padding: 24px 0;
}

.register-box {
  background: var(--card-bg-color);
  padding: 32px;
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color-soft);
  width: 560px;
  color: var(--text-color);
}

.register-box h2 {
  text-align: center;
  margin-bottom: 22px;
  color: var(--text-color);
}

.agreement-item :deep(.el-form-item__content) {
  line-height: 1.6;
}

.agreement-links {
  margin-top: 6px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.agreement-links a {
  color: var(--primary-color);
  text-decoration: none;
}

.agreement-links a:hover {
  text-decoration: underline;
}

.legal-hint {
  margin: -4px 0 14px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.legal-hint a {
  margin-left: 4px;
  color: var(--primary-color);
  text-decoration: none;
}

.legal-hint a:hover {
  text-decoration: underline;
}

.text-center {
  text-align: center;
}
</style>
