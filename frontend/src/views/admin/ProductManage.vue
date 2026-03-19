<template>
  <div class="admin-product-manage">
    <h2>商品管理</h2>
    <div style="margin-bottom: 20px;">
      <el-button type="danger" @click="batchDelete" :disabled="selectedProducts.length === 0">批量删除</el-button>
    </div>
    <el-table 
      :data="productList" 
      v-loading="loading" 
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="title" label="商品标题" min-width="150" show-overflow-tooltip></el-table-column>
      <el-table-column label="图片" width="80">
        <template slot-scope="scope">
          <img :src="scope.row.image || '/placeholder.png'" style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px;" />
        </template>
      </el-table-column>
      <el-table-column prop="sellerNickname" label="卖家" width="100" show-overflow-tooltip></el-table-column>
      <el-table-column prop="price" label="价格" width="100">
        <template slot-scope="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80"></el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核" width="100">
        <template slot-scope="scope">
          <el-tag size="small" :type="getAuditType(scope.row.auditStatus)">
            {{ getAuditText(scope.row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewProductDetail(scope.row)">详情</el-button>
          <el-button type="text" size="small" @click="editProduct(scope.row)">编辑</el-button>
          <el-button
            type="text"
            size="small"
            style="color: var(--success-color);"
            @click="auditProduct(scope.row.id, 'APPROVE')"
          >通过</el-button>
          <el-button
            type="text"
            size="small"
            style="color: var(--danger-color);"
            @click="auditProduct(scope.row.id, 'TAKE_DOWN')"
          >违规下架</el-button>
          <el-button 
            v-if="scope.row.status === 'ON_SALE'" 
            type="text" 
            size="small" 
            style="color: var(--warning-color);"
            @click="offShelf(scope.row.id)"
          >
            下架
          </el-button>
          <el-button 
            v-if="scope.row.status === 'ON_SALE'" 
            type="text" 
            size="small" 
            style="color: var(--text-secondary);"
            @click="markOutOfStock(scope.row.id)"
          >
            缺货
          </el-button>
          <el-button 
            v-if="scope.row.status === 'OUT_OF_STOCK'" 
            type="text" 
            size="small" 
            style="color: var(--success-color);"
            @click="restoreOnSale(scope.row.id)"
          >
            上架
          </el-button>
          <el-button type="text" size="small" style="color: var(--danger-color);" @click="deleteProduct(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 商品详情弹窗 -->
    <el-dialog title="商品详情" :visible.sync="detailDialogVisible" width="500px">
      <div v-if="currentProduct" class="product-detail-card">
        <div class="detail-image">
          <img :src="currentProduct.image || '/placeholder.png'" />
        </div>
        <div class="detail-info">
          <h3>{{ currentProduct.title }}</h3>
          <p class="price">¥{{ currentProduct.price }}</p>
          <div class="meta-row">
            <span>库存: {{ currentProduct.stock }}</span>
            <span>状态: {{ getStatusText(currentProduct.status) }}</span>
          </div>
          <div class="meta-row">
            <span>分类: {{ currentProduct.categoryName || '未分类' }}</span>
            <span>发货地: {{ currentProduct.shippingAddress }}</span>
          </div>
          <div class="meta-row">
            <span>卖家: {{ currentProduct.sellerNickname }}</span>
            <span>发布时间: {{ currentProduct.createTime }}</span>
          </div>
          <div class="description-box">
            <h4>商品描述</h4>
            <p>{{ currentProduct.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
    
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
import { productApi } from '../../api'

export default {
  name: 'AdminProductManage',
  data() {
    return {
      productList: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      detailDialogVisible: false,
      currentProduct: null,
      selectedProducts: []
    }
  },
  created() {
    this.loadProducts()
  },
  methods: {
    viewProductDetail(product) {
      this.currentProduct = product
      this.detailDialogVisible = true
    },
    async loadProducts() {
      this.loading = true
      try {
        const res = await productApi.getProductList({
          page: this.currentPage,
          size: this.pageSize
        })
        this.productList = res.data.records
        this.total = res.data.total
      } catch (error) {
        this.$message.error('加载商品失败')
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadProducts()
    },
    handleSelectionChange(val) {
      this.selectedProducts = val
    },
    async batchDelete() {
      try {
        await this.$confirm(`确定要删除选中的 ${this.selectedProducts.length} 个商品吗？`, '提示', {
          type: 'warning'
        })
        
        const ids = this.selectedProducts.map(item => item.id)
        await productApi.batchDeleteProducts(ids)
        this.$message.success('批量删除成功')
        this.loadProducts()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    editProduct(product) {
      this.$message.info('编辑功能开发中')
    },
    async offShelf(id) {
      try {
        await productApi.offShelfProduct(id)
        this.$message.success('下架成功')
        this.loadProducts()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async markOutOfStock(id) {
      try {
        await productApi.markOutOfStock(id)
        this.$message.success('标记成功')
        this.loadProducts()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async restoreOnSale(id) {
      try {
        console.log('恢复上架，商品ID:', id)
        console.log('调用接口: /product/restore-on-sale/' + id)
        const res = await productApi.restoreOnSale(id)
        console.log('恢复上架响应:', res)
        this.$message.success('恢复上架成功')
        this.loadProducts()
      } catch (error) {
        console.error('恢复上架失败:', error)
        console.error('错误详情:', error.response)
        this.$message.error(error.message || error.response?.data?.message || '操作失败')
      }
    },
    async deleteProduct(id) {
      try {
        await this.$confirm('确定要删除该商品吗？', '提示', {
          type: 'warning'
        })
        await productApi.deleteProduct(id)
        this.$message.success('删除成功')
        this.loadProducts()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    getStatusText(status) {
      const statusMap = {
        'ON_SALE': '在售',
        'OFF_SALE': '已下架',
        'OUT_OF_STOCK': '缺货'
      }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = {
        'ON_SALE': 'success',
        'OFF_SALE': 'info',
        'OUT_OF_STOCK': 'warning'
      }
      return typeMap[status] || ''
    },

    getAuditText(status) {
      const map = {
        'PENDING': '待审',
        'APPROVED': '通过',
        'REJECTED': '拒绝'
      }
      return map[status] || (status || '-')
    },

    getAuditType(status) {
      const map = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger'
      }
      return map[status] || 'info'
    },

    async auditProduct(productId, action) {
      let remark = ''
      let restrictedFlag = 0
      let riskLevel = 'LOW'

      try {
        if (action === 'TAKE_DOWN') {
          const { value } = await this.$prompt('请输入违规下架原因（建议注明禁限售/侵权/疑似假货）', '违规下架', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '原因',
            inputValidator: (v) => (v && v.trim() !== '') ? true : '请填写原因'
          })
          remark = value
          restrictedFlag = 1
          riskLevel = 'HIGH'
        } else {
          const { value } = await this.$prompt('请输入审核备注（可选）', '审核通过', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '备注（可选）'
          })
          remark = value
        }
      } catch (e) {
        return
      }

      try {
        await productApi.auditProduct({ productId, action, remark, restrictedFlag, riskLevel })
        this.$message.success('操作成功')
        this.loadProducts()
      } catch (error) {
        this.$message.error(error.message || error.response?.data?.message || '操作失败')
      }
    }
  }
}
</script>

<style scoped>
.admin-product-manage {
  padding: 20px;
}

.product-detail-card {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.detail-image img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.detail-info h3 {
  margin: 0 0 10px;
  color: var(--text-color);
}

.detail-info .price {
  font-size: 20px;
  color: var(--danger-color);
  font-weight: bold;
  margin-bottom: 10px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.description-box {
  margin-top: 15px;
  padding: 10px;
  background-color: var(--bg-color);
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.description-box h4 {
  margin: 0 0 5px;
  font-size: 14px;
  color: var(--text-color);
}

.description-box p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}
</style>
