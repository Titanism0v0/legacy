<template>
  <div class="product-manage">
    <h2>商品管理</h2>
    <el-button type="primary" @click="showDialog = true" style="margin-bottom: 20px;">发布商品</el-button>
    
    <el-table :data="productList" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="商品标题" min-width="150" show-overflow-tooltip></el-table-column>
      <el-table-column label="图片" width="80">
        <template slot-scope="scope">
          <img :src="scope.row.image || '/placeholder.png'" style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px;" />
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120">
        <template slot-scope="scope">
          {{ scope.row.currency || 'CNY' }} {{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80"></el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewProductDetail(scope.row)">详情</el-button>
          <el-button type="text" size="small" @click="editProduct(scope.row)">编辑</el-button>
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
            v-if="scope.row.status === 'OFF_SALE'" 
            type="text" 
            size="small" 
            style="color: var(--success-color);"
            @click="restoreOnSale(scope.row.id)"
          >
            重新上架
          </el-button>
          <el-button 
            v-if="scope.row.status === 'OFF_SALE'" 
            type="text" 
            size="small" 
            style="color: var(--danger-color);"
            @click="deleteProduct(scope.row.id)"
          >
            删除
          </el-button>
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
            <span>浏览量: {{ currentProduct.viewCount || 0 }}</span>
            <span>发布时间: {{ currentProduct.createTime }}</span>
          </div>
          <div class="description-box">
            <h4>商品描述</h4>
            <p>{{ currentProduct.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog :title="dialogTitle" :visible.sync="showDialog" width="800px">
      <el-form :model="productForm" :rules="rules" ref="productForm" label-width="100px">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="productForm.title"></el-input>
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input type="textarea" v-model="productForm.description" :rows="4"></el-input>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width: 140px; margin-right: 10px;"></el-input-number>
          <el-select v-model="productForm.currency" placeholder="货币" style="width: 100px;">
            <el-option label="CNY" value="CNY"></el-option>
            <el-option label="USD" value="USD"></el-option>
            <el-option label="JPY" value="JPY"></el-option>
            <el-option label="EUR" value="EUR"></el-option>
            <el-option label="GBP" value="GBP"></el-option>
            <el-option label="KRW" value="KRW"></el-option>
            <el-option label="CAD" value="CAD"></el-option>
            <el-option label="AUD" value="AUD"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="发货地址" prop="shippingAddress">
          <el-input v-model="productForm.shippingAddress"></el-input>
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <div style="display: flex; gap: 10px; align-items: flex-start;">
            <div class="image-uploader">
              <el-upload
                class="avatar-uploader"
                :action="uploadAction"
                :show-file-list="false"
                :headers="uploadHeaders"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload">
                <img v-if="productForm.image" :src="productForm.image" class="avatar">
                <i v-else class="el-icon-plus avatar-uploader-icon"></i>
              </el-upload>
              <div class="upload-tip">点击上传本地图片</div>
            </div>
            <div style="flex: 1;">
              <el-input v-model="productForm.image" placeholder="或输入图片URL" clearable>
                <template slot="prepend">URL</template>
              </el-input>
              <div style="margin-top: 10px; color: #909399; font-size: 12px;">
                支持 JPG/PNG 格式，大小不超过 2MB
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { productApi, categoryApi } from '../../api'

export default {
  name: 'ProductManage',
  data() {
    return {
      productList: [],
      categories: [],
      loading: false,
      showDialog: false,
      detailDialogVisible: false,
      currentProduct: null,
      productForm: {
        id: null,
        title: '',
        categoryId: null,
        description: '',
        price: 0,
        currency: 'CNY',
        stock: 0,
        shippingAddress: '',
        image: ''
      },
      rules: {
        title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
        description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
        price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
        stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
        shippingAddress: [{ required: true, message: '请输入发货地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.productForm.id ? '编辑商品' : '发布商品'
    },
    uploadHeaders() {
      return {
        Authorization: 'Bearer ' + this.$store.state.token
      }
    },
    uploadAction() {
      // 代理转发到后端 /upload/product
      return '/api/upload/product'
    }
  },
  created() {
    this.loadCategories()
    this.loadProducts()
  },
  methods: {
    async loadCategories() {
      try {
        console.log('开始加载商品分类...')
        const res = await categoryApi.getAllCategories()
        console.log('分类接口返回:', res)
        console.log('分类数据:', res.data)
        this.categories = res.data || []
        if (this.categories.length === 0) {
          console.warn('分类列表为空')
          this.$message.warning('暂无商品分类，请联系管理员添加分类')
        } else {
          console.log('成功加载分类数量:', this.categories.length)
        }
      } catch (error) {
        console.error('加载分类失败:', error)
        console.error('错误详情:', error.response || error.message)
        this.$message.error('加载分类失败: ' + (error.message || '未知错误'))
      }
    },
    async loadProducts() {
      this.loading = true
      try {
        const res = await productApi.getMyProducts()
        this.productList = res.data
      } catch (error) {
        this.$message.error('加载商品失败')
      } finally {
        this.loading = false
      }
    },
    editProduct(product) {
      this.productForm = { ...product }
      // 如果没有货币单位，默认设置为 CNY
      if (!this.productForm.currency) {
        this.$set(this.productForm, 'currency', 'CNY')
      }
      this.showDialog = true
    },
    async saveProduct() {
      // 再次确保货币单位有值
      if (!this.productForm.currency) {
        this.productForm.currency = 'CNY'
      }
      this.$refs.productForm.validate(async (valid) => {
        if (valid) {
          try {
            if (this.productForm.id) {
              await productApi.updateProduct(this.productForm)
              this.$message.success('更新成功')
            } else {
              await productApi.addProduct(this.productForm)
              this.$message.success('发布成功')
            }
            this.showDialog = false
            this.resetForm()
            this.loadProducts()
          } catch (error) {
            this.$message.error(error.message || '操作失败')
          }
        }
      })
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
    async deleteProduct(id) {
      try {
        await this.$confirm('此操作将永久删除该商品, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await productApi.deleteProduct(id)
        this.$message.success('删除成功')
        this.loadProducts()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '操作失败')
        }
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
    resetForm() {
      // 获取用户所在国家作为默认发货地
      const userCountry = this.$store.state.user ? this.$store.state.user.country : ''
      // 简单的映射表，将货币代码转换为国家名称（仅作为默认值，用户可修改）
      const countryMap = {
        'CNY': '中国',
        'USD': '美国',
        'JPY': '日本',
        'EUR': '欧洲',
        'GBP': '英国',
        'KRW': '韩国',
        'CAD': '加拿大',
        'AUD': '澳大利亚'
      }
      
      this.productForm = {
        id: null,
        title: '',
        categoryId: null,
        description: '',
        price: 0,
        currency: 'CNY',
        stock: 0,
        shippingAddress: countryMap[userCountry] || '',
        image: ''
      }
      if (this.$refs.productForm) {
        this.$refs.productForm.resetFields()
      }
    },
    viewProductDetail(product) {
      this.currentProduct = product
      this.detailDialogVisible = true
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
    handleUploadSuccess(res, file) {
      if (res.code === 200) {
        this.productForm.image = res.data.url
        this.$message.success('上传成功')
      } else {
        this.$message.error(res.message || '上传失败')
      }
    },
    beforeUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('上传图片只能是 JPG/PNG 格式!')
      }
      if (!isLt2M) {
        this.$message.error('上传图片大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    }
  },
  watch: {
    showDialog(val) {
      if (!val) {
        this.resetForm()
      }
    }
  }
}
</script>

<style scoped>
.product-manage {
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

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}
.image-uploader {
  text-align: center;
  width: 100px;
}
.upload-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
}
</style>
