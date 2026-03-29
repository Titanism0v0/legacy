import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../utils/axios'
import { chatApi } from '../api'

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
    currency: localStorage.getItem('currency') || 'CNY',
    chatUnreadTotal: 0
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

    SET_CHAT_UNREAD_TOTAL(state, n) {
      state.chatUnreadTotal = typeof n === 'number' && n >= 0 ? n : 0
    },

    LOGOUT(state) {
      state.token = ''
      state.user = null
      state.currency = 'CNY'
      state.chatUnreadTotal = 0
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
      localStorage.removeItem('currency')
      delete axios.defaults.headers.common['Authorization']
    }
  },

  actions: {
    login({ commit, dispatch }, { token, user }) {
      commit('SET_TOKEN', token)
      commit('SET_USER', user)
      dispatch('refreshChatUnread').catch(() => {})
    },
    logout({ commit }) {
      commit('LOGOUT')
    },
    async refreshChatUnread({ commit, state }) {
      const user = state.user
      const token = state.token
      if (!token || !user || user.role === 'ADMIN') {
        commit('SET_CHAT_UNREAD_TOTAL', 0)
        return
      }
      if (user.role !== 'USER' && user.role !== 'SELLER') {
        commit('SET_CHAT_UNREAD_TOTAL', 0)
        return
      }
      try {
        const res = await chatApi.getSessions({ page: 1, size: 100 })
        const data = res.data || res
        const records = (data && data.records) ? data.records : []
        let total = 0
        for (let i = 0; i < records.length; i++) {
          const s = records[i]
          if (user.role === 'USER') {
            total += s.unreadForBuyer || 0
          } else if (user.role === 'SELLER') {
            total += s.unreadForSeller || 0
          }
        }
        commit('SET_CHAT_UNREAD_TOTAL', total)
      } catch (e) {
        // 静默失败，避免打断其它流程
      }
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
