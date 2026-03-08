<template>

<div class="detail">

<el-row :gutter="20">

<el-col :span="12">

<el-carousel height="450px">

<el-carousel-item
v-for="(img,i) in imageList"
:key="i"
>

<img
:src="img"
class="detail-img"
/>

</el-carousel-item>

</el-carousel>

</el-col>

<el-col :span="12">

<div class="info">

<h2>{{product.title}}</h2>

<div class="price">
{{ formatPrice(product.price, product.currency) }}
</div>

<div class="item">
分类：{{product.categoryName}}
</div>

<div class="item">
库存：{{product.stock}}
</div>

<div class="item">
发货地址：{{product.shippingAddress}}
</div>

<div class="item" style="display: flex; align-items: center;" v-if="product.sellerId">
卖家：
<Avatar :name="product.sellerNickname" :src="product.sellerAvatar" :size="24" style="margin: 0 5px; cursor: pointer;" @click.native="goToSeller" />
<span style="cursor: pointer;" @click="goToSeller">{{product.sellerNickname}}</span>
</div>

<div class="actions" style="margin-top: 20px;">
  <el-input-number v-model="quantity" :min="1" :max="product.stock || 1" :disabled="!product.stock" style="margin-right: 15px;"></el-input-number>
  <el-button type="primary" icon="el-icon-shopping-cart-2" @click="addToCart" :disabled="!product.stock">加入购物车</el-button>
  <el-button type="danger" icon="el-icon-goods" @click="buyNow" :disabled="!product.stock">立即购买</el-button>
</div>

<div class="desc">

<h3>商品描述</h3>

<p>{{product.description}}</p>

</div>

</div>

</el-col>

</el-row>

<!-- 可能感兴趣的商品（同分类） -->
<section v-if="relatedProducts.length > 0" class="related-section">
  <h3 class="related-title">可能感兴趣的商品</h3>
  <div class="related-grid">
    <div
      v-for="p in relatedProducts"
      :key="p.id"
      class="related-card"
      @click="$router.push('/product/' + p.id)"
    >
      <img :src="getRelatedImage(p)" class="related-img" />
      <div class="related-info">
        <div class="related-name" :title="p.title">{{ p.title }}</div>
        <div class="related-price">{{ formatPrice(p.price, p.currency) }}</div>
      </div>
    </div>
  </div>
</section>

</div>

</template>

<script>

import {productApi, cartApi} from "../api"
import currencyMixin from "@/mixins/currencyMixin"
import Avatar from '@/components/Avatar.vue'

export default{
components: {
  Avatar
},
mixins: [currencyMixin],
data() {
  return {
    product: {},
    imageList: [],
    quantity: 1,
    relatedProducts: []
  }
},

created(){

this.load()

},

methods:{

async addToCart() {
  if (!this.$store.getters.isAuthenticated) {
    this.$message.warning('请先登录')
    this.$router.push('/login')
    return
  }
  try {
    await cartApi.addToCart({
      productId: this.product.id,
      quantity: this.quantity
    })
    this.$message.success('已加入购物车')
  } catch (error) {
    this.$message.error(error.message || '加入购物车失败')
  }
},

async buyNow() {
    if (!this.$store.getters.isAuthenticated) {
      this.$message.warning('请先登录')
      this.$router.push('/login')
      return
    }
    try {
      await cartApi.addToCart({
        productId: this.product.id,
        quantity: this.quantity
      })
      this.$router.push('/cart')
    } catch (error) {
      this.$message.error(error.message || '操作失败')
    }
  },

  goToSeller() {
    if (this.product.sellerId) {
      this.$router.push(`/seller/${this.product.sellerId}`)
    }
  },

  async load(){

const id=this.$route.params.id

const res=await productApi.getProductById(id)

this.product=res.data

if(this.product.images){
  try {
    const parsed = JSON.parse(this.product.images)
    this.imageList = Array.isArray(parsed) ? parsed : [this.product.images]
  } catch (e) {
    if(this.product.images.includes(",")){
      this.imageList = this.product.images.split(",")
    } else {
      this.imageList = [this.product.images]
    }
  }
} else if (this.product.image) {
  this.imageList = [this.product.image]
}
// 同分类推荐（排除当前商品）
if (this.product.categoryId) {
  try {
    const res = await productApi.getProductList({
      categoryId: this.product.categoryId,
      status: 'ON_SALE',
      page: 1,
      size: 8
    })
    const list = res.data && res.data.records ? res.data.records : []
    this.relatedProducts = list.filter(item => item.id !== this.product.id)
  } catch (e) {
    this.relatedProducts = []
  }
}
  },

  getRelatedImage(p) {
    if (p.images) {
      try {
        const parsed = JSON.parse(p.images)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed[0]
      } catch (e) {}
      if (typeof p.images === 'string' && p.images.includes(',')) return p.images.split(',')[0]
      return p.images
    }
    return p.image || '/placeholder.png'
  }
}

}

</script>

<style scoped>

.detail{
padding:20px
}

.detail-img{

width:100%;
height:450px;
object-fit:contain

}

.price{

color:#ff4d4f;
font-size:26px;
margin:10px 0

}

.item{

margin-bottom:6px

}

.desc {
  margin-top: 20px;
}

.related-section {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color, #ebeef5);
}

.related-title {
  font-size: 18px;
  margin: 0 0 20px 0;
  color: var(--text-color, #303133);
  padding-left: 10px;
  border-left: 4px solid var(--primary-color, #409eff);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.related-card {
  background: var(--card-bg-color, #fff);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.related-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}

.related-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.related-info {
  padding: 12px;
}

.related-name {
  font-size: 14px;
  color: var(--text-color, #303133);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-price {
  font-size: 16px;
  font-weight: bold;
  color: var(--danger-color, #f56c6c);
}

</style>