<template>
  <div class="product-detail" v-loading="loading">
    <el-row :gutter="20" v-if="product">
      <el-col :span="12">
        <el-card>
          <img :src="product.image || '/placeholder.png'" :alt="product.title" style="width: 100%;" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
            <h2>{{ product.title }}</h2>
            <div class="price">{{ formatPrice(product.price, product.currency) }}</div>
            <div class="info-item">
              <span>分类：</span>
              <span>{{ product.categoryName }}</span>
          </div>
          <div class="info-item">
            <span>库存：</span>
            <span>{{ product.stock }}</span>
          </div>
          <div class="info-item">
            <span>发货地址：</span>
            <span>{{ product.shippingAddress }}</span>
          </div>
          <div class="info-item">
            <span>卖家：</span>
            <span>{{ product.sellerNickname }}</span>
          </div>
          <div class="description">
            <h3>商品描述</h3>
            <p>{{ product.description }}</p>
          </div>
          <div class="actions" v-if="!isAdmin">
            <el-input-number v-model="quantity" :min="1" :max="product.stock" style="margin-right: 10px;"></el-input-number>
            <el-button type="primary" @click="addToCart" :disabled="!isAuthenticated">加入购物车</el-button>
            <el-button type="success" @click="buyNow" :disabled="!isAuthenticated">立即购买</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { productApi, cartApi } from '../api'
import { mapGetters } from 'vuex'

export default {
  name: 'ProductDetail',
  data() {
    return {
      product: null,
      quantity: 1,
      loading: false
    }
  },
  computed: {
    ...mapGetters(['isAuthenticated', 'userRole']),
    isAdmin() {
      return this.userRole === 'ADMIN'
    }
  },
  created() {
    this.loadProduct()
  },
  methods: {
    async loadProduct() {
      this.loading = true
      try {
        const id = this.$route.params.id
        const res = await productApi.getProductById(id)
        this.product = res.data
      } catch (error) {
        this.$message.error('加载商品失败')
        this.$router.push('/home')
      } finally {
        this.loading = false
      }
    },
    async addToCart() {
      if (!this.isAuthenticated) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      
      try {
        await cartApi.addToCart({
          productId: this.product.id,
          quantity: this.quantity
        })
        this.$message.success('已添加到购物车')
      } catch (error) {
        this.$message.error(error.message || '添加失败')
      }
    },
    buyNow() {
      if (!this.isAuthenticated) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      
      // 跳转到订单确认页面或直接创建订单
      this.$router.push({
        path: '/orders',
        query: {
          productId: this.product.id,
          quantity: this.quantity
        }
      })
    }
  }
}
</script>

<style scoped>
.product-detail {
  padding: 20px;
}

.price {
  font-size: 28px;
  color: var(--danger-color);
  font-weight: bold;
  margin: 20px 0;
}

.info-item {
  margin: 10px 0;
  font-size: 14px;
  color: var(--text-color);
}

.info-item span:first-child {
  color: var(--text-secondary);
  margin-right: 5px;
}

.description {
  margin: 20px 0;
  padding: 15px;
  background-color: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-color);
}

.description h3 {
  margin-bottom: 10px;
  color: var(--text-color);
}

.actions {
  margin-top: 30px;
  display: flex;
  align-items: center;
}
</style>
