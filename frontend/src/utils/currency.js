export const currencies = [
  { label: '人民币 (CNH)', value: 'CNH', rate: 6.85, symbol: '¥' },
  { label: '欧元 (EUR)', value: 'EUR', rate: 0.85, symbol: '€' },
  { label: '日元 (JPY)', value: 'JPY', rate: 156.34, symbol: '¥' },
  { label: '英镑 (GBP)', value: 'GBP', rate: 0.74, symbol: '£' },
  { label: '瑞士法郎 (CHF)', value: 'CHF', rate: 0.77, symbol: 'Fr' },
  { label: '澳元 (AUD)', value: 'AUD', rate: 1.42, symbol: 'A$' },
  { label: '加元 (CAD)', value: 'CAD', rate: 1.37, symbol: 'C$' },
  { label: '港元 (HKD)', value: 'HKD', rate: 7.80, symbol: 'HK$' },
  { label: '瑞典克朗 (SEK)', value: 'SEK', rate: 9.06, symbol: 'kr' },
  { label: '美元 (USD)', value: 'USD', rate: 1.00, symbol: '$' }
]

export const getCurrencyByValue = (value) => {
  return currencies.find(c => c.value === value) || currencies[0] // Default to CNH
}

export const convertPrice = (price, sourceCurrencyValue, targetCurrencyValue) => {
  // 如果没有指定源货币，默认为 CNH（兼容旧代码）
  // 但为了安全，最好调用者明确传源货币
  const source = getCurrencyByValue(sourceCurrencyValue || 'CNH')
  const target = getCurrencyByValue(targetCurrencyValue || 'CNH')
  
  if (!price) return 0
  
  // 1. Convert source to USD (Base)
  // Source Price / Source Rate = USD Price
  const priceInUSD = price / source.rate
  
  // 2. Convert USD to Target
  // USD Price * Target Rate = Target Price
  const result = priceInUSD * target.rate
  
  // Format based on currency (JPY usually no decimals, others 2)
  if (target.value === 'JPY') {
    return Math.round(result)
  }
  return result.toFixed(2)
}

export const formatPriceDisplay = (price, sourceCurrencyValue, targetCurrencyValue) => {
  // 如果只传了两个参数，假设第二个是 target，第一个是 priceInCNH（兼容旧逻辑）
  // 但现在主要逻辑应该是三个参数：price, source, target
  // 为了兼容性，我们可以检查参数
  
  let source = sourceCurrencyValue
  let target = targetCurrencyValue
  
  // 如果只传了两个参数，假设 source 是 CNH
  if (targetCurrencyValue === undefined) {
    target = sourceCurrencyValue
    source = 'CNH'
  }
  
  const targetObj = getCurrencyByValue(target)
  const converted = convertPrice(price, source, target)
  return `${targetObj.symbol} ${converted}`
}
