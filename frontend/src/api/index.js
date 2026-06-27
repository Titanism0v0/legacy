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
  submitKyc(data) {
    return axios.post('/user/kyc/submit', data)
  },
  auditKyc(data) {
    return axios.post('/user/kyc/audit', data)
  }
}

export const legalApi = {
  getCurrent() {
    return axios.get('/legal/current')
  }
}

export const exchangeRateApi = {
  getCurrent() {
    return axios.get('/exchange-rate/current')
  }
}

// 商品相关API
export const productApi = {
  getProductList(params) {
    return axios.get('/product/list', { params })
  },
  getAdminProductList(params) {
    return axios.get('/admin/products', { params })
  },
  getProductById(id) {
    return axios.get(`/product/detail/${id}`)
  },
  getProductRecommendations(id, params) {
    return axios.get(`/product/detail/${id}/recommendations`, { params })
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
  },
  auditProduct(data) {
    return axios.post('/product/audit', data)
  }
}

// 分类相关API
export const categoryApi = {
  getAllCategories() {
    return axios.get('/category/list')
  },
  getTopCategories() {
    return axios.get('/category/top')
  },
  getSubCategories(parentId) {
    return axios.get(`/category/sub/${parentId}`)
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
  estimate(params) {
    return axios.get('/order/estimate', { params })
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
  },
  auditOrder(data) {
    return axios.post('/order/audit', data)
  },
  updateTracking(data) {
    return axios.post('/order/update-tracking', data)
  },
  getStatusFlow(params) {
    return axios.get('/order/status-flow', { params })
  },
  getOrderStatusFlow(id) {
    return axios.get(`/order/${id}/status-flow`)
  },
  getOrderInsight(id) {
    return axios.get(`/order/${id}/insight`)
  },
  advanceFulfillment(data) {
    return axios.post('/order/advance', data)
  }
}

export const paymentApi = {
  prepay(orderId) {
    return axios.post(`/payment/prepay/${orderId}`)
  },
  getStatus(orderId) {
    return axios.get(`/payment/status/${orderId}`)
  },
  refund(orderId, data) {
    return axios.post(`/payment/refund/${orderId}`, data || {})
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
  },
  respond(data) {
    return axios.post('/after-sales/respond', data)
  },
  sellerDecision(data) {
    return axios.post('/after-sales/seller-decision', data)
  },
  requestArbitration(data) {
    return axios.post('/after-sales/request-arbitration', data)
  },
  arbitrate(data) {
    return axios.post('/after-sales/arbitrate', data)
  },
  getLogs(afterSalesId) {
    return axios.get('/after-sales/logs', { params: { afterSalesId } })
  }
}

// 聊天相关API
export const chatApi = {
  getSessions(params) {
    return axios.get('/chat/sessions', { params })
  },
  getMessages(params) {
    return axios.get('/chat/messages', { params })
  },
  markRead(sessionId) {
    return axios.post('/chat/mark-read', null, { params: { sessionId } })
  },
  startSession(peerUserId) {
    return axios.post('/chat/start', null, { params: { peerUserId } })
  },
  startSessionWithSeller(sellerId) {
    return axios.post('/chat/start', null, { params: { sellerId } })
  }
}

export const communityApi = {
  getPosts(params) {
    return axios.get('/community/posts', { params })
  },
  getPost(id) {
    return axios.get(`/community/posts/${id}`)
  },
  beautifyPost(data) {
    return axios.post('/community/posts/beautify', data)
  },
  createPost(data) {
    return axios.post('/community/posts', data)
  },
  deletePost(id) {
    return axios.delete(`/community/posts/${id}`)
  },
  getComments(postId) {
    return axios.get(`/community/posts/${postId}/comments`)
  },
  createComment(data) {
    return axios.post('/community/comments', data)
  },
  deleteComment(id) {
    return axios.delete(`/community/comments/${id}`)
  },
  adminGetPosts(params) {
    return axios.get('/admin/community/posts', { params })
  },
  adminGetComments(params) {
    return axios.get('/admin/community/comments', { params })
  },
  adminAuditPost(id, data) {
    return axios.post(`/admin/community/posts/${id}/audit`, data)
  },
  adminDeletePost(id) {
    return axios.delete(`/admin/community/posts/${id}`)
  },
  adminDeleteComment(id) {
    return axios.delete(`/admin/community/comments/${id}`)
  }
}

// 订单证据链API
export const orderEvidenceApi = {
  add(data) {
    return axios.post('/order-evidence/add', data)
  },
  list(params) {
    return axios.get('/order-evidence/list', { params })
  }
}
