<template>
  <div class="service-support">
    <h2>售后与客服</h2>
    
    <div class="service-container">
      <!-- 售后申请入口 -->
      <el-card class="box-card service-card" shadow="hover">
        <div class="card-header">
          <i class="el-icon-refresh-left icon-large"></i>
          <h3>退换货申请</h3>
        </div>
        <p>如商品存在质量问题或不符合描述，您可以申请退货或换货。</p>
        <el-button type="primary" size="small" @click="handleApply">申请售后</el-button>
      </el-card>

      <!-- 在线客服入口 -->
      <el-card class="box-card service-card" shadow="hover">
        <div class="card-header">
          <i class="el-icon-headset icon-large"></i>
          <h3>在线客服</h3>
        </div>
        <p>工作时间：周一至周日 9:00 - 22:00，我们将竭诚为您服务。</p>
        <el-button type="success" size="small" @click="handleContact">联系客服</el-button>
      </el-card>

      <!-- 常见问题 -->
      <el-card class="box-card faq-card">
        <div slot="header">
          <span>常见问题 FAQ</span>
        </div>
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item title="如何修改收货地址？" name="1">
            <div>您可以进入“个人中心 - 地址管理”页面，点击“编辑”按钮修改现有地址，或点击“添加地址”新增收货信息。</div>
          </el-collapse-item>
          <el-collapse-item title="订单发货后多久能收到？" name="2">
            <div>一般情况下，发货后3-5天内送达。偏远地区可能需要5-7天。您可以随时在“我的订单”中查看物流进度。</div>
          </el-collapse-item>
          <el-collapse-item title="退款流程是怎样的？" name="3">
            <div>申请退款后，卖家将在48小时内处理。审核通过后，款项将原路退回您的支付账户，预计1-3个工作日到账。</div>
          </el-collapse-item>
          <el-collapse-item title="商品支持七天无理由退货吗？" name="4">
            <div>大部分商品支持七天无理由退货（不影响二次销售的前提下）。特殊商品（如生鲜、定制品）请以商品详情页说明为准。</div>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ServiceSupport',
  data() {
    return {
      activeName: '1'
    }
  },
  methods: {
    handleApply() {
      this.$confirm('请在“我的订单”页面选择具体的订单申请售后服务。是否立即前往订单列表？', '提示', {
        confirmButtonText: '前往订单',
        cancelButtonText: '查看记录',
        type: 'info'
      }).then(() => {
        const role = (this.$store && this.$store.state && this.$store.state.user) ? this.$store.state.user.role : null
        const target = role === 'ADMIN' ? '/admin/orders' : (role === 'SELLER' ? '/seller/orders' : '/orders')
        this.$router.push(target).catch(() => {})
      }).catch(() => {
        this.$router.push('/after-sales/list').catch(() => {})
      })
    },
    handleContact() {
      this.$alert('客服电话：400-888-9999 (工作时间: 9:00-22:00)', '联系客服', {
        confirmButtonText: '确定'
      })
    }
  }
}
</script>

<style scoped>
.service-support {
  padding: 20px;
}

.service-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.service-card {
  flex: 1;
  min-width: 300px;
  text-align: center;
  padding: 20px;
}

.faq-card {
  width: 100%;
  margin-top: 20px;
}

.icon-large {
  font-size: 48px;
  color: var(--primary-color);
  margin-bottom: 10px;
}

.card-header h3 {
  margin: 10px 0;
}

p {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 20px;
}

/* Deep selector for Element UI components to match dark theme */
::v-deep .el-collapse {
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

::v-deep .el-collapse-item__header {
  background-color: var(--card-bg-color);
  color: var(--text-color);
  border-bottom: 1px solid var(--border-color);
  padding-left: 10px; /* Add some padding for better look */
}

::v-deep .el-collapse-item__header.is-active {
  color: var(--primary-color);
}

::v-deep .el-collapse-item__arrow {
  color: var(--text-secondary);
}

::v-deep .el-collapse-item__wrap {
  background-color: var(--card-bg-color);
  border-bottom: 1px solid var(--border-color);
}

::v-deep .el-collapse-item__content {
  color: var(--text-secondary);
  background-color: var(--card-bg-color);
  padding: 10px 10px 20px 10px; /* Adjust padding */
}
</style>
