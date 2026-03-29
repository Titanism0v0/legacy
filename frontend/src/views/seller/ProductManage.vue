<template>
  <div class="seller-product-manage">
    <h2>我的商品</h2>

    <el-alert
      v-if="!kycApproved"
      type="warning"
      :closable="false"
      title="你当前未通过商家资质审核，暂时不能上架商品。请到个人中心提交KYC材料。"
      class="kyc-alert"
    />

    <div class="toolbar">
      <el-button type="primary" @click="openCreateDialog" :disabled="!kycApproved">发布商品</el-button>
    </div>

    <el-table :data="productList" v-loading="isTableLoading" style="width: 100%">
      <el-table-column prop="title" label="商品标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="主图" width="80">
        <template slot-scope="scope">
          <img :src="getImage(scope.row)" class="product-thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="categoryId" label="分类ID" width="80" />
      <el-table-column label="价格" width="130">
        <template slot-scope="scope">{{ formatPrice(scope.row.price, scope.row.currency) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="riskLevel" label="风控" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.riskLevel === 'HIGH' ? 'danger' : (scope.row.riskLevel === 'MEDIUM' ? 'warning' : 'success')">
            {{ scope.row.riskLevel || 'LOW' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核状态" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.auditStatus === 'APPROVED' ? 'success' : (scope.row.auditStatus === 'REJECTED' ? 'danger' : 'warning')">
            {{ scope.row.auditStatus || 'PENDING' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="上架状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleEditProduct(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="handleOffShelf(scope.row.id)" v-if="scope.row.status === 'ON_SALE'">下架</el-button>
          <el-button type="text" size="small" @click="handleRestoreOnSale(scope.row.id)" v-else>上架</el-button>
          <el-button type="text" size="small" style="color: var(--danger-color);" @click="handleDeleteProduct(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="isDialogVisible"
      width="720px"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      append-to-body
    >
      <el-form :model="productForm" ref="productFormRef" label-width="100px" :rules="productFormRules">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="productForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" filterable clearable style="width: 100%">
            <el-option v-for="category in categoryList" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input type="textarea" v-model="productForm.description" rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <div class="price-row">
            <el-input-number v-model="productForm.price" :min="0.01" :precision="2" :step="0.01" style="flex:1" />
            <el-select v-model="productForm.currency" style="width: 130px">
              <el-option label="CNY" value="CNY" />
              <el-option label="HKD" value="HKD" />
              <el-option label="USD" value="USD" />
              <el-option label="JPY" value="JPY" />
              <el-option label="EUR" value="EUR" />
              <el-option label="GBP" value="GBP" />
              <el-option label="KRW" value="KRW" />
              <el-option label="CAD" value="CAD" />
              <el-option label="AUD" value="AUD" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="商品库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发货地址" prop="shippingAddress">
          <el-input v-model="productForm.shippingAddress" maxlength="200" show-word-limit />
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
          <div class="upload-note">第一张作为封面；支持 JPG/PNG，单张不超过2MB。</div>
        </el-form-item>
      </el-form>
      <template v-slot:footer>
        <el-button @click="isDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProduct" :loading="isSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { productApi, categoryApi, userApi } from '@/api'
import currencyMixin from '@/mixins/currencyMixin'

export default {
  name: 'ProductManage',
  mixins: [currencyMixin],
  data() {
    return {
      productList: [],
      categoryList: [],
      isTableLoading: false,
      isDialogVisible: false,
      isSaving: false,
      imageList: [],
      sellerInfo: null,
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
        title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
        price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
        stock: [{ required: true, message: '请输入商品库存', trigger: 'blur' }],
        shippingAddress: [{ required: true, message: '请输入发货地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.productForm.id ? '编辑商品' : '发布商品'
    },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + this.$store.state.token }
    },
    uploadAction() {
      return '/api/upload/product'
    },
    kycApproved() {
      return this.sellerInfo && this.sellerInfo.kycStatus === 'APPROVED'
    }
  },
  created() {
    this.initPageData()
  },
  methods: {
    async initPageData() {
      await this.refreshSellerInfo()
      await Promise.all([this.loadCategoryList(), this.loadProductList()])
    },
    async refreshSellerInfo() {
      try {
        const res = await userApi.getUserInfo()
        this.sellerInfo = res.data
        this.$store.commit('SET_USER', res.data)
      } catch (e) {
        this.sellerInfo = this.$store.state.user
      }
    },
    async loadCategoryList() {
      try {
        const response = await categoryApi.getAllCategories()
        this.categoryList = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        this.categoryList = []
      }
    },
    async loadProductList() {
      this.isTableLoading = true
      try {
        const response = await productApi.getMyProducts()
        this.productList = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        this.$message.error(error.message || '加载商品失败')
        this.productList = []
      } finally {
        this.isTableLoading = false
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
      return p.image || '/placeholder.png'
    },
    getStatusText(status) {
      const statusMap = { ON_SALE: '在售', OFF_SALE: '已下架', OUT_OF_STOCK: '缺货' }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = { ON_SALE: 'success', OFF_SALE: 'info', OUT_OF_STOCK: 'warning' }
      return typeMap[status] || ''
    },
    openCreateDialog() {
      if (!this.kycApproved) {
        this.$message.warning('资质审核通过后才可发布商品')
        return
      }
      this.resetProductForm()
      this.isDialogVisible = true
    },
    handleEditProduct(product) {
      this.productForm = { ...product }
      this.productForm.price = Number(this.productForm.price) || 0.01
      this.productForm.stock = Number(this.productForm.stock) || 0
      if (!this.productForm.currency) this.productForm.currency = 'CNY'

      let images = []
      if (typeof this.productForm.images === 'string') {
        try {
          images = this.productForm.images.startsWith('[') ? JSON.parse(this.productForm.images) : this.productForm.images.split(',')
        } catch (e) {
          images = []
        }
      } else if (Array.isArray(this.productForm.images)) {
        images = this.productForm.images
      }
      if (images.length === 0 && this.productForm.image) images.push(this.productForm.image)
      this.productForm.images = images
      this.imageList = images.map((url, index) => ({ name: `image-${index}`, url }))
      this.isDialogVisible = true
    },
    handleUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        this.imageList = fileList
        this.productForm.images = fileList.map(f => (f.response ? f.response.data.url : f.url)).filter(Boolean)
        this.productForm.image = this.productForm.images[0] || ''
      } else {
        this.$message.error(response.message || '图片上传失败')
      }
    },
    handleRemove(file, fileList) {
      this.imageList = fileList
      this.productForm.images = fileList.map(f => f.url || (f.response && f.response.data.url)).filter(Boolean)
      this.productForm.image = this.productForm.images[0] || ''
    },
    beforeUpload(file) {
      const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isJPGOrPNG) this.$message.error('只支持 JPG/PNG 格式')
      if (!isLt2M) this.$message.error('图片大小不能超过2MB')
      return isJPGOrPNG && isLt2M
    },
    async handleSaveProduct() {
      if (!this.kycApproved) {
        this.$message.warning('资质审核通过后才可上架商品')
        return
      }
      try {
        await this.$refs.productFormRef.validate()
        this.isSaving = true
        const submitData = { ...this.productForm }
        if (Array.isArray(submitData.images)) {
          submitData.images = JSON.stringify(submitData.images.filter(Boolean))
          if (!submitData.image && submitData.images.length > 2) {
            const parsed = JSON.parse(submitData.images)
            submitData.image = parsed[0] || ''
          }
        }

        if (submitData.id) {
          await productApi.updateProduct(submitData)
          this.$message.success('商品更新成功')
        } else {
          await productApi.addProduct(submitData)
          this.$message.success('商品发布成功，等待审核')
        }
        this.isDialogVisible = false
        this.loadProductList()
      } catch (error) {
        if (error !== false) this.$message.error(error.message || '操作失败')
      } finally {
        this.isSaving = false
      }
    },
    async handleOffShelf(id) {
      try {
        await productApi.offShelfProduct(id)
        this.$message.success('下架成功')
        this.loadProductList()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async handleRestoreOnSale(id) {
      try {
        await productApi.restoreOnSale(id)
        this.$message.success('上架成功')
        this.loadProductList()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async handleDeleteProduct(id) {
      try {
        await this.$confirm('此操作将永久删除该商品，是否继续？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await productApi.deleteProduct(id)
        this.$message.success('商品删除成功')
        this.loadProductList()
      } catch (error) {
        if (error !== 'cancel') this.$message.error(error.message || '删除失败')
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
      if (this.$refs.productFormRef) this.$refs.productFormRef.clearValidate()
    }
  }
}
</script>

<style scoped>
.seller-product-manage {
  padding: 20px;
}

.kyc-alert {
  margin-bottom: 12px;
}

.toolbar {
  margin-bottom: 14px;
}

.product-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}

.price-row {
  display: flex;
  gap: 10px;
}

.upload-note {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 5px;
}
</style>

