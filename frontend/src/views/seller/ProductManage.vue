<template>
  <div class="seller-detail">
    <!-- 卖家信息卡片 & 操作栏 -->
    <div class="seller-info-card">
      <div class="seller-profile">
        <Avatar :src="userInfo.avatar" :name="userInfo.nickname" :size="80" />
        <div class="seller-meta">
          <h2 class="seller-name">{{ userInfo.nickname || '商家' }}</h2>
          <div class="seller-stats">
            <span class="stat-item">
              商品总数：<span class="stat-value">{{ productList.length }}</span>
            </span>
          </div>
        </div>
      </div>
      <div class="seller-actions">
        <el-button type="primary" icon="el-icon-plus" @click="handleOpenCreateDialog">发布商品</el-button>
      </div>
    </div>

    <!-- 商品列表区域 -->
    <div class="products-section">
      <h3 class="section-title">我的商品管理</h3>
      
      <div v-loading="isTableLoading" class="product-grid" v-if="productList.length > 0">
        <el-card
          v-for="p in productList"
          :key="p.id"
          class="product-card"
          @click.native="handleEditProduct(p)"
          :body-style="{ padding: '0px' }"
          shadow="hover"
        >
          <div class="image-wrapper">
            <img :src="getImage(p)" class="product-image" />
            <div class="status-badge" v-if="p.status !== 'ON_SALE'">
              <el-tag size="mini" :type="getStatusType(p.status)" effect="dark">
                {{ getStatusText(p.status) }}
              </el-tag>
            </div>
          </div>
          <div class="product-info">
            <div class="product-title" :title="p.title">{{ p.title }}</div>
            <div class="product-price">
              {{ formatPrice(p.price, p.currency) }}
            </div>
            <div class="product-meta">
              <span class="product-stock">库存: {{ p.stock }}</span>
              <span class="click-tip">点击编辑</span>
            </div>
          </div>
        </el-card>
      </div>
      
      <el-empty v-else description="暂无发布的商品，快去发布一个吧！">
        <el-button type="primary" @click="handleOpenCreateDialog">立即发布</el-button>
      </el-empty>
    </div>

    <!-- 商品发布/编辑弹窗 (保留原有功能) -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="isDialogVisible"
      width="700px"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      append-to-body
    >
      <el-form
        :model="productForm"
        ref="productFormRef"
        label-width="100px"
        :rules="productFormRules"
        autocomplete="off"
      >
        <el-form-item label="商品标题" prop="title">
          <el-input
            v-model="productForm.title"
            placeholder="请输入商品标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品分类" prop="categoryId">
          <el-select
            v-model="productForm.categoryId"
            placeholder="请选择商品分类"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="category in categoryList"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            type="textarea"
            v-model="productForm.description"
            placeholder="请输入商品描述"
            rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品价格" prop="price">
          <div style="display: flex; gap: 10px;">
            <el-input-number
              v-model="productForm.price"
              :min="0.01"
              :precision="2"
              :step="0.01"
              placeholder="请输入商品价格"
              style="flex: 1;"
            />
            <el-select v-model="productForm.currency" placeholder="选择货币" style="width: 120px;">
              <el-option label="CNY" value="CNY"></el-option>
              <el-option label="HKD" value="HKD"></el-option>
              <el-option label="USD" value="USD"></el-option>
              <el-option label="JPY" value="JPY"></el-option>
              <el-option label="EUR" value="EUR"></el-option>
              <el-option label="GBP" value="GBP"></el-option>
              <el-option label="KRW" value="KRW"></el-option>
              <el-option label="CAD" value="CAD"></el-option>
              <el-option label="AUD" value="AUD"></el-option>
            </el-select>
          </div>
        </el-form-item>

        <el-form-item label="商品库存" prop="stock">
          <el-input-number
            v-model="productForm.stock"
            :min="0"
            :step="1"
            placeholder="请输入商品库存"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="发货地址" prop="shippingAddress">
          <el-input
            v-model="productForm.shippingAddress"
            placeholder="请输入发货地址"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品图片" prop="images">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            list-type="picture-card"
            :file-list="imageList"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :before-upload="beforeUpload"
            :limit="9"
            multiple
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <div style="font-size: 12px; color: var(--text-secondary); margin-top: 5px;">
            第一张图片将作为商品封面，支持 JPG/PNG 格式，单张不超过 2MB
          </div>
        </el-form-item>
      </el-form>

      <template v-slot:footer>
        <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
           <!-- 左侧操作按钮 -->
           <div>
             <el-button 
               v-if="productForm.id && productForm.status === 'ON_SALE'" 
               type="warning" 
               plain
               size="small"
               @click="handleOffShelf(productForm.id)"
             >
               下架商品
             </el-button>
             <el-button 
               v-if="productForm.id && (productForm.status === 'OUT_OF_STOCK' || productForm.status === 'OFF_SALE')" 
               type="success" 
               plain
               size="small"
               @click="handleRestoreOnSale(productForm.id)"
             >
               上架商品
             </el-button>
             <el-button 
               v-if="productForm.id" 
               type="danger" 
               plain
               size="small"
               @click="handleDeleteProduct(productForm.id)"
             >
               删除商品
             </el-button>
           </div>
           
           <!-- 右侧保存按钮 -->
           <div>
             <el-button @click="isDialogVisible = false">取消</el-button>
             <el-button
               type="primary"
               @click="handleSaveProduct"
               :loading="isSaving"
             >
               保存修改
             </el-button>
           </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { productApi, categoryApi } from '@/api'
