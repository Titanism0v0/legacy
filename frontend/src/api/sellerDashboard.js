import axios from '../utils/axios'

export const sellerDashboardApi = {
  getOverview(params) {
    return axios.get('/seller/dashboard/overview', { params })
  }
}
