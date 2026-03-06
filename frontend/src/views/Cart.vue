<template>
  <div class="cart">
    <h2>购物车</h2>
    <el-table :data="cartList" v-loading="loading" style="width: 100%">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column label="商品" width="300">
        <template slot-scope="scope">
          <div style="display: flex; align-items: center;">
            <img :src="scope.row.productImage || '/placeholder.png'" style="width: 80px; height: 80px; margin-right: 10px;" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120">
        <template slot-scope="scope">
          {{ formatPrice(scope.row.productPrice) }}
        </template>
      </el-table-column>
      <el-table-column label="数量" width="150">
        <template slot-scope="scope">
          <el-input-number v-model="scope.row.quantity" :min="1" @change="updateQuantity(scope.row)"></el-input-number>
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template slot-scope="scope">
          {{ formatPrice(scope.row.productPrice * scope.row.quantity) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template slot-scope="scope">
          <el-button type="danger" size="small" @click="deleteItem(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="cart-footer">
      <div class="total">
        总计：{{ formatPrice(totalPrice) }}
      </div>
      <el-button type="primary" @click="checkout" :disabled="cartList.length === 0" :loading="checkoutLoading">去结算</el-button>
    </div>
    
    <!-- 选择收货地址对话框 -->
    <el-dialog title="选择收货地址" :visible.sync="addressDialogVisible" width="600px">
      <div v-if="addressList.length === 0" style="text-align: center; padding: 20px;">
        <p>暂无收货地址</p>
        <el-button type="primary" @click="goToAddAddress">去添加地址</el-button>
      </div>
      <el-radio-group v-else v-model="selectedAddressId" style="width: 100%;">
        <div v-for="addr in addressList" :key="addr.id" class="address-item">
          <el-radio :label="addr.id">
            <span class="receiver">{{ addr.receiverName }}</span>
            <span class="phone">{{ addr.receiverPhone }}</span>
            <span class="address">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</span>
            <el-tag v-if="addr.isDefault === 1" type="success" size="mini" style="margin-left: 10px;">默认</el-tag>
          </el-radio>
        </div>
      </el-radio-group>
      <div slot="footer">
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCheckout" :disabled="!selectedAddressId" :loading="checkoutLoading">确认下单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { cartApi, productApi, addressApi, orderApi } from '../api'

export default {
  name: 'Cart',
  data() {
    return {
      cartList: [],
      loading: false,
      checkoutLoading: false,
      addressDialogVisible: false,
      addressList: [],
      selectedAddressId: null
    }
  },
  computed: {
    totalPrice() {
      return this.cartList.reduce((sum, item) => {
        return sum + (item.productPrice * item.quantity)
      }, 0)
    }
  },
  created() {
    this.loadCart()
  },
  methods: {
    async loadCart() {
      this.loading = true
      try {
        const res = await cartApi.getCartList()
        // 需要获取商品详情
        const cartItems = res.data
        if (cartItems.length === 0) {
          this.cartList = []
          return
        }
        const productPromises = cartItems.map(item => productApi.getProductById(item.productId))
        const products = await Promise.all(productPromises)
        
        this.cartList = cartItems.map((item, index) => {
          const product = products[index].data
          return {
            ...item,
            productTitle: product.title,
            productImage: product.image,
            productPrice: product.price
          }
        })
      } catch (error) {
        this.$message.error('加载购物车失败')
      } finally {
        this.loading = false
      }
    },
    async updateQuantity(item) {
      try {
        await cartApi.updateCartQuantity({
          cartId: item.id,
          quantity: item.quantity
        })
      } catch (error) {
        this.$message.error('更新失败')
        this.loadCart()
      }
    },
    async deleteItem(id) {
      try {
        await cartApi.deleteCartItem(id)
        this.$message.success('删除成功')
        this.loadCart()
      } catch (error) {
        this.$message.error('删除失败')
      }
    },
    async checkout() {
      // 先加载收货地址
      try {
        const res = await addressApi.getAddressList()
        this.addressList = res.data || []
        
        // 如果有默认地址，自动选中
        const defaultAddr = this.addressList.find(addr => addr.isDefault === 1)
        if (defaultAddr) {
          this.selectedAddressId = defaultAddr.id
        } else if (this.addressList.length > 0) {
          this.selectedAddressId = this.addressList[0].id
        }
        
        this.addressDialogVisible = true
      } catch (error) {
        this.$message.error('加载收货地址失败')
      }
    },
    goToAddAddress() {
      this.addressDialogVisible = false
      this.$router.push('/address')
    },
    async confirmCheckout() {
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }
      
      this.checkoutLoading = true
      try {
        // 为购物车中每个商品创建订单
        const orderPromises = this.cartList.map(item => {
          return orderApi.createOrder({
            productId: item.productId,
            addressId: this.selectedAddressId,
            quantity: item.quantity
          })
        })
        
        const results = await Promise.all(orderPromises)
        
        // 检查是否有失败的订单
        const failedOrders = results.filter(res => res.code !== 200)
        if (failedOrders.length > 0) {
          this.$message.warning(`部分订单创建失败：${failedOrders.map(r => r.message).join(', ')}`)
        }
        
        // 清空购物车
        await cartApi.clearCart()
        
        this.$message.success('订单创建成功！')
        this.addressDialogVisible = false
        
        // 跳转到订单页面
        this.$router.push('/orders')
      } catch (error) {
        console.error('结算失败:', error)
        this.$message.error(error.message || '结算失败，请重试')
      } finally {
        this.checkoutLoading = false
      }
    }
  }
}
</script>

<style scoped>
.cart {
  padding: 20px;
}

.cart-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
}

.total {
  font-size: 20px;
  font-weight: bold;
  color: var(--danger-color);
}

.address-item {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
}

.address-item:last-child {
  border-bottom: none;
}

.address-item .receiver {
  font-weight: bold;
  margin-right: 15px;
  color: var(--text-color);
}

.address-item .phone {
  color: var(--text-secondary);
  margin-right: 15px;
}

.address-item .address {
  color: var(--text-color);
}
</style>
