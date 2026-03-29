import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/css/common.css'
import Message from './utils/message' // 引入封装后的 Message
import currencyMixin from './mixins/currencyMixin'

Vue.config.productionTip = false

Vue.use(ElementUI)
Vue.mixin(currencyMixin)

// 替换默认的 Message
Vue.prototype.$message = Message
ElementUI.Message = Message

const savedTheme = localStorage.getItem('theme')
const theme = savedTheme === 'light' ? 'light' : 'dark'
document.documentElement.setAttribute('data-theme', theme)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
