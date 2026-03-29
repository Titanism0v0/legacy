<template>
  <div class="seller-overview">
    <div class="overview-header">
      <div>
        <h2>销售总览</h2>
        <p class="overview-subtitle">快速查看近期订单、金额、待处理事项和热销商品。</p>
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

      <div class="section-grid">
        <el-card class="overview-card">
          <div slot="header" class="card-header">
            <span>订单状态分布</span>
            <el-button type="text" @click="$router.push('/seller/orders')">前往订单管理</el-button>
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
            <span>热销商品榜</span>
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
                    <div class="product-meta">库存：{{ scope.row.currentStock == null ? '-' : scope.row.currentStock }}</div>
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
          <el-empty v-else description="当前时间范围内暂无热销商品数据" :image-size="90" />
        </el-card>
      </div>

      <el-card class="overview-card recent-orders-card">
        <div slot="header" class="card-header">
          <span>最近订单预览</span>
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
        <el-empty v-else description="当前时间范围内暂无订单" :image-size="100" />
      </el-card>
    </div>
  </div>
</template>

<script>
import currencyMixin from '@/mixins/currencyMixin'
import { sellerDashboardApi } from '@/api/sellerDashboard'

const STATUS_META = [
  { status: 'PENDING_PAYMENT', label: '待付款' },
  { status: 'PAYMENT_PROCESSING', label: '支付处理中' },
  { status: 'PENDING_SHIPMENT', label: '待发货' },
  { status: 'SHIPPED', label: '已发货' },
  { status: 'COMPLETED', label: '已完成' },
  { status: 'CANCELLED', label: '已取消' }
]

export default {
  name: 'SellerOverview',
  mixins: [currencyMixin],
  data() {
    return {
      loading: false,
      selectedDays: 7,
      overview: {
        summary: {
          orderCount: 0,
          orderAmount: 0,
          pendingShipmentCount: 0,
          refundOrderCount: 0
        },
        statusBreakdown: [],
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
          orderCount: matched ? matched.orderCount : 0
        }
      })
    },
    rangeText() {
      if (this.overview.range && this.overview.range.allTime) {
        return '全部历史'
      }
      return this.selectedDays === 30 ? '最近30天' : '最近7天'
    }
  },
  created() {
    this.loadOverview()
  },
  methods: {
    async loadOverview() {
      this.loading = true
      try {
        const params = {}
        if (this.selectedDays > 0) {
          params.days = this.selectedDays
        } else {
          params.days = 0
        }
        const res = await sellerDashboardApi.getOverview(params)
        this.overview = res.data || res || this.overview
      } catch (error) {
        this.$message.error(error.message || '加载销售总览失败')
      } finally {
        this.loading = false
      }
    },
    handleRangeChange() {
      this.loadOverview()
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

.summary-card {
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

.section-grid {
  display: grid;
  grid-template-columns: 1.1fr 1.4fr;
  gap: 20px;
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

  .section-grid {
    grid-template-columns: 1fr;
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
