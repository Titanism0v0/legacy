import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../utils/axios'

Vue.use(Vuex)

// 从localStorage读取用户信息，处理可能的格式问题
const getUserFromStorage = () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr || userStr === 'null' || userStr === 'undefined') {
      return null
    }
    const user = JSON.parse(userStr)
    console.log('从localStorage读取用户信息:', user)
    return user
  } catch (e) {
    console.error('读取用户信息失败:', e)
    return null
  }
}

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    user: getUserFromStorage(),
    currency: localStorage.getItem('currency') || 'CNH'
  },

  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
      if (token) {
        axios.defaults.headers.common['Authorization'] = 'Bearer ' + token
      } else {
        delete axios.defaults.headers.common['Authorization']
      }
    },

    SET_USER(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
      if (user && user.country) {
        state.currency = user.country
        localStorage.setItem('currency', user.country)
      }
    },

    SET_CURRENCY(state, currency) {
      state.currency = currency
      localStorage.setItem('currency', currency)
    },

    LOGOUT(state) {
      state.token = ''
      state.user = null
      state.currency = 'CNH'
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('currency')
      delete axios.defaults.headers.common['Authorization']
    }
  },

  actions: {
    login({ commit }, { token, user }) {
      commit('SET_TOKEN', token)
      commit('SET_USER', user)
    },
    logout({ commit }) {
      commit('LOGOUT')
    },
    setCurrency({ commit }, currency) {
      commit('SET_CURRENCY', currency)
    }
  },

  getters: {
    isAuthenticated: state => !!state.token,

    userRole: state => {
      return state.user ? state.user.role : null
    },

    currentCurrency: state => state.currency,

    // ✅ 新增：是否管理员
    isAdmin: state => state.user && state.user.role === 'ADMIN',

    // ✅ 新增：是否普通用户（可以买东西的）
    isNormalUser: state => state.user && state.user.role === 'USER',

    // ✅ 新增：是否卖家
    isSeller: state => state.user && state.user.role === 'SELLER'
  }
})