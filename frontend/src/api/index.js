import axios from '../utils/axios'

// 用户相关API
export const userApi = {
  login(data) {
    return axios.post('/user/login', data)
  },
  register(data) {
    return axios.post('/user/register', data)
  },
  getUserInfo() {
    return axios.get('/user/info')
  },
  updateUser(data) {
    return axios.put('/user/update', data)
  },
  getUserList(params) {
    return axios.get('/user/list', { params })
  },
  deleteUser(id) {
    return axios.delete(`/user/${id}`)
  },
  resetPasswordByContact(data) {
    return axios.post('/user/reset-password-by-contact', data)
  }
}

// 商品相关API
export const productApi = {
  getProductList(params) {
    return axios.get('/product/list', { params })
  },
  getProductById(id) {
    return axios.get(`/product/detail/${id}`)
  },
  addProduct(data) {
    return axios.post('/product/add', data)
  },
  updateProduct(data) {
    return axios.put('/product/update', data)
  },
  offShelfProduct(id) {
    return axios.put(`/product/off-shelf/${id}`)
  },
  markOutOfStock(id) {
    return axios.put(`/product/out-of-stock/${id}`)
  },
  restoreOnSale(id) {
    return axios.put(`/product/restore-on-sale/${id}`)
  },
  deleteProduct(id) {
    return axios.delete(`/product/${id}`)
  },
  getMyProducts() {
    return axios.get('/product/my-products')
  },
  batchDeleteProducts(ids) {
    return axios.delete('/product/batch', { data: ids })
  },
  getSellerProducts(sellerId, params) {
    return axios.get(`/product/list?sellerId=${sellerId}`, { params })
  }
}

// 分类相关API
export const categoryApi = {
  getAllCategories() {
    return axios.get('/category/list')
  },
  addCategory(data) {
    return axios.post('/category/add', data)
  },
  updateCategory(data) {
    return axios.put('/category/update', data)
  },
  deleteCategory(id) {
    return axios.delete(`/category/${id}`)
  }
}

// 购物车相关API
export const cartApi = {
  addToCart(data) {
    return axios.post('/cart/add', data)
  },
  getCartList() {
    return axios.get('/cart/list')
  },
  updateCartQuantity(data) {
    return axios.put('/cart/update', data)
  },
  deleteCartItem(id) {
    return axios.delete(`/cart/${id}`)
  },
  clearCart() {
    return axios.delete('/cart/clear')
  }
}

// 地址相关API
export const addressApi = {
  addAddress(data) {
    return axios.post('/address/add', data)
  },
  getAddressList() {
    return axios.get('/address/list')
  },
  updateAddress(data) {
    return axios.put('/address/update', data)
  },
  deleteAddress(id) {
    return axios.delete(`/address/${id}`)
  }
}

// 订单相关API
export const orderApi = {
  createOrder(data) {
    return axios.post('/order/create', data)
  },
  payOrder(id) {
    return axios.post(`/order/pay/${id}`)
  },
  getPaymentQRCode(id) {
    return axios.get(`/order/payment-qrcode/${id}`)
  },
  confirmPayment(id, data) {
    return axios.post(`/order/confirm-payment/${id}`, data || {})
  },
  shipOrder(data) {
    return axios.post('/order/ship', data)
  },
  confirmReceipt(id) {
    return axios.post(`/order/confirm/${id}`)
  },
  cancelOrder(id) {
    return axios.post(`/order/cancel/${id}`)
  },
  getOrderList(params) {
    return axios.get('/order/list', { params })
  },
  getOrderById(id) {
    return axios.get(`/order/${id}`)
  },
  deleteOrder(id) {
    return axios.delete(`/order/${id}`)
  },
  batchDeleteOrders(ids) {
    return axios.delete('/order/batch', { data: ids })
  }
}

// 商家评价相关API
export const sellerReviewApi = {
  getList(sellerId) {
    return axios.get('/seller-review/list', { params: { sellerId } })
  },
  add(data) {
    return axios.post('/seller-review/add', data)
  },
  delete(id) {
    return axios.delete(`/seller-review/${id}`)
  },
  hasReviewed(orderId) {
    return axios.get('/seller-review/has-reviewed', { params: { orderId } })
  }
}

// 售后相关API
export const afterSalesApi = {
  apply(data) {
    return axios.post('/after-sales/apply', data)
  },
  getList(params) {
    return axios.get('/after-sales/list', { params })
  },
  getDetail(id) {
    return axios.get(`/after-sales/detail/${id}`)
  },
  audit(data) {
    return axios.post('/after-sales/audit', data)
  }
}
