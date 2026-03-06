<template>
  <div class="home">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品..."
        @keyup.enter.native="handleSearch"
        class="search-input"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch" class="search-btn"></el-button>
      </el-input>
    </div>

    <div class="category-nav">
      <el-tag
        v-for="category in categories"
        :key="category.id"
        :type="selectedCategoryId === category.id ? 'primary' : ''"
        @click="selectCategory(category.id)"
        style="margin-right: 10px; cursor: pointer;"
      >
        {{ category.name }}
      </el-tag>
      <el-tag
        :type="selectedCategoryId === null ? 'primary' : ''"
        @click="selectCategory(null)"
        style="cursor: pointer;"
      >
        全部
      </el-tag>
    </div>

    <!-- 瀑布流商品列表 -->
    <div class="waterfall" v-loading="loading">
      <div
        class="waterfall-item"
        v-for="product in productList"
        :key="product.id"
      >
        <el-card class="product-card" @click.native="goToDetail(product.id)">
          <div class="product-image">
            <img
              :src="product.image || '/placeholder.png'"
              :alt="product.title"
              loading="lazy"
            />
          </div>

          <div class="product-info">
            <h3 class="product-title">{{ product.title }}</h3>
            <div class="product-price">{{ formatPrice(product.price, product.currency) }}</div>

            <div class="product-meta">
              <span>发货地：{{ product.shippingAddress }}</span>
              <span>库存：{{ product.stock }}</span>
            </div>

            <div class="product-seller">
              <img :src="product.sellerAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="seller-avatar" />
              <span>{{ product.sellerNickname }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      @current-change="handlePageChange"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="text-align: center; margin-top: 20px;"
    ></el-pagination>
  </div>
</template>

<script>
import { productApi, categoryApi } from '../api'

export default {
  name: 'Home',
  data() {
    return {
      keyword: '',
      categories: [],
      selectedCategoryId: null,
      productList: [],
      loading: false,
      currentPage: 1,
      pageSize: 12,
      total: 0
    }
  },
  created() {
    this.loadCategories()
    this.loadProducts()
  },
  methods: {
    async loadCategories() {
      try {
        const res = await categoryApi.getAllCategories()
        this.categories = res.data
      } catch (error) {
        this.$message.error('加载分类失败')
      }
    },
    async loadProducts() {
      this.loading = true
      try {
        const res = await productApi.getProductList({
          page: this.currentPage,
          size: this.pageSize,
          categoryId: this.selectedCategoryId,
          keyword: this.keyword,
          status: 'ON_SALE'
        })
        if (res.code === 200) {
          this.productList = res.data.records
          this.total = res.data.total
        } else {
          this.$message.error(res.message || '加载商品失败')
        }
      } catch (error) {
        console.error('加载商品失败:', error)
        this.$message.error('加载商品失败')
      } finally {
        this.loading = false
      }
    },
    selectCategory(categoryId) {
      this.selectedCategoryId = categoryId
      this.currentPage = 1
      this.loadProducts()
    },
    handleSearch() {
      this.currentPage = 1
      this.loadProducts()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadProducts()
    },
    goToDetail(id) {
      this.$router.push(`/product/${id}`)
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
  text-align: center;
}

.search-input {
  width: 100%;
  max-width: 600px;
}

::v-deep .el-input-group__append {
  background-color: var(--input-bg-color) !important;
  border-color: var(--border-color) !important;
  padding: 0;
}

.search-btn {
  background-color: var(--input-bg-color) !important;
  color: var(--primary-color) !important;
  border: 1px solid var(--border-color) !important;
  border-left: none !important;
  border-radius: 0 4px 4px 0 !important;
}

.search-btn:hover {
  background-color: var(--primary-color-soft) !important;
}

.category-nav {
  margin-bottom: 20px;
  padding: 15px;
  background: var(--card-bg-color);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
}

/* ===== 瀑布流布局 ===== */
.waterfall {
  column-count: 4;
  column-gap: 20px;
}

.waterfall-item {
  break-inside: avoid;
  margin-bottom: 20px;
  width: 100%;
}

/* ===== 商品卡片 ===== */
.product-card {
  cursor: pointer;
  transition: all 0.3s;
  background-color: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  display: block;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border-color: var(--primary-color);
}

/* 图片自适应 */
.product-image {
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: auto;
  display: block;
}

/* 商品信息 */
.product-info {
  padding: 10px 0;
}

.product-title {
  font-size: 16px;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-color);
}

.product-price {
  font-size: 20px;
  color: var(--danger-color);
  font-weight: bold;
  margin-bottom: 10px;
}

.product-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
}

.product-seller {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.seller-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 5px;
}

/* ===== 响应式列数 ===== */
@media (max-width: 1200px) {
  .waterfall {
    column-count: 3;
  }
}

@media (max-width: 768px) {
  .waterfall {
    column-count: 2;
  }
}

@media (max-width: 480px) {
  .waterfall {
    column-count: 1;
  }
}
</style>