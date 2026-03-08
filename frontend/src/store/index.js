import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../utils/axios'

Vue.use(Vuex)

// 从localStorage读取用户信息，处理可能的格式问题
const getUserFromStorage = () => {
  try {
    const userStr = sessionStorage.getItem('user')
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
    token: sessionStorage.getItem('token') || '',
    user: getUserFromStorage(),
    currency: localStorage.getItem('currency') || 'CNH'
  },

  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      sessionStorage.setItem('token', token)
      if (token) {
        axios.defaults.headers.common['Authorization'] = 'Bearer ' + token
      } else {
        delete axios.defaults.headers.common['Authorization']
      }
    },

    SET_USER(state, user) {
      state.user = user
      sessionStorage.setItem('user', JSON.stringify(user))
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
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
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

    isAdmin: state => state.user && state.user.role === 'ADMIN',

    isNormalUser: state => state.user && state.user.role === 'USER',

    isSeller: state => state.user && state.user.role === 'SELLER'
  }
})