<template>
  <div class="home">

    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品..."
        @keyup.enter.native="handleSearch"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch"/>
      </el-input>
    </div>

    <div class="category-nav">
      <el-tag
        :type="selectedCategoryId===null?'primary':''"
        @click="selectCategory(null)"
      >
        全部
      </el-tag>

      <el-tag
        v-for="c in categories"
        :key="c.id"
        :type="selectedCategoryId===c.id?'primary':''"
        @click="selectCategory(c.id)"
      >
        {{c.name}}
      </el-tag>
    </div>

    <div class="waterfall">

      <div
        class="waterfall-item"
        v-for="p in productList"
        :key="p.id"
      >
        <el-card
          class="product-card"
          @click.native="goDetail(p.id)"
        >

          <img
            class="product-image"
            :src="getImage(p)"
          />

          <div class="product-info">

            <div class="product-title">
              {{p.title}}
            </div>

            <div class="product-price">
              {{ formatPrice(p.price, p.currency) }}
            </div>

            <div class="product-meta">
              发货地：{{p.shippingAddress}}
              <span>库存：{{p.stock}}</span>
            </div>

            <div class="product-seller" @click="(e) => goSeller(e, p.sellerId)" style="cursor: pointer;">

              <Avatar
                :src="p.sellerAvatar"
                :name="p.sellerNickname"
                :size="24"
                style="margin-right: 6px;"
              />

              {{p.sellerNickname}}

            </div>

          </div>

        </el-card>
      </div>

    </div>

    <!-- 分页组件 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      >
      </el-pagination>
    </div>

  </div>
</template>

<script>
import {productApi,categoryApi} from "../api"
import currencyMixin from "@/mixins/currencyMixin"
import Avatar from '@/components/Avatar.vue'

export default{
components: {
  Avatar
},
mixins: [currencyMixin],
data(){
return{
keyword:"",
categories:[],
selectedCategoryId:null,
productList:[],
currentPage: 1,
pageSize: 12,
total: 0
}
},

created(){
this.loadCategory()
this.loadProduct()
},

methods:{

async loadCategory(){

const res=await categoryApi.getAllCategories()
this.categories=res.data

},

async loadProduct(){

const res=await productApi.getProductList({
status:"ON_SALE",
page: this.currentPage,
size: this.pageSize,
categoryId: this.selectedCategoryId,
keyword: this.keyword
})

this.productList=res.data.records
this.total=res.data.total

},

handlePageChange(page) {
  this.currentPage = page
  this.loadProduct()
  window.scrollTo({ top: 0, behavior: 'smooth' })
},

selectCategory(id){

this.selectedCategoryId=id
this.currentPage=1
this.loadProduct()

},

handleSearch(){

this.currentPage=1
this.loadProduct()

},

goDetail(id){

    this.$router.push(`/product/${id}`)

    },

    goSeller(e, id){
      e.stopPropagation()
      this.$router.push(`/seller/${id}`)
    },

    getImage(p){
      if(p.images){
        try {
          const parsed = JSON.parse(p.images)
          if(Array.isArray(parsed) && parsed.length > 0) return parsed[0]
        } catch(e) {}

        if(p.images.includes(",")){
          return p.images.split(",")[0]
        }
        return p.images
      }
      if(p.image){
        return p.image
      }
      return ""
    }

}

}
</script>

<style scoped>

.home{
padding:20px
}

.search-bar{
width:600px;
margin:auto;
margin-bottom:20px
}

.category-nav{
margin-bottom:20px
}

.category-nav .el-tag{
margin-right:10px;
cursor:pointer
}

/* 瀑布流 */

.waterfall{

column-count:3;
column-gap:20px

}

.waterfall-item{

break-inside:avoid;
margin-bottom:20px

}

.product-card{

cursor:pointer;
transition:.3s

}

.product-card:hover{

transform:translateY(-5px)

}

.product-image{

width:100%;
border-radius:6px

}

.product-info{

margin-top:10px

}

.product-title{

font-size:16px;
font-weight:600;
margin-bottom:5px

}

.product-price{

color:#ff4d4f;
font-size:18px;
margin-bottom:5px

}

.product-meta{

font-size:12px;
color:#999;
display:flex;
justify-content:space-between

}

.product-seller{

margin-top:6px;
display:flex;
align-items:center;
font-size:12px

}

.avatar{

width:24px;
height:24px;
border-radius:50%;
margin-right:6px

}

.pagination-container {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding-bottom: 20px;
}

</style>