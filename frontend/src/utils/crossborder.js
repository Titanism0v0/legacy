const DEFAULT_TAX_RATE = 0.1
const BASE_SHIPPING = 35
const PER_ITEM_SHIPPING = 8

export function estimateByLocalRule(product, quantity = 1) {
  const qty = Math.max(1, Number(quantity || 1))
  const unitPrice = Number(product?.price || 0)
  const subtotalPrice = Number((unitPrice * qty).toFixed(2))
  const taxRateSnapshot = resolveTaxRate(product?.categoryId)
  const taxEstimatedAmount = Number((subtotalPrice * taxRateSnapshot).toFixed(2))
  const shippingFeeSnapshot = Number((BASE_SHIPPING + qty * PER_ITEM_SHIPPING).toFixed(2))
  const totalPrice = Number((subtotalPrice + taxEstimatedAmount + shippingFeeSnapshot).toFixed(2))
  return {
    subtotalPrice,
    taxRateSnapshot,
    taxEstimatedAmount,
    shippingFeeSnapshot,
    totalPrice,
    customsClearanceStatus: 'PENDING_DECLARATION',
    taxIncludedFlag: 0
  }
}

function resolveTaxRate(categoryId) {
  if (Number(categoryId) === 1) return 0.15
  if (Number(categoryId) === 2) return 0.2
  if (Number(categoryId) === 4) return 0.13
  return DEFAULT_TAX_RATE
}

