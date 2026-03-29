<template>
  <div class="guide-page">
    <el-card>
      <h2>Cross-Border Purchase Guide</h2>
      <el-alert
        type="info"
        :closable="false"
        title="This platform is for cross-border purchasing. Tax, customs, delivery and after-sales policies differ from domestic e-commerce."
      />

      <div class="section">
        <h3>1. Price Structure</h3>
        <p>Order amount = 商品小计 + 预估税费 + 运费。结算页会分别展示，不再只显示单一总价。</p>
        <p>税费为预估值，清关完成后以最终规则为准。</p>
      </div>

      <div class="section">
        <h3>2. Customs & Clearance</h3>
        <p>跨境订单会经过清关流程，状态包括申报、运输中、已清关。</p>
        <p>部分国家和类目可能触发更严格审查，时效会受影响。</p>
      </div>

      <div class="section">
        <h3>3. After-Sales Limits</h3>
        <p>售后需提供证据（图片/视频/文字），系统会先规则判定，再给出 AI 建议。</p>
        <p>复杂争议进入平台仲裁，所有处理会记录审计日志。</p>
      </div>

      <div class="section">
        <h3>4. Compliance</h3>
        <p>商品发布受类目白名单/黑名单约束，受限类目禁止上架。</p>
        <p>商家需先完成资质提交并通过管理员审核，才能上架销售。</p>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CrossBorderGuide'
}
</script>

<style scoped>
.guide-page {
  padding: 20px;
  max-width: 980px;
  margin: 0 auto;
}

.section {
  margin-top: 18px;
}

.section h3 {
  margin-bottom: 6px;
}
</style>

