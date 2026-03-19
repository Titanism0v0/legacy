<template>
  <div class="seller-detail">
    <!-- 卖家信息卡片 -->
    <div class="seller-info-card">
      <div class="seller-profile">
        <Avatar :src="sellerInfo.avatar" :name="sellerInfo.nickname" :size="80" />
        <div class="seller-meta">
          <h2 class="seller-name">{{ sellerInfo.nickname }}</h2>
          <div class="seller-stats">
            <span class="stat-item">
              商品数：<span class="stat-value">{{ total }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 导航栏：全部商品 | 评价 -->
    <div class="section-nav">
      <span
        :class="['nav-item', { active: activeTab === 'products' }]"
        @click="activeTab = 'products'"
      >全部商品</span>
      <span class="nav-divider"></span>
      <span
        :class="['nav-item', { active: activeTab === 'reviews' }]"
        @click="switchToReviews"
      >评价</span>
    </div>

    <!-- 全部商品 -->
    <div v-show="activeTab === 'products'" class="products-section">
      <div v-loading="loading" class="product-grid" v-if="productList.length > 0">
        <el-card
          v-for="p in productList"
          :key="p.id"
          class="product-card"
          @click.native="goProductDetail(p.id)"
          :body-style="{ padding: '0px' }"
        >
          <div class="image-wrapper">
            <img :src="getImage(p)" class="product-image" />
          </div>
          <div class="product-info">
            <div class="product-title" :title="p.title">{{ p.title }}</div>
            <div class="product-price">{{ formatPrice(p.price, p.currency) }}</div>
            <div class="product-stock">库存: {{ p.stock }}</div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="该卖家暂无在售商品"></el-empty>
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="loadSellerProducts"
        />
      </div>
    </div>

    <!-- 评价列表 -->
    <div v-show="activeTab === 'reviews'" class="reviews-section">
      <div v-loading="reviewsLoading" v-if="reviewList.length > 0" class="review-list">
        <div v-for="r in reviewList" :key="r.id" class="review-item">
          <div class="review-header">
            <Avatar :src="r.buyerAvatar" :name="r.buyerNickname" :size="40" class="review-avatar" />
            <div class="review-meta">
              <span class="review-nickname">{{ r.buyerNickname || '用户' }}</span>
              <span class="review-time">{{ formatReviewTime(r.createTime) }}</span>
              <el-rate :value="Number(r.rating)" disabled :max="5" class="review-stars" />
            </div>
            <el-button
              v-if="canDeleteReview(r)"
              type="text"
              size="small"
              class="review-delete"
              @click="handleDeleteReview(r)"
            >删除</el-button>
          </div>
          <div class="review-content-box">{{ r.content || '（无内容）' }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无评价"></el-empty>
    </div>
  </div>
</template>

<script>
import { productApi, sellerReviewApi } from '@/api'
import currencyMixin from '@/mixins/currencyMixin'
import Avatar from '@/components/Avatar.vue'
import store from '@/store'

export default {
  name: 'SellerDetail',
  components: { Avatar },
  mixins: [currencyMixin],
  data() {
    return {
      sellerId: null,
      sellerInfo: {
        nickname: '卖家',
        avatar: ''
      },
      productList: [],
      loading: false,
      currentPage: 1,
      pageSize: 12,
      total: 0,
      activeTab: 'products',
      reviewList: [],
      reviewsLoading: false
    }
  },
  created() {
    this.sellerId = this.$route.params.id
    if (this.sellerId) {
      this.loadSellerInfo()
      this.loadSellerProducts()
    }
  },
  methods: {
    async loadSellerInfo() {},

    async loadSellerProducts() {
      this.loading = true
      try {
        const res = await productApi.getSellerProducts(this.sellerId, {
          page: this.currentPage,
          size: this.pageSize,
          status: 'ON_SALE'
        })
        const data = res.data || res
        this.productList = data.records || []
        this.total = data.total != null ? data.total : 0
        if (this.productList.length > 0) {
          const first = this.productList[0]
          this.sellerInfo = {
            nickname: first.sellerNickname || '卖家',
            avatar: first.sellerAvatar
          }
        }
      } catch (error) {
        console.error(error)
        this.$message.error('加载商品失败')
      } finally {
        this.loading = false
      }
    },

    switchToReviews() {
      this.activeTab = 'reviews'
      if (this.reviewList.length === 0 && this.sellerId) {
        this.loadReviews()
      }
    },

    async loadReviews() {
      if (!this.sellerId) return
      this.reviewsLoading = true
      try {
        const res = await sellerReviewApi.getList(Number(this.sellerId))
        const list = (res && res.data) ? res.data : (Array.isArray(res) ? res : [])
        this.reviewList = Array.isArray(list) ? list : []
      } catch (e) {
        console.error(e)
        this.reviewList = []
      } finally {
        this.reviewsLoading = false
      }
    },

    formatReviewTime(t) {
      if (!t) return ''
      const d = typeof t === 'string' ? new Date(t) : t
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const min = String(d.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${day} ${h}:${min}`
    },

    canDeleteReview(r) {
      const uid = store.state.user && store.state.user.id
      if (!uid) return false
      return Number(r.buyerId) === Number(uid)
    },

    async handleDeleteReview(r) {
      try {
        await this.$confirm('确定删除这条评价吗？', '提示', {
          type: 'warning'
        })
      } catch {
        return
      }
      try {
        await sellerReviewApi.delete(r.id)
        this.$message.success('已删除')
        this.loadReviews()
      } catch (e) {
        this.$message.error(e.message || e.response?.data?.message || '删除失败')
      }
    },

    getImage(p) {
      if (p.images) {
        try {
          const parsed = typeof p.images === 'string' ? JSON.parse(p.images) : p.images
          if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
        } catch (e) {}
        if (typeof p.images === 'string' && p.images.includes(',')) return p.images.split(',')[0]
        return p.images
      }
      return p.image || ''
    },

    goProductDetail(id) {
      this.$router.push(`/product/${id}`)
    },

    // 从卖家详情页发起聊天
    contactSeller() {
      if (!this.sellerId) {
        this.$message.warning('未找到卖家信息')
        return
      }
      this.$router.push({
        path: '/chat',
        query: { sellerId: this.sellerId }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.seller-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.seller-info-card {
  background-color: var(--card-bg-color);
  border-radius: var(--card-radius);
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: var(--card-shadow);
  display: flex;
  align-items: center;

  .seller-profile {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .seller-meta {
    .seller-name {
      margin: 0 0 10px 0;
      font-size: 24px;
      color: var(--text-color);
    }
    
    .seller-stats {
      color: var(--text-secondary);
      font-size: 14px;
      
      .stat-value {
        color: var(--primary-color);
        font-weight: bold;
        font-size: 16px;
      }
    }
  }
}

.section-nav {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  font-size: 20px;
  font-weight: bold;
  color: var(--text-color);
  .nav-item {
    cursor: pointer;
    color: var(--text-secondary);
    &.active {
      color: var(--text-color);
    }
    &:hover {
      color: var(--primary-color);
    }
  }
  .nav-divider {
    width: 4px;
    height: 20px;
    margin: 0 16px;
    background: var(--primary-color);
    border-radius: 2px;
  }
}

.section-title {
  font-size: 20px;
  margin-bottom: 20px;
  color: var(--text-color);
  border-left: 4px solid var(--primary-color);
  padding-left: 10px;
}

.reviews-section {
  min-height: 200px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  background-color: var(--card-bg-color);
  border-radius: var(--card-radius);
  padding: 20px;
  box-shadow: var(--card-shadow);
}

.review-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  .review-avatar {
    flex-shrink: 0;
    margin-right: 12px;
  }
  .review-meta {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
  }
  .review-nickname {
    font-weight: 600;
    font-size: 16px;
    color: var(--text-color);
  }
  .review-time {
    font-size: 13px;
    color: var(--text-secondary);
  }
  .review-stars {
    margin: 0;
  }
  .review-delete {
    color: var(--danger-color);
  }
}

.review-content-box {
  padding: 12px 16px;
  background: var(--bg-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-color);
  line-height: 1.6;
  border: 1px solid var(--border-color, rgba(0,0,0,0.06));
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.product-card {
  border: none;
  background-color: var(--card-bg-color);
  transition: transform 0.3s;
  cursor: pointer;
  
  &:hover {
    transform: translateY(-5px);
  }

  .image-wrapper {
    height: 200px;
    overflow: hidden;
    
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .product-info {
    padding: 15px;

    .product-title {
      font-size: 16px;
      font-weight: 500;
      color: var(--text-color);
      margin-bottom: 8px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .product-price {
      color: var(--danger-color);
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 5px;
    }

    .product-stock {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
}

.pagination-container {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}
</style>