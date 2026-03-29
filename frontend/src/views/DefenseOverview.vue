<template>
  <div class="defense-page">
    <el-card>
      <h2>答辩项目总览</h2>
      <p>用于答辩演示“已完成 / 开发中”边界，避免口头遗漏。</p>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <h3>已完成</h3>
            <ul>
              <li>商家资质提交与管理员审核</li>
              <li>商品发布类目合规校验</li>
              <li>订单税费/运费拆分展示与快照存档</li>
              <li>售后证据强制 + 规则判定 + AI建议</li>
              <li>管理员仲裁与审计日志</li>
            </ul>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <h3>开发中</h3>
            <ul>
              <li>更细粒度税则与国家政策配置</li>
              <li>AI模型接入（当前为可替换评分模块）</li>
              <li>自动化风控策略持续优化</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>

      <div class="flow">
        <h3>关键流程</h3>
        <el-steps :active="5" finish-status="success">
          <el-step title="用户下单"/>
          <el-step title="商家履约"/>
          <el-step title="售后申请"/>
          <el-step title="系统判定"/>
          <el-step title="管理员仲裁"/>
        </el-steps>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'DefenseOverview'
}
</script>

<style scoped>
.defense-page {
  padding: 20px;
}

.flow {
  margin-top: 24px;
}

ul {
  margin: 10px 0 0;
  padding-left: 18px;
}

li {
  line-height: 1.8;
}
</style>

