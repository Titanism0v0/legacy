<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <h2>找回密码</h2>
      <el-alert
        title="自助密码重置暂未开放"
        description="为保护账号安全，当前不再支持仅凭用户名、邮箱和手机号直接重置密码。请联系管理员完成身份核验后重置，验证码重置流程上线后会在此页面开放。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="actions">
        <el-button type="primary" @click="$router.push('/login')">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ForgotPassword'
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

.actions {
  margin-top: 24px;
  text-align: center;
}
</style>
