import axios from 'axios'
import Message from './message' // 引入封装后的 Message
import store from '../store'
import router from '../router'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = store.state.token
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      // 不在这里显示错误，让组件自己处理，避免重复弹窗
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      if (error.response.status === 401) {
        Message.error('未登录或登录已过期')
        store.dispatch('logout')
        router.push('/login')
      } else {
        // 不在这里显示错误，让组件自己处理，避免重复弹窗
        // Message.error(error.response.data.message || '请求失败')
      }
    } else {
      // 网络错误才在这里显示
      Message.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
