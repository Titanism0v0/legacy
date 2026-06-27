<template>
  <div class="detail">
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      title="商品详情暂时无法加载，请返回列表后重试"
      class="warn-alert"
    />
    <el-row v-else :gutter="20" v-loading="loading">
      <el-col :span="12">
        <el-carousel height="450px">
          <el-carousel-item v-for="(img, index) in imageList" :key="index">
            <img :src="img" class="detail-img" />
          </el-carousel-item>
        </el-carousel>
      </el-col>

      <el-col :span="12">
        <div class="info">
          <h2>{{ product.title }}</h2>
          <div class="price">{{ formatPrice(product.price, product.currency) }}</div>
          <div class="item">分类：{{ product.categoryName || '-' }}</div>
          <div class="item">库存：{{ product.stock }}</div>
          <div class="item">发货地：{{ product.shippingAddress || '-' }}</div>
          <div class="item" v-if="product.sellerId">
            卖家：
            <Avatar
              :name="product.sellerNickname"
              :src="product.sellerAvatar"
              :size="24"
              class="seller-avatar"
              @click.native="goToSeller"
            />
            <span class="seller-name" @click="goToSeller">{{ product.sellerNickname }}</span>
          </div>

          <el-alert
            v-if="product.restrictedFlag === 1"
            type="warning"
            :closable="false"
            title="该商品属于跨境风控敏感类目，下单前请确认合规与申报义务。"
            class="warn-alert"
          />

          <div v-if="canPurchase" class="actions">
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="product.stock || 1"
              :disabled="!product.stock"
            />
            <el-button
              type="primary"
              icon="el-icon-shopping-cart-2"
              @click="addToCart"
              :disabled="!product.stock"
            >
              加入购物车
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-goods"
              @click="buyNow"
              :disabled="!product.stock"
            >
              立即购买
            </el-button>
          </div>

          <el-card class="price-breakdown" shadow="never" v-loading="estimateLoading">
            <div class="breakdown-header">
              <h3>价格明细</h3>
              <el-button type="text" @click="ruleDialogVisible = true" :disabled="!estimate.totalPrice">
                查看税费和快递规则
              </el-button>
            </div>
            <div class="line">
              <span>商品小计</span>
              <b>{{ formatEstimatePrice(estimate.subtotalPrice) }}</b>
            </div>
            <div class="line">
              <span>国际运费</span>
              <b>{{ formatEstimatePrice(estimate.internationalShippingFee) }}</b>
            </div>
            <div class="line">
              <span>保险费</span>
              <b>{{ formatEstimatePrice(estimate.insuranceFee) }}</b>
            </div>
            <div class="line">
              <span>预估税费</span>
              <b>{{ formatEstimatePrice(estimate.taxEstimatedAmount) }}</b>
            </div>
            <div class="line total">
              <span>页面预估合计</span>
              <b>{{ formatEstimatePrice(estimate.totalPrice) }}</b>
            </div>
            <div class="hint">发货地区：{{ originLabel }}</div>
            <div class="hint">计税口径：{{ taxModeText }}</div>
            <div class="hint" v-if="estimate.paymentFallbackApplied">
              实际支付：{{ formatLiteralPrice(estimate.paymentTotalPrice, estimate.paymentCurrency) }}
            </div>
            <div class="hint">最终以海关审核、支付与物流信息为准。</div>
          </el-card>

          <div class="desc">
            <h3>商品描述</h3>
            <p>{{ product.description }}</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <section v-if="relatedProducts.length > 0" class="related-section">
      <h3 class="related-title">猜你喜欢</h3>
      <div class="related-grid">
        <div
          v-for="item in relatedProducts"
          :key="item.id"
          class="related-card"
          @click="openRelatedProduct(item.id)"
        >
          <img :src="getRelatedImage(item)" class="related-img" />
          <div class="related-info">
            <div class="related-name" :title="item.title">{{ item.title }}</div>
            <div class="related-price">{{ formatPrice(item.price, item.currency) }}</div>
          </div>
        </div>
      </div>
    </section>

    <FeeRuleDialog
      :visible.sync="ruleDialogVisible"
      :estimate="estimate"
      :title="`${product.title || '商品'}税费和快递规则`"
    />
  </div>
</template>

<script>
import { formatPriceDisplay } from '@/utils/currency'
import { productApi, cartApi, orderApi } from '../api'
import currencyMixin from '@/mixins/currencyMixin'
import Avatar from '@/components/Avatar.vue'
import FeeRuleDialog from '@/components/FeeRuleDialog.vue'

