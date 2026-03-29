<template>
  <div class="cart">
    <h2>购物车</h2>
    <el-table :data="cartList" v-loading="loading" style="width: 100%">
      <el-table-column label="商品" width="320">
        <template slot-scope="scope">
          <div class="product-cell">
            <img :src="scope.row.productImage || '/placeholder.png'" class="product-img" />
            <div>
              <div>{{ scope.row.productTitle }}</div>
              <div class="sub-price">{{ formatPrice(scope.row.productPrice, scope.row.currency) }} / 件</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="150">
        <template slot-scope="scope">
          <el-input-number
            v-model="scope.row.quantity"
            :min="1"
            :max="scope.row.stock || 1"
            @change="updateQuantity(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="费用明细" min-width="280">
        <template slot-scope="scope">
          <div class="fee-line">小计：{{ formatPrice(scope.row.estimate.subtotalPrice || 0, scope.row.currency) }}</div>
          <div class="fee-line">税费：{{ formatPrice(scope.row.estimate.taxEstimatedAmount || 0, scope.row.currency) }}</div>
          <div class="fee-line">运费：{{ formatPrice(scope.row.estimate.shippingFeeSnapshot || 0, scope.row.currency) }}</div>
          <div class="fee-line total">合计：{{ formatPrice(scope.row.estimate.totalPrice || 0, scope.row.currency) }}</div>
          <div class="tax-tag">{{ scope.row.estimate.taxIncludedFlag === 1 ? '含税' : '未税' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="danger" size="small" @click="deleteItem(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="cart-footer">
      <div class="total">
        总计：{{ formatPrice(totalPrice, displayCurrency) }}
      </div>
      <el-button type="primary" @click="checkout" :disabled="cartList.length === 0" :loading="checkoutLoading">去结算</el-button>
    </div>

    <el-dialog title="选择收货地址" :visible.sync="addressDialogVisible" width="640px">
      <div v-if="addressList.length === 0" class="empty-address">
        <p>暂无收货地址</p>
        <el-button type="primary" @click="goToAddAddress">去添加地址</el-button>
      </div>
      <el-radio-group v-else v-model="selectedAddressId" class="address-group">
        <div v-for="addr in addressList" :key="addr.id" class="address-item">
          <el-radio :label="addr.id">
            <span class="receiver">{{ addr.receiverName }}</span>
            <span class="phone">{{ addr.receiverPhone }}</span>
            <span class="address">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</span>
            <el-tag v-if="addr.isDefault === 1" type="success" size="mini">默认</el-tag>
          </el-radio>
        </div>
      </el-radio-group>

      <el-alert
        type="warning"
        :closable="false"
        title="确认下单即表示你已知悉：税费预估、跨境清关时效、售后证据要求。"
        class="settle-alert"
      />

      <div slot="footer">
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCheckout" :disabled="!selectedAddressId" :loading="checkoutLoading">确认下单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { cartApi, productApi, addressApi, orderApi } from '../api'
import { estimateByLocalRule } from '@/utils/crossborder'
import currencyMixin from '@/mixins/currencyMixin'

export default {
  name: 'Cart',
  mixins: [currencyMixin],
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
      return this.cartList.reduce((sum, item) => sum + Number(item.estimate.totalPrice || 0), 0)
    },
    displayCurrency() {
      if (this.cartList.length > 0) return this.cartList[0].currency || 'CNY'
      return 'CNY'
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
        const cartItems = res.data || []
        if (cartItems.length === 0) {
          this.cartList = []
          return
        }

        const products = await Promise.all(cartItems.map(item => productApi.getProductById(item.productId)))
        const list = []
        for (let i = 0; i < cartItems.length; i++) {
          const item = cartItems[i]
          const product = products[i].data
          let estimate = {}
          try {
            const quoteRes = await orderApi.estimate({ productId: item.productId, quantity: item.quantity })
            estimate = quoteRes.data || {}
          } catch (e) {
            estimate = estimateByLocalRule(product, item.quantity)
          }
          list.push({
            ...item,
            productTitle: product.title,
            productImage: product.image,
            productPrice: product.price,
            currency: product.currency,
            stock: product.stock,
            restrictedFlag: product.restrictedFlag,
            estimate
          })
        }
        this.cartList = list
      } catch (error) {
        this.$message.error('加载购物车失败')
      } finally {
        this.loading = false
      }
    },
    async updateQuantity(item) {
      try {
        if (item.stock != null && item.quantity > item.stock) {
          item.quantity = item.stock
        }
        await cartApi.updateCartQuantity({
          cartId: item.id,
          quantity: item.quantity
        })
        try {
          const quoteRes = await orderApi.estimate({ productId: item.productId, quantity: item.quantity })
          item.estimate = quoteRes.data || {}
        } catch (e) {
          item.estimate = estimateByLocalRule(item, item.quantity)
        }
      } catch (error) {
        this.$message.error(error.message || '更新失败')
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
      try {
        const res = await addressApi.getAddressList()
        this.addressList = res.data || []
        const defaultAddr = this.addressList.find(addr => addr.isDefault === 1)
        if (defaultAddr) this.selectedAddressId = defaultAddr.id
        else if (this.addressList.length > 0) this.selectedAddressId = this.addressList[0].id
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
        const orderPromises = this.cartList.map(item => {
          return orderApi.createOrder({
            productId: item.productId,
            addressId: this.selectedAddressId,
            quantity: item.quantity,
            taxEstimatedAmount: item.estimate.taxEstimatedAmount,
            taxDeclarationAccepted: 1,
            restrictedDeclarationAccepted: item.restrictedFlag === 1 ? 1 : 0
          })
        })
        await Promise.all(orderPromises)
        await cartApi.clearCart()
        this.$message.success('订单创建成功')
        this.addressDialogVisible = false
        this.$router.push('/orders')
      } catch (error) {
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

.product-cell {
  display: flex;
  align-items: center;
}

.product-img {
  width: 80px;
  height: 80px;
  margin-right: 10px;
  object-fit: cover;
}

.sub-price {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.fee-line {
  line-height: 1.6;
}

.fee-line.total {
  font-weight: bold;
}

.tax-tag {
  font-size: 12px;
  color: #909399;
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

.empty-address {
  text-align: center;
  padding: 20px;
}

.address-group {
  width: 100%;
}

.address-item {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
}

.receiver {
  font-weight: bold;
  margin-right: 15px;
}

.phone {
  color: var(--text-secondary);
  margin-right: 15px;
}

.settle-alert {
  margin-top: 16px;
}
</style>
