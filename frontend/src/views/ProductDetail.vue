<template>
  <div class="detail">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-carousel height="450px">
          <el-carousel-item v-for="(img, i) in imageList" :key="i">
            <img :src="img" class="detail-img" />
          </el-carousel-item>
        </el-carousel>
      </el-col>

      <el-col :span="12">
        <div class="info">
          <h2>{{ product.title }}</h2>
          <div class="price">{{ formatPrice(product.price, product.currency) }}</div>
          <div class="item">分类：{{ product.categoryName }}</div>
          <div class="item">库存：{{ product.stock }}</div>
          <div class="item">发货地址：{{ product.shippingAddress }}</div>
          <div class="item" v-if="product.sellerId">
            卖家：
            <Avatar :name="product.sellerNickname" :src="product.sellerAvatar" :size="24" class="seller-avatar" @click.native="goToSeller" />
            <span class="seller-name" @click="goToSeller">{{ product.sellerNickname }}</span>
          </div>
          <el-alert
            v-else-if="isSellerViewer"
            type="info"
            :closable="false"
            title="商家账号仅浏览商品信息，不显示购物车和购买入口。"
            class="warn-alert"
          />

          <el-alert
            v-if="product.restrictedFlag === 1"
            type="warning"
            :closable="false"
            title="该商品属于受限风控类目，下单前请确认合规声明。"
            class="warn-alert"
          />

          <div v-if="canPurchase" class="actions">
            <el-input-number v-model="quantity" :min="1" :max="product.stock || 1" :disabled="!product.stock" />
            <el-button type="primary" icon="el-icon-shopping-cart-2" @click="addToCart" :disabled="!product.stock">加入购物车</el-button>
            <el-button type="danger" icon="el-icon-goods" @click="buyNow" :disabled="!product.stock">立即购买</el-button>
          </div>

          <el-card class="price-breakdown" shadow="never">
            <h3>价格明细</h3>
            <div class="line"><span>商品小计</span><b>{{ formatPrice(estimate.subtotalPrice || 0, product.currency) }}</b></div>
            <div class="line"><span>预估税费（{{ ((estimate.taxRateSnapshot || 0) * 100).toFixed(1) }}%）</span><b>{{ formatPrice(estimate.taxEstimatedAmount || 0, product.currency) }}</b></div>
            <div class="line"><span>运费</span><b>{{ formatPrice(estimate.shippingFeeSnapshot || 0, product.currency) }}</b></div>
            <div class="line total"><span>合计（{{ estimate.taxIncludedFlag === 1 ? '含税' : '未税' }}）</span><b>{{ formatPrice(estimate.totalPrice || 0, product.currency) }}</b></div>
            <div class="hint">清关状态：{{ estimate.customsClearanceStatus || 'PENDING_DECLARATION' }}</div>
          </el-card>

          <div class="desc">
            <h3>商品描述</h3>
            <p>{{ product.description }}</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <section v-if="relatedProducts.length > 0" class="related-section">
      <h3 class="related-title">可能感兴趣的商品</h3>
      <div class="related-grid">
        <div
          v-for="p in relatedProducts"
          :key="p.id"
          class="related-card"
          @click="openRelatedProduct(p.id)"
        >
          <img :src="getRelatedImage(p)" class="related-img" />
          <div class="related-info">
            <div class="related-name" :title="p.title">{{ p.title }}</div>
            <div class="related-price">{{ formatPrice(p.price, p.currency) }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { productApi, cartApi, orderApi } from '../api'
import currencyMixin from '@/mixins/currencyMixin'
import { estimateByLocalRule } from '@/utils/crossborder'
import Avatar from '@/components/Avatar.vue'

export default {
  components: { Avatar },
  mixins: [currencyMixin],
  computed: {
    isSellerViewer() {
      return this.$store.getters.userRole === 'SELLER'
    },
    canPurchase() {
      return !this.isSellerViewer
    }
  },
  data() {
    return {
      product: {},
      imageList: [],
      quantity: 1,
      relatedProducts: [],
      estimate: {}
    }
  },
  watch: {
    quantity() {
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
        this.$message.warning('商家账号不支持加入购物车')
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
      if (this.product.sellerId) this.$router.push(`/seller/${this.product.sellerId}`)
    },
    openRelatedProduct(productId) {
      if (!productId || Number(productId) === Number(this.product.id)) return
      this.$router.push(`/product/${productId}`).catch(() => {})
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    async load() {
      const id = this.$route.params.id
      this.quantity = 1
      const res = await productApi.getProductById(id)
      this.product = res.data

      if (this.product.images) {
        try {
          const parsed = JSON.parse(this.product.images)
          this.imageList = Array.isArray(parsed) ? parsed : [this.product.images]
        } catch (e) {
          this.imageList = this.product.images.includes(',') ? this.product.images.split(',') : [this.product.images]
        }
      } else if (this.product.image) {
        this.imageList = [this.product.image]
      }

      if (this.product.categoryId) {
        try {
          const relatedRes = await productApi.getProductList({
            categoryId: this.product.categoryId,
            status: 'ON_SALE',
            page: 1,
            size: 8
          })
          const list = relatedRes.data && relatedRes.data.records ? relatedRes.data.records : []
          this.relatedProducts = list.filter(item => item.id !== this.product.id)
        } catch (e) {
          this.relatedProducts = []
        }
      }
      await this.loadEstimate()
    },
    async loadEstimate() {
      if (!this.product.id) return
      try {
        const res = await orderApi.estimate({ productId: this.product.id, quantity: this.quantity })
        this.estimate = res.data || {}
      } catch (e) {
        this.estimate = estimateByLocalRule(this.product, this.quantity)
      }
    },
    getRelatedImage(p) {
      if (p.images) {
        try {
          const parsed = JSON.parse(p.images)
          if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
        } catch (e) {}
        if (typeof p.images === 'string' && p.images.includes(',')) return p.images.split(',')[0]
        return p.images
      }
      return p.image || '/placeholder.png'
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
}

.warn-alert {
  margin: 12px 0;
}

.price-breakdown {
  margin-top: 16px;
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
}

.hint {
  color: #909399;
  font-size: 12px;
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
  margin: 0 0 20px 0;
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
