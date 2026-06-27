<template>
  <div class="admin-workbench" v-loading="loading">
    <div class="workbench-header">
      <div>
        <h2>管理工作台</h2>
        <p>优先处理审核、仲裁和风控事项，统一查看平台运行状态。</p>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="selectedDays" size="small" @change="handleRangeChange">
          <el-radio-button :label="7">近 7 天</el-radio-button>
          <el-radio-button :label="30">近 30 天</el-radio-button>
          <el-radio-button :label="0">全部</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="loadOverview" :loading="loading">刷新</el-button>
      </div>
    </div>

    <template v-if="!loading && errorMessage">
      <el-alert :title="errorMessage" type="error" show-icon :closable="false" />
      <div class="state-action">
        <el-button type="primary" size="small" @click="loadOverview">重试</el-button>
      </div>
    </template>

    <template v-else-if="!loading && isEmpty">
      <el-empty description="当前没有待处理事项，状态良好。" :image-size="90" />
    </template>

    <template v-else-if="!loading">
      <section class="section">
        <div class="section-title">关键待办</div>
        <div class="card-grid">
          <el-card
            v-for="item in summaryCards"
            :key="item.key"
            class="task-card"
            shadow="hover"
            @click.native="goTo(item.path)"
          >
            <div class="task-card-main">
              <div class="task-card-label">{{ item.label }}</div>
              <div class="task-card-value">{{ item.value }}</div>
            </div>
            <div class="task-card-link">去处理</div>
          </el-card>
        </div>
      </section>

      <section class="section section-split">
        <el-card class="risk-card" shadow="never">
          <div class="section-title">风险告警</div>
          <div class="risk-main">
            <div class="risk-left">
              <div class="risk-label">高风险/受限商品</div>
              <div class="risk-value">{{ risk.highRiskCount || 0 }}</div>
            </div>
            <div class="risk-right">
              <p>{{ risk.highRiskHintText || '暂无风险提示。' }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="status-card" shadow="never">
          <div class="section-title">订单状态分布</div>
          <div v-if="statusCards.length === 0" class="status-empty">暂无状态数据</div>
          <div v-else class="status-grid">
            <div v-for="item in statusCards" :key="item.status" class="status-item">
              <div class="status-name">{{ item.label }}</div>
              <div class="status-count">{{ item.orderCount }}</div>
            </div>
          </div>
        </el-card>
      </section>

      <section class="section">
        <el-card class="trend-card" shadow="never">
          <div class="trend-header">
            <div>
              <div class="section-title">订单趋势</div>
              <div class="trend-subtitle">
                区间订单数 {{ summary.orderCount || 0 }}，交易额 {{ formatMoney(summary.orderAmount) }}
              </div>
            </div>
          </div>
          <div v-if="orderTrend.length === 0" class="trend-empty">
            <el-empty description="当前区间暂无订单趋势数据" :image-size="80" />
          </div>
          <div v-else ref="trendChart" class="trend-chart" />
        </el-card>
      </section>

      <section class="section">
        <div class="section-title">快捷入口</div>
        <div class="quick-grid">
          <el-card
            v-for="link in quickLinks"
            :key="link.key"
            class="quick-card"
            shadow="hover"
            @click.native="goTo(link.path)"
          >
            <div class="quick-main">
              <i :class="link.icon" class="quick-icon" />
              <div>
                <div class="quick-title">{{ link.title }}</div>
                <div class="quick-desc">{{ link.description }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { adminWorkbenchApi } from '@/api/adminWorkbench'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const STATUS_LABELS = {
  PENDING_PAYMENT: '待付款',
  PAYMENT_PROCESSING: '支付处理中',
  PENDING_AUDIT: '待审核',
  PENDING_SHIPMENT: '待发货',
  PURCHASING: '采购中',
  PURCHASED: '已采购',
  INTL_SHIPPING: '国际运输',
  CUSTOMS_CLEARANCE: '清关中',
  WAREHOUSE_INSPECTION: '仓库验货',
  DOMESTIC_SHIPPING: '国内配送',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

export default {
  name: 'AdminWorkbench',
  data() {
    return {
      loading: false,
      errorMessage: '',
      selectedDays: 7,
      summary: {
        pendingOrderAuditCount: 0,
        pendingProductAuditCount: 0,
        pendingKycCount: 0,
        pendingAfterSalesCount: 0,
        pendingCommunityReviewCount: 0,
        activeFulfillmentCount: 0,
        customsPendingCount: 0,
        orderCount: 0,
        orderAmount: 0
      },
      risk: {
        highRiskCount: 0,
        highRiskHintText: ''
      },
      quickLinks: [],
      orderStatusBreakdown: [],
      orderTrend: [],
      trendChart: null,
      themeObserver: null
    }
  },
  computed: {
    summaryCards() {
      return [
        {
          key: 'orderAudit',
          label: '待订单审核',
          value: this.summary.pendingOrderAuditCount || 0,
          path: '/admin/orders'
        },
        {
          key: 'productAudit',
          label: '待商品审核',
          value: this.summary.pendingProductAuditCount || 0,
          path: '/admin/products'
        },
        {
          key: 'kyc',
          label: '待商家资质审核',
          value: this.summary.pendingKycCount || 0,
          path: '/admin/users'
        },
        {
          key: 'afterSales',
          label: '待售后处理',
          value: this.summary.pendingAfterSalesCount || 0,
          path: '/admin/after-sales'
        },
        {
          key: 'community',
          label: '待社区审核',
          value: this.summary.pendingCommunityReviewCount || 0,
          path: '/admin/community'
        },
        {
          key: 'activeFulfillment',
          label: '履约中订单',
          value: this.summary.activeFulfillmentCount || 0,
          path: '/admin/orders'
        },
        {
          key: 'customsPending',
          label: '清关/验货待办',
          value: this.summary.customsPendingCount || 0,
          path: '/admin/orders'
        }
      ]
    },
    statusCards() {
      return this.orderStatusBreakdown.map((item) => ({
        status: item.status,
        label: STATUS_LABELS[item.status] || item.status,
        orderCount: Number(item.orderCount || 0)
      }))
    },
    isEmpty() {
      const todoTotal = this.summaryCards.reduce((sum, item) => sum + Number(item.value || 0), 0)
      return todoTotal === 0 && Number(this.risk.highRiskCount || 0) === 0 && this.orderTrend.length === 0
    }
  },
  mounted() {
    this.initThemeObserver()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.themeObserver) {
      this.themeObserver.disconnect()
      this.themeObserver = null
    }
    window.removeEventListener('resize', this.handleResize)
    if (this.trendChart) {
      this.trendChart.dispose()
      this.trendChart = null
    }
  },
  created() {
    this.loadOverview()
  },
  methods: {
    async loadOverview() {
      this.loading = true
      this.errorMessage = ''
      try {
        const data = await adminWorkbenchApi.getOverview({ days: this.selectedDays })
        this.summary = data.summary || this.summary
        this.risk = data.risk || this.risk
        this.quickLinks = Array.isArray(data.quickLinks) ? data.quickLinks : []
        this.orderStatusBreakdown = Array.isArray(data.orderStatusBreakdown) ? data.orderStatusBreakdown : []
        this.orderTrend = Array.isArray(data.orderTrend) ? data.orderTrend : []
      } catch (error) {
        this.errorMessage = error.message || '加载工作台数据失败，请稍后重试。'
      } finally {
        this.loading = false
      }
      this.$nextTick(() => {
        this.renderTrendChart()
      })
    },
    handleRangeChange() {
      this.loadOverview()
    },
    goTo(path) {
      if (!path) return
      this.$router.push(path)
    },
    formatMoney(value) {
      const amount = Number(value || 0)
      return `¥ ${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    },
    initThemeObserver() {
      this.themeObserver = new MutationObserver(() => {
        this.renderTrendChart()
      })
      this.themeObserver.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['data-theme']
      })
    },
    handleResize() {
      if (this.trendChart) {
        this.trendChart.resize()
      }
    },
    renderTrendChart() {
      const currentChartEl = this.$refs.trendChart

      if (!currentChartEl || this.orderTrend.length === 0) {
        if (this.trendChart) {
          this.trendChart.dispose()
          this.trendChart = null
        }
        return
      }

      if (this.trendChart && this.trendChart.getDom() !== currentChartEl) {
        this.trendChart.dispose()
        this.trendChart = null
      }

      if (!this.trendChart) {
        this.trendChart = echarts.init(currentChartEl)
      }

      const rootStyles = getComputedStyle(document.documentElement)
      const textColor = rootStyles.getPropertyValue('--text-secondary').trim() || '#909399'
      const titleColor = rootStyles.getPropertyValue('--text-color').trim() || '#303133'
      const borderColor = rootStyles.getPropertyValue('--border-color').trim() || '#e4e7ed'
      const barColor = '#3375E0'
      const lineColor = '#10B981'

      const labels = this.orderTrend.map((item) => item.dateLabel || item.dateValue || '')
      const countData = this.orderTrend.map((item) => Number(item.orderCount || 0))
      const amountData = this.orderTrend.map((item) => Number(item.orderAmount || 0))

      this.trendChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: 40,
          right: 40,
          top: 24,
          bottom: 28
        },
        legend: {
          data: ['订单数', '交易额'],
          textStyle: {
            color: textColor
          }
        },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: {
            color: textColor
          },
          axisLine: {
            lineStyle: {
              color: borderColor
            }
          }
        },
        yAxis: [
          {
            type: 'value',
            name: '订单数',
            axisLabel: {
              color: textColor
            },
            splitLine: {
              lineStyle: {
                color: borderColor
              }
            },
            nameTextStyle: {
              color: titleColor
            }
          },
          {
            type: 'value',
            name: '交易额',
            axisLabel: {
              color: textColor
            },
            splitLine: {
              show: false
            },
            nameTextStyle: {
              color: titleColor
            }
          }
        ],
        series: [
          {
            name: '订单数',
            type: 'bar',
            yAxisIndex: 0,
            itemStyle: {
              color: barColor
            },
            data: countData
          },
          {
            name: '交易额',
            type: 'line',
            smooth: true,
            yAxisIndex: 1,
            itemStyle: {
              color: lineColor
            },
            lineStyle: {
              width: 2
            },
            data: amountData
          }
        ]
      })
    }
  }
}
</script>

<style scoped>
.admin-workbench {
  padding: 20px;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.workbench-header h2 {
  margin: 0 0 8px;
  color: var(--text-color);
}

.workbench-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  margin-bottom: 12px;
  color: var(--text-color);
  font-size: 15px;
  font-weight: 600;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.task-card {
  cursor: pointer;
  border-radius: 10px;
}

.task-card-main {
  min-height: 70px;
}

.task-card-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.task-card-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--text-color);
}

.task-card-link {
  margin-top: 6px;
  font-size: 12px;
  color: var(--primary-color);
}

.section-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.risk-card,
.status-card,
.trend-card {
  border-radius: 10px;
}

.risk-main {
  display: flex;
  gap: 18px;
}

.risk-left {
  min-width: 180px;
  padding-right: 16px;
  border-right: 1px solid var(--border-color);
}

.risk-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.risk-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--danger-color);
}

.risk-right {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.risk-right p {
  margin: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.status-item {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-color);
}

.status-name {
  color: var(--text-secondary);
  font-size: 12px;
}

.status-count {
  margin-top: 8px;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--text-color);
}

.status-empty {
  color: var(--text-secondary);
  font-size: 13px;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.trend-subtitle {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.trend-chart {
  width: 100%;
  height: 320px;
}

.trend-empty {
  padding-top: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  cursor: pointer;
  border-radius: 10px;
}

.quick-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 70px;
}

.quick-icon {
  font-size: 24px;
  color: var(--primary-color);
}

.quick-title {
  color: var(--text-color);
  font-size: 14px;
  font-weight: 600;
}

.quick-desc {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.state-action {
  margin-top: 12px;
}

@media (max-width: 1200px) {
  .card-grid,
  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .section-split {
    grid-template-columns: 1fr;
  }

  .status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .workbench-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .card-grid,
  .quick-grid,
  .status-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .risk-main {
    flex-direction: column;
  }

  .risk-left {
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    padding-right: 0;
    padding-bottom: 10px;
  }

  .trend-chart {
    height: 260px;
  }
}
</style>
