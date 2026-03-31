import axios from '../utils/axios'

export const adminDashboardApi = {
  getOverview(params) {
    return axios.get('/admin/dashboard/overview', { params })
  }
}