export default {
  name: 'ProductDetail',
  components: { Avatar, FeeRuleDialog },
  mixins: [currencyMixin],
  data() {
    return {
      product: {},
      imageList: [],
      quantity: 1,
      relatedProducts: [],
      estimate: {},
      estimateLoading: false,
      ruleDialogVisible: false,
      loading: false,
      loadError: false
    }
  },
  computed: {
    isSellerViewer() {
      return this.$store.getters.userRole === 'SELLER'
    },
    canPurchase() {
      return !this.isSellerViewer
    },
    originLabel() {
      return (this.estimate.ruleSummary && this.estimate.ruleSummary.originLabel) || '-'
    },
    taxModeText() {
      return this.estimate.taxMode === 'CBEC_PREFERENTIAL'
        ? '跨境零售进口优惠口径'
        : '一般贸易估算口径'
    }
  },
  watch: {
    quantity() {
      this.loadEstimate()
    },
    currentCurrency() {
      this.loadEstimate()
    },
    '$route.params.id'() {
      this.load()
    }
  },
  created() {
    this.load()
  },
  methods: {
    async addToCart() {
      if (!this.canPurchase) {
        this.$message.warning('商家账号不支持购买商品')
        return
      }
      if (!this.$store.getters.isAuthenticated) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      try {
        await cartApi.addToCart({
          productId: this.product.id,
          quantity: this.quantity
        })
        this.$message.success('已加入购物车')
      } catch (error) {
        this.$message.error(error.message || '加入购物车失败')
      }
    },
    async buyNow() {
      if (!this.canPurchase) {
        this.$message.warning('商家账号不支持立即购买')
        return
      }
      if (!this.$store.getters.isAuthenticated) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      try {
        await cartApi.addToCart({
          productId: this.product.id,
          quantity: this.quantity
        })
        this.$router.push('/cart')
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    goToSeller() {
      if (this.product.sellerId) {
        this.$router.push(`/seller/${this.product.sellerId}`)
      }
    },
    openRelatedProduct(productId) {
      if (!productId || Number(productId) === Number(this.product.id)) return
      this.$router.push(`/product/${productId}`).catch(() => {})
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    async load() {
      const id = this.$route.params.id
      this.quantity = 1
      this.loading = true
      this.loadError = false
      try {
        const res = await productApi.getProductById(id)
        this.product = res.data || {}

        if (this.product.images) {
          try {
            const parsed = JSON.parse(this.product.images)
            this.imageList = Array.isArray(parsed) ? parsed : [this.product.images]
          } catch (error) {
            this.imageList = this.product.images.includes(',') ? this.product.images.split(',') : [this.product.images]
          }
        } else if (this.product.image) {
          this.imageList = [this.product.image]
        } else {
          this.imageList = ['/placeholder.svg']
        }

        await Promise.all([
          this.loadRecommendations(),
          this.loadEstimate()
        ])
      } catch (error) {
        this.product = {}
        this.imageList = ['/placeholder.svg']
        this.relatedProducts = []
        this.loadError = true
      } finally {
        this.loading = false
      }
    },
    async loadRecommendations() {
      if (!this.product.id) {
        this.relatedProducts = []
        return
      }

      try {
        const res = await productApi.getProductRecommendations(this.product.id, { limit: 6 })
        this.relatedProducts = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        this.relatedProducts = []
      }
    },
    async loadEstimate() {
      if (!this.product.id) return
      this.estimateLoading = true
      try {
        const res = await orderApi.estimate({
          productId: this.product.id,
          quantity: this.quantity,
          settlementCurrency: this.currentCurrency
        })
        this.estimate = res.data || {}
      } catch (error) {
        this.estimate = {}
        this.$message.error(error.message || '加载税费预估失败')
      } finally {
        this.estimateLoading = false
      }
    },
    getRelatedImage(product) {
      if (product.images) {
        try {
          const parsed = JSON.parse(product.images)
          if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
        } catch (error) {}
        if (typeof product.images === 'string' && product.images.includes(',')) return product.images.split(',')[0]
        return product.images
      }
      return product.image || '/placeholder.svg'
    },
    formatEstimatePrice(amount) {
      const currency = this.estimate.displayCurrency || this.currentCurrency || 'CNY'
      return formatPriceDisplay(amount || 0, currency, currency)
    },
    formatLiteralPrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || 'CNY', currency || 'CNY')
    }
  }
}
</script>

<style scoped>
.detail {
  padding: 20px;
}

.detail-img {
  width: 100%;
  height: 450px;
  object-fit: contain;
}

.price {
  color: #ff4d4f;
  font-size: 26px;
  margin: 10px 0;
}

.item {
  margin-bottom: 8px;
}

.seller-avatar {
  margin: 0 5px;
  cursor: pointer;
}

.seller-name {
  cursor: pointer;
}

.actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.warn-alert {
  margin: 12px 0;
}

.price-breakdown {
  margin-top: 16px;
}

.breakdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.breakdown-header h3 {
  margin: 0;
}

.price-breakdown .line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.price-breakdown .line.total {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ddd;
  font-weight: 600;
}

.hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.desc {
  margin-top: 20px;
}

.related-section {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color, #ebeef5);
}

.related-title {
  font-size: 18px;
  margin: 0 0 20px;
  color: var(--text-color, #303133);
  padding-left: 10px;
  border-left: 4px solid var(--primary-color, #409eff);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.related-card {
  background: var(--card-bg-color, #fff);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.related-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.related-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.related-info {
  padding: 12px;
}

.related-name {
  font-size: 14px;
  color: var(--text-color, #303133);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-price {
  font-size: 16px;
  font-weight: bold;
  color: var(--danger-color, #f56c6c);
}
</style>
