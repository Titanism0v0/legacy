<template>
  <el-dialog
    :visible.sync="dialogVisible"
    :title="dialogTitle"
    width="720px"
    append-to-body
  >
    <div v-if="estimate" class="fee-rule-dialog">
      <el-alert
        v-if="estimate.paymentFallbackApplied"
        type="warning"
        :closable="false"
        title="页面金额按当前币种展示，实际支付与订单快照统一按人民币结算。"
        class="dialog-alert"
      />

      <div class="summary-card">
        <div class="summary-row">
          <span>商品小计</span>
          <b>{{ formatLiteralPrice(estimate.subtotalPrice, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>国际运费</span>
          <b>{{ formatLiteralPrice(estimate.internationalShippingFee, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>保险费</span>
          <b>{{ formatLiteralPrice(estimate.insuranceFee, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>完税价格</span>
          <b>{{ formatLiteralPrice(estimate.dutiablePrice, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>关税</span>
          <b>{{ formatLiteralPrice(estimate.tariffAmount, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>进口增值税</span>
          <b>{{ formatLiteralPrice(estimate.vatAmount, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row">
          <span>进口消费税</span>
          <b>{{ formatLiteralPrice(estimate.consumptionTaxAmount, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row total-row">
          <span>预估税费合计</span>
          <b>{{ formatLiteralPrice(estimate.taxEstimatedAmount, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row total-row">
          <span>页面预估合计</span>
          <b>{{ formatLiteralPrice(estimate.totalPrice, estimate.displayCurrency) }}</b>
        </div>
        <div class="summary-row" v-if="estimate.paymentFallbackApplied">
          <span>实际人民币支付</span>
          <b>{{ formatLiteralPrice(estimate.paymentTotalPrice, estimate.paymentCurrency) }}</b>
        </div>
      </div>

      <div class="rule-grid">
        <div class="rule-panel">
          <h4>税费规则</h4>
          <p>{{ estimate.ruleSummary && estimate.ruleSummary.taxRuleNote }}</p>
          <p>税费口径：{{ estimate.taxMode === 'CBEC_PREFERENTIAL' ? '跨境零售进口优惠口径' : '一般贸易估算口径' }}</p>
          <p>税费版本：{{ estimate.ruleVersion || 'CBEC-IMPORT-V1' }}</p>
        </div>
        <div class="rule-panel">
          <h4>快递规则</h4>
          <p>发货地区：{{ estimate.ruleSummary && estimate.ruleSummary.originLabel }}</p>
          <p>{{ estimate.ruleSummary && estimate.ruleSummary.shippingRuleNote }}</p>
          <p>运费与保险费已计入完税价格估算。</p>
        </div>
      </div>

      <div class="notice-panel">
        <h4>政策提示</h4>
        <p>{{ estimate.ruleSummary && estimate.ruleSummary.policyNotice }}</p>
        <p>{{ estimate.ruleSummary && estimate.ruleSummary.annualLimitNotice }}</p>
        <p>{{ estimate.ruleSummary && estimate.ruleSummary.paymentNotice }}</p>
        <p>最终税费、清关与放行结果以海关审核、申报数据和支付/物流信息为准。</p>
      </div>
    </div>
    <span slot="footer">
      <el-button @click="dialogVisible = false">关闭</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { formatPriceDisplay } from '@/utils/currency'

export default {
  name: 'FeeRuleDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    estimate: {
      type: Object,
      default: () => ({})
    },
    title: {
      type: String,
      default: ''
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    dialogTitle() {
      return this.title || '税费和快递规则'
    }
  },
  methods: {
    formatLiteralPrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || 'CNY', currency || 'CNY')
    }
  }
}
</script>

<style scoped>
.fee-rule-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-alert {
  margin-bottom: 4px;
}

.summary-card {
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.8;
}

.total-row {
  font-weight: 600;
}

.rule-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.rule-panel,
.notice-panel {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  background: #fff;
}

.rule-panel h4,
.notice-panel h4 {
  margin: 0 0 10px;
}

.rule-panel p,
.notice-panel p {
  margin: 0 0 8px;
  line-height: 1.6;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .rule-grid {
    grid-template-columns: 1fr;
  }
}
</style>
