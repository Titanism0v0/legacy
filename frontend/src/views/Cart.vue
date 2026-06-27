<template>
  <div class="cart">
    <h2>购物车</h2>
    <el-table :data="cartList" v-loading="loading" style="width: 100%">
      <el-table-column label="商品" width="320">
        <template slot-scope="scope">
          <div class="product-cell">
            <img :src="scope.row.productImage || '/placeholder.svg'" class="product-img" />
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
      <el-table-column label="费用明细" min-width="340">
        <template slot-scope="scope">
          <div class="fee-line">商品小计：{{ formatEstimatePrice(scope.row.estimate.subtotalPrice, scope.row.estimate.displayCurrency) }}</div>
          <div class="fee-line">国际运费：{{ formatEstimatePrice(scope.row.estimate.internationalShippingFee, scope.row.estimate.displayCurrency) }}</div>
          <div class="fee-line">保险费：{{ formatEstimatePrice(scope.row.estimate.insuranceFee, scope.row.estimate.displayCurrency) }}</div>
          <div class="fee-line">预估税费：{{ formatEstimatePrice(scope.row.estimate.taxEstimatedAmount, scope.row.estimate.displayCurrency) }}</div>
          <div class="fee-line total">页面合计：{{ formatEstimatePrice(scope.row.estimate.totalPrice, scope.row.estimate.displayCurrency) }}</div>
          <div class="fee-line hint">计税口径：{{ getTaxModeText(scope.row.estimate.taxMode) }}</div>
          <div class="fee-line hint" v-if="scope.row.estimate.paymentFallbackApplied">
            实际支付：{{ formatLiteralPrice(scope.row.estimate.paymentTotalPrice, scope.row.estimate.paymentCurrency) }}
          </div>
          <el-button type="text" size="mini" @click="openRuleDialog(scope.row)">查看规则</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="danger" size="small" @click="deleteItem(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="cart-footer">
      <div class="total-box">
        <div class="total">页面预估总计：{{ formatLiteralPrice(totalPrice, currentCurrency) }}</div>
        <div class="pay-total" v-if="showPaymentTotal">
          实际人民币支付：{{ formatLiteralPrice(paymentTotalPrice, 'CNY') }}
        </div>
      </div>
      <el-button
        type="primary"
        @click="checkout"
        :disabled="cartList.length === 0 || hasEstimateError"
        :loading="checkoutLoading"
      >
        去结算
      </el-button>
    </div>

    <el-dialog title="选择收货地址" :visible.sync="addressDialogVisible" width="680px">
      <div v-if="addressList.length === 0" class="empty-address">
        <p>暂无收货地址</p>
        <el-button type="primary" @click="goToAddAddress">去添加地址</el-button>
      </div>
      <template v-else>
        <el-radio-group v-model="selectedAddressId" class="address-group">
          <div v-for="address in addressList" :key="address.id" class="address-item">
            <el-radio :label="address.id">
              <span class="receiver">{{ address.receiverName }}</span>
              <span class="phone">{{ address.receiverPhone }}</span>
              <span class="address">{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</span>
              <el-tag v-if="address.isDefault === 1" type="success" size="mini">默认</el-tag>
            </el-radio>
          </div>
        </el-radio-group>

        <div class="checkout-rule-links">
          <span>本次税费/运费规则：</span>
          <el-button
            v-for="item in cartList"
            :key="item.id"
            type="text"
            size="mini"
            @click="openRuleDialog(item)"
          >
            {{ item.productTitle }}
          </el-button>
        </div>

        <el-alert
          type="warning"
          :closable="false"
          title="请先查看税费和快递规则，再确认下单。税费为预估值，最终以海关审核、支付与物流信息为准。"
          class="settle-alert"
        />
        <el-alert
          v-if="showPaymentTotal"
          type="info"
          :closable="false"
          :title="`当前页面为 ${currentCurrency} 展示，实际支付将按人民币结算，预计应付 ${formatLiteralPrice(paymentTotalPrice, 'CNY')}`"
          class="settle-alert"
        />
      </template>

      <div slot="footer">
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCheckout" :disabled="!selectedAddressId" :loading="checkoutLoading">
          确认下单
        </el-button>
      </div>
    </el-dialog>

    <FeeRuleDialog
      :visible.sync="ruleDialogVisible"
      :estimate="currentRuleEstimate"
      :title="ruleDialogTitle"
    />
  </div>
</template>

<script>
import { formatPriceDisplay } from '@/utils/currency'
import { cartApi, productApi, addressApi, orderApi } from '../api'
import currencyMixin from '@/mixins/currencyMixin'
import FeeRuleDialog from '@/components/FeeRuleDialog.vue'

export default {
  name: 'Cart',
  components: { FeeRuleDialog },
  mixins: [currencyMixin],
  data() {
    return {
      cartList: [],
      loading: false,
      checkoutLoading: false,
      addressDialogVisible: false,
      addressList: [],
      selectedAddressId: null,
      ruleDialogVisible: false,
      currentRuleEstimate: {},
      ruleDialogTitle: ''
    }
  },
  computed: {
    totalPrice() {
      return this.cartList.reduce((sum, item) => sum + Number(item.estimate.totalPrice || 0), 0)
    },
    paymentTotalPrice() {
      return this.cartList.reduce((sum, item) => sum + Number(item.estimate.paymentTotalPrice || 0), 0)
    },
    showPaymentTotal() {
      return this.cartList.some(item => item.estimate.paymentFallbackApplied)
    },
    hasEstimateError() {
      return this.cartList.some(item => !item.estimate || item.estimate.totalPrice == null)
    }
  },
  watch: {
    currentCurrency() {
      this.loadCart()
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
          const product = products[i].data || {}
          const quoteRes = await orderApi.estimate({
            productId: item.productId,
            quantity: item.quantity,
            settlementCurrency: this.currentCurrency
          })
          list.push({
            ...item,
            productTitle: product.title,
            productImage: product.image,
            productPrice: product.price,
            currency: product.currency,
            stock: product.stock,
            restrictedFlag: product.restrictedFlag,
            estimate: quoteRes.data || {}
          })
        }
        this.cartList = list
      } catch (error) {
        this.cartList = []
        this.$message.error(error.message || '加载购物车失败')
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
        const quoteRes = await orderApi.estimate({
          productId: item.productId,
          quantity: item.quantity,
          settlementCurrency: this.currentCurrency
        })
        item.estimate = quoteRes.data || {}
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
        this.$message.error(error.message || '删除失败')
      }
    },
    async checkout() {
      if (this.hasEstimateError) {
        this.$message.warning('当前有商品尚未完成税费估算，请稍后重试')
        return
      }
      try {
        const res = await addressApi.getAddressList()
        this.addressList = res.data || []
        const defaultAddress = this.addressList.find(item => item.isDefault === 1)
        if (defaultAddress) {
          this.selectedAddressId = defaultAddress.id
        } else if (this.addressList.length > 0) {
          this.selectedAddressId = this.addressList[0].id
        }
        this.addressDialogVisible = true
      } catch (error) {
        this.$message.error(error.message || '加载收货地址失败')
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
        const orderPromises = this.cartList.map(item => orderApi.createOrder({
          productId: item.productId,
          addressId: this.selectedAddressId,
          quantity: item.quantity,
          settlementCurrency: this.currentCurrency,
          taxDeclarationAccepted: 1,
          restrictedDeclarationAccepted: item.restrictedFlag === 1 ? 1 : 0
        }))
        await Promise.all(orderPromises)
        await cartApi.clearCart()
        this.$message.success('订单创建成功')
        this.addressDialogVisible = false
        this.$router.push('/orders')
      } catch (error) {
        this.$message.error(error.message || '结算失败，请稍后重试')
      } finally {
        this.checkoutLoading = false
      }
    },
    openRuleDialog(item) {
      this.currentRuleEstimate = item && item.estimate ? item.estimate : {}
      this.ruleDialogTitle = item && item.productTitle ? `${item.productTitle}税费和快递规则` : '税费和快递规则'
      this.ruleDialogVisible = true
    },
    formatEstimatePrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || this.currentCurrency || 'CNY', currency || this.currentCurrency || 'CNY')
    },
    formatLiteralPrice(amount, currency) {
      return formatPriceDisplay(amount || 0, currency || 'CNY', currency || 'CNY')
    },
    getTaxModeText(taxMode) {
      return taxMode === 'CBEC_PREFERENTIAL' ? '跨境零售进口优惠口径' : '一般贸易估算口径'
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

.fee-line.hint {
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

.total-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.total,
.pay-total {
  font-size: 18px;
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

.checkout-rule-links {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.settle-alert {
  margin-top: 16px;
}
</style>
