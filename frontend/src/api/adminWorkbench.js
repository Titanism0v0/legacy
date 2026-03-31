import { adminDashboardApi } from './adminDashboard'

const QUICK_LINKS = [
  {
    key: 'products',
    title: '商品管理',
    description: '审核商品和处理上下架',
    path: '/admin/products',
    icon: 'el-icon-s-goods'
  },
  {
    key: 'orders',
    title: '订单管理',
    description: '处理待审核与异常订单',
    path: '/admin/orders',
    icon: 'el-icon-s-order'
  },
  {
    key: 'users',
    title: '用户管理',
    description: '审核商家资质与用户状态',
    path: '/admin/users',
    icon: 'el-icon-user-solid'
  },
  {
    key: 'community',
    title: '社区管理',
    description: '处理待审核帖子与评论',
    path: '/admin/community',
    icon: 'el-icon-chat-dot-square'
  },
  {
    key: 'afterSales',
    title: '售后管理',
    description: '处理售后申请与仲裁',
    path: '/after-sales/list',
    icon: 'el-icon-service'
  }
]

export const adminWorkbenchApi = {
  async getOverview(params) {
    const res = await adminDashboardApi.getOverview(params)
    const data = res.data || {}
    const summary = data.summary || {}
    const riskCount = Number(summary.highRiskProductCount || 0)

    return {
      summary: {
        pendingOrderAuditCount: Number(summary.pendingOrderAuditCount || 0),
        pendingProductAuditCount: Number(summary.pendingProductAuditCount || 0),
        pendingKycCount: Number(summary.pendingKycCount || 0),
        pendingAfterSalesCount: Number(summary.pendingAfterSalesCount || 0),
        pendingCommunityReviewCount: Number(summary.pendingCommunityReviewCount || 0),
        orderCount: Number(summary.orderCount || 0),
        orderAmount: Number(summary.orderAmount || 0)
      },
      risk: {
        highRiskCount: riskCount,
        highRiskHintText: riskCount > 0
          ? '检测到高风险或受限商品，建议优先处理商品审核与下架。'
          : '当前未发现高风险商品。'
      },
      orderStatusBreakdown: Array.isArray(data.orderStatusBreakdown) ? data.orderStatusBreakdown : [],
      orderTrend: Array.isArray(data.orderTrend) ? data.orderTrend : [],
      range: data.range || {},
      quickLinks: QUICK_LINKS
    }
  }
}

