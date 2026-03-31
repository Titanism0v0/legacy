<template>
  <div class="seller-overview">
    <div class="overview-header">
      <div>
        <h2>销售总览</h2>
        <p class="overview-subtitle">
          快速查看近期成交、待处理订单和热销商品，方便商家及时判断经营状态。
        </p>
      </div>
      <div class="range-switcher">
        <el-radio-group v-model="selectedDays" size="small" @change="handleRangeChange">
          <el-radio-button :label="7">最近7天</el-radio-button>
          <el-radio-button :label="30">最近30天</el-radio-button>
          <el-radio-button :label="0">全部</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-loading="loading">
      <div class="summary-grid">
        <el-card class="summary-card">
          <div class="summary-label">订单总数</div>
          <div class="summary-value">{{ summary.orderCount }}</div>
          <div class="summary-note">{{ rangeText }}</div>
        </el-card>
        <el-card class="summary-card">
          <div class="summary-label">订单总额</div>
          <div class="summary-value">{{ formatPrice(summary.orderAmount || 0, 'CNY') }}</div>
          <div class="summary-note">按全部订单口径统计</div>
        </el-card>
        <el-card class="summary-card">
          <div class="summary-label">待发货数</div>
          <div class="summary-value">{{ summary.pendingShipmentCount }}</div>
          <div class="summary-note">状态为待发货</div>
        </el-card>
        <el-card class="summary-card">
          <div class="summary-label">退款相关单数</div>
          <div class="summary-value">{{ summary.refundOrderCount }}</div>
          <div class="summary-note">退款状态非 NONE</div>
        </el-card>
      </div>

      <div class="chart-grid">
        <el-card class="overview-card">
          <div slot="header" class="card-header">
            <div>
              <span>销售趋势</span>
              <div class="card-caption">{{ trendCaption }}</div>
            </div>
            <el-button type="text" @click="$router.push('/seller/orders')">查看订单</el-button>
          </div>
          <div v-if="dailyTrend.length > 0" class="chart-box">
            <canvas ref="trendChart"></canvas>
          </div>
          <el-empty
            v-else
            description="当前时间范围内暂无趋势数据"
            :image-size="90"
          />
        </el-card>

        <el-card class="overview-card">
          <div slot="header" class="card-header">
            <div>
              <span>订单状态分布</span>
              <div class="card-caption">帮助你快速识别待处理订单</div>
            </div>
            <el-button type="text" @click="$router.push('/seller/orders')">前往订单管理</el-button>
          </div>
          <div v-if="hasStatusData" class="status-panel">
            <div class="status-chart-box">
              <canvas ref="statusChart"></canvas>
            </div>
            <div class="status-list">
              <div
                v-for="item in statusBreakdownWithColor"
                :key="item.status"
                class="status-list-item"
              >
                <div class="status-list-main">
                  <span class="status-dot" :style="{ backgroundColor: item.color }"></span>
                  <span class="status-name">{{ item.label }}</span>
                </div>
                <span class="status-list-count">{{ item.orderCount }}</span>
              </div>
            </div>
          </div>
          <el-empty
            v-else
            description="当前时间范围内暂无订单状态数据"
            :image-size="90"
          />
        </el-card>
      </div>

      <div class="section-grid">
        <el-card class="overview-card">
          <div slot="header" class="card-header">
            <div>
              <span>状态速览</span>
              <div class="card-caption">按订单状态统计当前时间范围内的数量</div>
            </div>
          </div>
          <div class="status-grid">
            <div v-for="item in normalizedStatusBreakdown" :key="item.status" class="status-item">
              <div class="status-name">{{ item.label }}</div>
              <div class="status-count">{{ item.orderCount }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="overview-card">
          <div slot="header" class="card-header">
            <div>
              <span>热销商品榜</span>
              <div class="card-caption">按销量优先、销售额其次排序</div>
            </div>
            <el-button type="text" @click="$router.push('/seller/products')">前往商品管理</el-button>
          </div>
          <el-table
            v-if="topProducts.length > 0"
            :data="topProducts"
            size="small"
            stripe
            class="compact-table"
          >
            <el-table-column label="商品" min-width="220">
              <template slot-scope="scope">
                <div class="product-cell">
                  <img :src="scope.row.productImage || '/placeholder.png'" class="product-thumb" />
                  <div>
                    <div class="product-title">{{ scope.row.productTitle }}</div>
                    <div class="product-meta">
                      库存：{{ scope.row.currentStock == null ? '-' : scope.row.currentStock }}
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="90" />
            <el-table-column prop="quantitySold" label="售出件数" width="100" />
            <el-table-column label="订单总额" width="130">
              <template slot-scope="scope">
                {{ formatPrice(scope.row.orderAmount || 0, 'CNY') }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-else
            description="当前时间范围内暂无热销商品数据"
            :image-size="90"
          />
        </el-card>
      </div>

      <el-card class="overview-card recent-orders-card">
        <div slot="header" class="card-header">
          <div>
            <span>最近订单预览</span>
            <div class="card-caption">最新成交记录，方便继续处理</div>
          </div>
          <el-button type="text" @click="$router.push('/seller/orders')">查看全部订单</el-button>
        </div>
        <el-table
          v-if="recentOrders.length > 0"
          :data="recentOrders"
          size="small"
          stripe
          class="compact-table"
        >
          <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="productTitle" label="商品" min-width="180" show-overflow-tooltip />
          <el-table-column prop="buyerNickname" label="买家" width="100" show-overflow-tooltip />
          <el-table-column label="金额" width="120">
            <template slot-scope="scope">
              {{ formatPrice(scope.row.totalPrice || 0, 'CNY') }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template slot-scope="scope">
              {{ formatDateTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="goToOrders(scope.row)">继续处理</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-else
          description="当前时间范围内暂无订单"
          :image-size="100"
        />
      </el-card>
    </div>
  </div>
</template>

<script>
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip
} from 'chart.js'
import currencyMixin from '@/mixins/currencyMixin'
import { sellerDashboardApi } from '@/api/sellerDashboard'

Chart.register(
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip
)

const STATUS_META = [
  { status: 'PENDING_PAYMENT', label: '待付款', color: '#f59e0b' },
  { status: 'PAYMENT_PROCESSING', label: '支付处理中', color: '#f97316' },
  { status: 'PENDING_SHIPMENT', label: '待发货', color: '#2f7bff' },
  { status: 'SHIPPED', label: '已发货', color: '#06b6d4' },
  { status: 'COMPLETED', label: '已完成', color: '#10b981' },
  { status: 'CANCELLED', label: '已取消', color: '#94a3b8' }
]

export default {
  name: 'SellerOverview',
  mixins: [currencyMixin],
  data() {
    return {
      loading: false,
      selectedDays: 7,
      trendChart: null,
      statusChart: null,
      overview: {
        summary: {
          orderCount: 0,
          orderAmount: 0,
          pendingShipmentCount: 0,
          refundOrderCount: 0
        },
        statusBreakdown: [],
        dailyTrend: [],
        topProducts: [],
        recentOrders: [],
        range: {
          days: 7,
          allTime: false
        }
      }
    }
  },
  computed: {
    summary() {
      return this.overview.summary || {}
    },
    dailyTrend() {
      return this.overview.dailyTrend || []
    },
    topProducts() {
      return this.overview.topProducts || []
    },
    recentOrders() {
      return this.overview.recentOrders || []
    },
    normalizedStatusBreakdown() {
      const source = this.overview.statusBreakdown || []
      return STATUS_META.map(item => {
        const matched = source.find(row => row.status === item.status)
        return {
          status: item.status,
          label: item.label,
          color: item.color,
          orderCount: matched ? matched.orderCount : 0
        }
      })
    },
    statusBreakdownWithColor() {
      return this.normalizedStatusBreakdown.filter(item => item.orderCount > 0)
    },
    hasStatusData() {
      return this.statusBreakdownWithColor.length > 0
    },
    rangeText() {
      if (this.overview.range && this.overview.range.allTime) {
        return '全部历史'
      }
      const days = this.overview.range && this.overview.range.days
      return days === 30 ? '最近30天' : '最近7天'
    },
    trendCaption() {
      return `${this.rangeText}内按订单创建时间统计`
    }
  },
  created() {
    this.loadOverview()
  },
  beforeDestroy() {
    this.destroyCharts()
  },
  methods: {
    async loadOverview() {
      this.loading = true
      try {
        const params = {
          days: this.selectedDays > 0 ? this.selectedDays : 0
        }
        const res = await sellerDashboardApi.getOverview(params)
        this.overview = res.data || res || this.overview
        this.$nextTick(() => {
          this.renderCharts()
        })
      } catch (error) {
        this.destroyCharts()
        this.$message.error(error.message || '加载销售总览失败')
      } finally {
        this.loading = false
      }
    },
    handleRangeChange() {
      this.loadOverview()
    },
    renderCharts() {
      this.renderTrendChart()
      this.renderStatusChart()
    },
    renderTrendChart() {
      if (this.trendChart) {
        this.trendChart.destroy()
        this.trendChart = null
      }
      const canvas = this.$refs.trendChart
      if (!canvas || this.dailyTrend.length === 0) {
        return
      }

      const labels = this.dailyTrend.map(item => item.dateLabel || item.dateValue || '-')
      const amountData = this.dailyTrend.map(item => Number(item.orderAmount || 0))
      const countData = this.dailyTrend.map(item => Number(item.orderCount || 0))

      this.trendChart = new Chart(canvas.getContext('2d'), {
        data: {
          labels,
          datasets: [
            {
              type: 'line',
              label: '销售额',
              data: amountData,
              yAxisID: 'amount',
              borderColor: '#2f7bff',
              backgroundColor: 'rgba(47, 123, 255, 0.14)',
              fill: true,
              tension: 0.35,
              borderWidth: 2,
              pointRadius: 3,
              pointHoverRadius: 5,
              pointBackgroundColor: '#2f7bff'
            },
            {
              type: 'bar',
              label: '订单数',
              data: countData,
              yAxisID: 'count',
              backgroundColor: 'rgba(16, 185, 129, 0.28)',
              borderColor: 'rgba(16, 185, 129, 0.8)',
              borderWidth: 1,
              borderRadius: 8,
              maxBarThickness: 24
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: {
            mode: 'index',
            intersect: false
          },
          plugins: {
            legend: {
              position: 'top',
              align: 'start'
            },
            tooltip: {
              callbacks: {
                label: context => {
                  if (context.dataset.label === '销售额') {
                    return `销售额：${this.formatPrice(context.raw || 0, 'CNY')}`
                  }
                  return `订单数：${context.raw || 0}`
                }
              }
            }
          },
          scales: {
            x: {
              grid: {
                display: false
              }
            },
            amount: {
              type: 'linear',
              position: 'left',
              beginAtZero: true,
              ticks: {
                callback: value => this.formatAxisAmount(value)
              }
            },
            count: {
              type: 'linear',
              position: 'right',
              beginAtZero: true,
              grid: {
                drawOnChartArea: false
              },
              ticks: {
                precision: 0
              }
            }
          }
        }
      })
    },
    renderStatusChart() {
      if (this.statusChart) {
        this.statusChart.destroy()
        this.statusChart = null
      }
      const canvas = this.$refs.statusChart
      if (!canvas || !this.hasStatusData) {
        return
      }

      this.statusChart = new Chart(canvas.getContext('2d'), {
        type: 'doughnut',
        data: {
          labels: this.statusBreakdownWithColor.map(item => item.label),
          datasets: [
            {
              data: this.statusBreakdownWithColor.map(item => item.orderCount),
              backgroundColor: this.statusBreakdownWithColor.map(item => item.color),
              borderWidth: 0,
              hoverOffset: 8
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: '66%',
          plugins: {
            legend: {
              display: false
            },
            tooltip: {
              callbacks: {
                label: context => `${context.label}：${context.raw || 0}`
              }
            }
          }
        }
      })
    },
    destroyCharts() {
      if (this.trendChart) {
        this.trendChart.destroy()
        this.trendChart = null
      }
      if (this.statusChart) {
        this.statusChart.destroy()
        this.statusChart = null
      }
    },
    formatAxisAmount(value) {
      const numericValue = Number(value || 0)
      if (numericValue >= 10000) {
        return `¥${(numericValue / 10000).toFixed(1)}w`
      }
      if (numericValue >= 1000) {
        return `¥${(numericValue / 1000).toFixed(1)}k`
      }
      return `¥${numericValue}`
    },
    getStatusText(status) {
      const map = {
        PENDING_PAYMENT: '待付款',
        PAYMENT_PROCESSING: '支付处理中',
        PENDING_AUDIT: '待审核',
        PENDING_SHIPMENT: '待发货',
        SHIPPED: '已发货',
        COMPLETED: '已完成',
        CANCELLED: '已取消'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        PENDING_PAYMENT: 'warning',
        PAYMENT_PROCESSING: 'warning',
        PENDING_AUDIT: 'warning',
        PENDING_SHIPMENT: 'info',
        SHIPPED: '',
        COMPLETED: 'success',
        CANCELLED: 'danger'
      }
      return map[status] || ''
    },
    formatDateTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}`
    },
    goToOrders() {
      this.$router.push('/seller/orders')
    }
  }
}
</script>

<style scoped>
.seller-overview {
  padding: 20px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.overview-header h2 {
  margin: 0 0 8px;
}

.overview-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card,
.overview-card {
  border-radius: 12px;
}

.summary-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color);
  line-height: 1.2;
}

.summary-note {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.section-grid {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 20px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.card-caption {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.chart-box {
  height: 320px;
  position: relative;
}

.status-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  align-items: center;
  gap: 20px;
}

.status-chart-box {
  height: 300px;
  position: relative;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.status-list-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-list-count {
  font-weight: 700;
  color: var(--text-color);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-item {
  padding: 16px;
  border-radius: 10px;
  background: var(--bg-color);
  border: 1px solid var(--border-color);
}

.status-name {
  color: var(--text-secondary);
  font-size: 13px;
}

.status-count {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-color);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-title {
  font-size: 13px;
  color: var(--text-color);
  line-height: 1.4;
}

.product-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.compact-table {
  width: 100%;
}

.recent-orders-card {
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid,
  .section-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .status-panel {
    grid-template-columns: 1fr;
  }

  .status-chart-box {
    height: 260px;
  }
}

@media (max-width: 768px) {
  .overview-header {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
