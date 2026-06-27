<template>
  <div class="guide-page">
    <el-card shadow="never">
      <div class="guide-header">
        <h2>跨境购买规则说明</h2>
        <p>
          本平台按中国跨境电商零售进口政策估算税费，并按商品发货地估算国际运费和保险费。
          页面展示金额仅用于下单参考，税费最终以海关审核结果为准。
        </p>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        title="跨境订单税费、时效与售后规则不同于国内电商，下单前请先查看本页和结算页的规则说明。"
      />

      <div class="section">
        <h3>1. 税费估算口径</h3>
        <p>
          自 2019 年 1 月 1 日起，符合跨境电商零售进口政策的订单，在单次交易限值内适用优惠口径：
          关税暂按 0% 计征，进口环节增值税、消费税按法定应纳税额的 70% 征收。
        </p>
        <p>
          若商品不适用跨境零售进口优惠，或完税价格超过 5000 元，则切换为一般贸易估算口径，
          按关税、进口增值税、进口消费税全额估算。
        </p>
        <el-alert
          type="info"
          :closable="false"
          title="平台当前仅提示 26000 元年度交易限值，不在第一版中做自动拦截。"
        />
      </div>

      <div class="section">
        <h3>2. 完税价格如何计算</h3>
        <p>完税价格 = 商品小计（人民币） + 国际运费（人民币） + 保险费（人民币）。</p>
        <p>
          其中税费档案来自商品所属类目，系统会自动识别是否适用跨境零售进口优惠口径，
          并在商品页、购物车和结算弹窗中展示关税、增值税、消费税的拆分结果。
        </p>
      </div>

      <div class="section">
        <h3>3. 发货地如何影响运费</h3>
        <p>发货地不直接改变法定税率，但会影响国际段运费、保险费、预计时效和风险提示。</p>
        <el-table :data="zoneRules" border size="small" class="zone-table">
          <el-table-column prop="name" label="发货分区" min-width="130" />
          <el-table-column prop="regions" label="识别关键词" min-width="260" />
          <el-table-column prop="shippingRule" label="费用规则" min-width="220" />
        </el-table>
        <p class="hint">
          国际运费 = 基础运费 + max(件数 - 1, 0) × 续件费；保险费 = max((商品小计 + 国际运费) × 保险费率, 2.00)。
        </p>
      </div>

      <div class="section">
        <h3>4. 币种展示与支付说明</h3>
        <p>
          首页切换的币种用于展示估算金额，税费计算仍以人民币为基准。若当前展示币种不是人民币，
          结算页会明确提示“页面为展示币种，实际支付按人民币结算”。
        </p>
        <p>
          支付前请重点查看本次规则弹窗中的展示币种金额、人民币应付金额、计税口径和发货分区说明。
        </p>
      </div>

      <div class="section">
        <h3>5. 清关与售后提醒</h3>
        <p>
          跨境订单需要经过申报、运输、清关和派送等环节，特殊类目或敏感地区可能触发更严格审核，
          物流时效会比国内订单更长。
        </p>
        <p>
          若申请售后，请按页面要求提交图片、视频或文字说明。税费、清关和物流问题会纳入售后判断，
          复杂争议由平台继续审核处理。
        </p>
      </div>

      <div class="section">
        <h3>6. 风险与合规提示</h3>
        <ul class="guide-list">
          <li>税费为平台根据当前政策和类目档案作出的估算，最终以海关审核为准。</li>
          <li>若商品不在跨境零售进口适用品类内，系统会回退为一般贸易估算口径。</li>
          <li>部分国家或地区的物流限制、抽检和退运风险会在结算规则弹窗中提示。</li>
          <li>请在提交订单前确认已阅读税费和快递规则，再进行支付。</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CrossBorderGuide',
  data() {
    return {
      zoneRules: [
        {
          name: '亚洲近邻',
          regions: '日本、韩国、香港、澳门、台湾、新加坡、泰国、马来西亚',
          shippingRule: '基础运费 18，续件 5，保险费率 1.0%'
        },
        {
          name: '太平洋区域',
          regions: '美国、加拿大、澳大利亚、新西兰',
          shippingRule: '基础运费 32，续件 8，保险费率 1.2%'
        },
        {
          name: '欧洲区域',
          regions: '英国、法国、德国、意大利、西班牙、荷兰、瑞士、瑞典、挪威、丹麦',
          shippingRule: '基础运费 36，续件 10，保险费率 1.2%'
        },
        {
          name: '全球其他区域',
          regions: '其余国家或无法识别的发货地',
          shippingRule: '基础运费 42，续件 12，保险费率 1.5%'
        }
      ]
    }
  }
}
</script>

<style scoped>
.guide-page {
  padding: 20px;
  max-width: 980px;
  margin: 0 auto;
}

.guide-header {
  margin-bottom: 20px;
}

.guide-header h2 {
  margin: 0 0 10px;
}

.guide-header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.section {
  margin-top: 22px;
}

.section h3 {
  margin-bottom: 10px;
  color: var(--text-color);
}

.section p {
  margin: 0 0 10px;
  line-height: 1.9;
  color: var(--text-secondary);
}

.zone-table {
  margin: 12px 0;
}

.hint {
  font-size: 13px;
}

.guide-list {
  margin: 0;
  padding-left: 20px;
  color: var(--text-secondary);
}

.guide-list li {
  margin-bottom: 10px;
  line-height: 1.8;
}
</style>