import currencyMixin from '@/mixins/currencyMixin'
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'ProductManage',
  components: { Avatar },
  mixins: [currencyMixin],
  data() {
    return {
      productList: [],
      categoryList: [],
      isTableLoading: false,
      isDialogVisible: false,
      isSaving: false,
      imageList: [],
      productForm: {
        id: null,
        title: '',
        categoryId: null,
        description: '',
        price: 0.01,
        currency: 'CNY',
        stock: 0,
        shippingAddress: '',
        image: '',
        images: [],
        status: 'ON_SALE'
      },
      productFormRules: {
        title: [
          { required: true, message: '请输入商品标题', trigger: 'blur' },
          { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
        ],
        categoryId: [
          { required: true, message: '请选择商品分类', trigger: 'change' }
        ],
        price: [
          { required: true, message: '请输入商品价格', trigger: 'blur' },
          { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
        ],
        stock: [
          { required: true, message: '请输入商品库存', trigger: 'blur' },
          { type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' }
        ],
        shippingAddress: [
          { required: true, message: '请输入发货地址', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    userInfo() {
      return this.$store.state.user || {}
    },
    dialogTitle() {
      return this.productForm.id ? '编辑商品' : '发布商品'
    },
    uploadHeaders() {
      return {
        Authorization: 'Bearer ' + this.$store.state.token
      }
    },
    uploadAction() {
      return '/api/upload/product'
    }
  },
  created() {
    this.initPageData()
  },
  methods: {
    async initPageData() {
      try {
        await Promise.all([
          this.loadCategoryList(),
          this.loadProductList()
        ])
      } catch (error) {
        console.warn('页面数据初始化部分失败:', error)
      }
    },

    async loadCategoryList() {
      try {
        const response = await categoryApi.getAllCategories()
        this.categoryList = Array.isArray(response.data) 
          ? response.data 
          : (response.data?.data || [])
      } catch (error) {
        console.error('加载分类失败:', error)
      }
    },

    async loadProductList() {
      this.isTableLoading = true
      try {
        const response = await productApi.getMyProducts()
        this.productList = Array.isArray(response.data)
          ? response.data
          : (response.data && response.data.data ? response.data.data : []) || []
      } catch (error) {
        console.warn('商品列表加载异常:', error)
        this.productList = []
        const msg = (error.response && error.response.data && error.response.data.message) || error.message || '加载商品失败'
        this.$message.error(msg)
      } finally {
        this.isTableLoading = false
      }
    },

    getImage(p) {
      if (p.images) {
        try {
          // 如果已经是数组
          if (Array.isArray(p.images)) return p.images[0] || ''
          
          // 如果是JSON字符串
          if (typeof p.images === 'string' && (p.images.startsWith('[') || p.images.startsWith('{'))) {
             const parsed = JSON.parse(p.images)
             if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
          }
          
          // 如果是逗号分隔字符串
          if (typeof p.images === 'string' && p.images.includes(",")) return p.images.split(",")[0]
          
          return p.images
        } catch (e) {
          console.error('Image parse error', e)
        }
      }
      return p.image || '/placeholder.png'
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

    handleOpenCreateDialog() {
      this.resetProductForm()
      this.isDialogVisible = true
    },

    handleEditProduct(product) {
      this.productForm = { ...product }
      this.productForm.price = Number(this.productForm.price) || 0.01
      this.productForm.stock = Number(this.productForm.stock) || 0
      if (!this.productForm.currency) {
        this.productForm.currency = 'CNY'
      }
      
      // 处理图片回显
      if (typeof this.productForm.images === 'string') {
        try {
          if (this.productForm.images.startsWith('[')) {
            this.productForm.images = JSON.parse(this.productForm.images)
          } else {
            this.productForm.images = this.productForm.images.split(',')
          }
        } catch (e) {
          this.productForm.images = []
        }
      } 
      if (!Array.isArray(this.productForm.images)) {
        this.productForm.images = []
      }
      if (this.productForm.images.length === 0 && this.productForm.image) {
        this.productForm.images.push(this.productForm.image)
      }
      
      this.imageList = this.productForm.images.map((url, index) => ({
        name: `image-${index}`,
        url: url
      }))

      this.isDialogVisible = true
    },

    handleUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        this.imageList = fileList
        this.productForm.images = fileList.map(f => {
          if (f.response) return f.response.data.url
          return f.url
        })
        if (this.productForm.images.length > 0) {
          this.productForm.image = this.productForm.images[0]
        }
      } else {
        this.$message.error(response.message || '图片上传失败')
        const index = fileList.indexOf(file)
        if (index !== -1) fileList.splice(index, 1)
      }
    },

    handleRemove(file, fileList) {
      this.imageList = fileList
      this.productForm.images = fileList.map(f => f.url || (f.response && f.response.data.url))
      if (this.productForm.images.length > 0) {
        this.productForm.image = this.productForm.images[0]
      } else {
        this.productForm.image = ''
      }
    },

    beforeUpload(file) {
      const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isJPGOrPNG) this.$message.error('上传图片只能是 JPG/PNG 格式!')
      if (!isLt2M) this.$message.error('上传图片大小不能超过 2MB!')
      return isJPGOrPNG && isLt2M
    },

    async handleSaveProduct() {
      const formRef = this.$refs.productFormRef
      try {
        await formRef.validate()
        this.isSaving = true
        const submitData = { ...this.productForm }
        if (submitData.images && Array.isArray(submitData.images)) {
          const validImages = submitData.images.filter(img => img)
          submitData.images = JSON.stringify(validImages)
          if (!submitData.image && validImages.length > 0) {
            submitData.image = validImages[0]
          }
        }

        if (submitData.id) {
          await productApi.updateProduct(submitData)
          this.$message.success('商品更新成功')
        } else {
          await productApi.addProduct(submitData)
          this.$message.success('商品发布成功')
        }
        this.isDialogVisible = false
        this.loadProductList()
      } catch (error) {
        if (error !== false) {
          this.$message.error('操作失败，请重试')
          console.error('保存商品失败:', error)
        }
      } finally {
        this.isSaving = false
      }
    },

    async handleOffShelf(id) {
      try {
        await productApi.offShelfProduct(id)
        this.$message.success('下架成功')
        this.isDialogVisible = false // 关闭弹窗
        this.loadProductList()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },

    async handleRestoreOnSale(id) {
      try {
        await productApi.restoreOnSale(id)
        this.$message.success('上架成功')
        this.isDialogVisible = false // 关闭弹窗
        this.loadProductList()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },

    async handleDeleteProduct(id) {
      try {
        await this.$confirm(
          '此操作将永久删除该商品, 是否继续?',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await productApi.deleteProduct(id)
        this.$message.success('商品删除成功')
        this.isDialogVisible = false // 关闭弹窗
        this.loadProductList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除商品失败:', error)
        }
      }
    },

    resetProductForm() {
      this.productForm = {
        id: null,
        title: '',
        categoryId: null,
        description: '',
        price: 0.01,
        currency: 'CNY',
        stock: 0,
        shippingAddress: '',
        image: '',
        images: [],
        status: 'ON_SALE'
      }
      this.imageList = []
      if (this.$refs.productFormRef) {
        this.$refs.productFormRef.clearValidate()
      }
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
  justify-content: space-between;
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

.section-title {
  font-size: 20px;
  margin-bottom: 20px;
  color: var(--text-color);
  border-left: 4px solid var(--primary-color);
  padding-left: 10px;
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
  position: relative;
  
  &:hover {
    transform: translateY(-5px);
    
    .click-tip {
        opacity: 1;
    }
  }

  .image-wrapper {
    height: 200px;
    overflow: hidden;
    position: relative;
    
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .status-badge {
        position: absolute;
        top: 8px;
        right: 8px;
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

    .product-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .product-stock {
          font-size: 12px;
          color: var(--text-secondary);
        }
        
        .click-tip {
            font-size: 12px;
            color: var(--primary-color);
            opacity: 0;
            transition: opacity 0.3s;
        }
    }
  }
}

// 弹窗样式调整
:deep(.el-dialog) {
  background-color: var(--card-bg-color);
  
  .el-dialog__header {
    border-bottom: 1px solid var(--border-color);
    padding-bottom: 12px;
    
    .el-dialog__title {
      color: var(--text-color);
      font-size: 16px;
      font-weight: 600;
    }
  }

  .el-dialog__body {
    padding: 20px;
    color: var(--text-color);
  }
  
  .el-form-item__label {
    color: var(--text-color);
  }
  
  .el-input__inner, .el-textarea__inner {
    background-color: var(--input-bg-color);
    border-color: var(--border-color);
    color: var(--text-color);
  }
}
</style>