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

    <div class="category-tip" v-if="categoryTipText">
      {{ categoryTipText }}
    </div>

    <div class="category-nav">
      <el-tag
        :type="selectedTopId===null?'primary':''"
        @click="selectTopCategory(null)"
      >
        全部
      </el-tag>
      <el-tag
        v-for="c in topCategories"
        :key="c.id"
        :type="selectedTopId===c.id?'primary':''"
        @click="selectTopCategory(c.id)"
      >
        {{c.name}}
      </el-tag>
    </div>

    <div v-if="selectedTopId" class="category-nav sub-nav">
      <span class="sub-nav-label">细分类：</span>
      <el-tag
        :type="selectedSubId===null?'primary':''"
        size="small"
        @click="selectSubCategory(null)"
      >
        全部
      </el-tag>
      <el-tag
        v-for="s in subCategories"
        :key="s.id"
        :type="selectedSubId===s.id?'primary':''"
        size="small"
        @click="selectSubCategory(s.id)"
      >
        {{s.name}}
      </el-tag>
      <span v-if="subCategories.length===0" class="sub-nav-empty">（该分类下暂无细分类）</span>
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
topCategories:[],
subCategories:[],
selectedTopId:null,
selectedSubId:null,
productList:[],
currentPage: 1,
pageSize: 12,
total: 0,
loading: false
}
},
computed:{
categoryTipText(){
if(this.loading) return '加载中…'
if(this.keyword) return `搜索「${this.keyword}」共 ${this.total} 件商品`
if(this.selectedTopId===null) return this.total>=0 ? `当前：全部商品（共 ${this.total} 件）` : ''
const top=this.topCategories.find(c=>c.id===this.selectedTopId)
const topName=top?top.name:''
if(this.selectedSubId!=null){
const sub=this.subCategories.find(s=>s.id===this.selectedSubId)
const subName=sub?sub.name:''
return `当前：${topName} > ${subName}（共 ${this.total} 件）`
}
return `当前：${topName}（共 ${this.total} 件）`
}
},

created(){
this.loadTopCategories()
this.loadProduct()
},

methods:{

async loadTopCategories(){
const res=await categoryApi.getTopCategories()
this.topCategories=res.data||[]
},

async loadProduct(){
this.loading=true
try{
const categoryId=this.selectedSubId!=null?this.selectedSubId:this.selectedTopId
const res=await productApi.getProductList({
status:"ON_SALE",
page: this.currentPage,
size: this.pageSize,
categoryId: categoryId,
keyword: this.keyword
})
this.productList=res.data.records||[]
this.total=res.data.total!=null?res.data.total:0
}finally{
this.loading=false
}
},

handlePageChange(page) {
  this.currentPage = page
  this.loadProduct()
  window.scrollTo({ top: 0, behavior: 'smooth' })
},

async selectTopCategory(topId){
this.selectedTopId=topId
this.selectedSubId=null
this.currentPage=1
this.subCategories=[]
if(topId){
try{
const res=await categoryApi.getSubCategories(topId)
this.subCategories=Array.isArray(res.data)?res.data:[]
}catch(e){
this.$message.warning('加载子分类失败')
this.subCategories=[]
}
}
this.loadProduct()
},
selectSubCategory(subId){
this.selectedSubId=subId
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

.category-tip{
margin-bottom:10px;
font-size:14px;
color:#666
}

.category-nav{
margin-bottom:20px
}

.category-nav .el-tag{
margin-right:10px;
cursor:pointer
}
.sub-nav{
margin-top:8px;
margin-bottom:12px;
display:flex;
flex-wrap:wrap;
align-items:center;
gap:8px;
}
.sub-nav .el-tag{
margin-right:0;
margin-bottom:4px;
flex-shrink:0;
}
.sub-nav-label{
font-size:13px;
color:#666;
margin-right:4px;
}
.sub-nav-empty{
font-size:12px;
color:#999;
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