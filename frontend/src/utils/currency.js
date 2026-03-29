export const currencies = [
  { label: '人民币 (CNY)', value: 'CNY', rate: 6.85, symbol: '¥' },
  { label: '离岸人民币 (CNH)', value: 'CNH', rate: 6.85, symbol: '¥' },
  { label: '美元 (USD)', value: 'USD', rate: 1.0, symbol: '$' },
  { label: '欧元 (EUR)', value: 'EUR', rate: 0.85, symbol: '€' },
  { label: '日元 (JPY)', value: 'JPY', rate: 156.34, symbol: '¥' },
  { label: '英镑 (GBP)', value: 'GBP', rate: 0.74, symbol: '£' },
  { label: '韩元 (KRW)', value: 'KRW', rate: 1378.0, symbol: '₩' },
  { label: '加元 (CAD)', value: 'CAD', rate: 1.37, symbol: 'C$' },
  { label: '澳元 (AUD)', value: 'AUD', rate: 1.42, symbol: 'A$' },
  { label: '港币 (HKD)', value: 'HKD', rate: 7.8, symbol: 'HK$' },
  { label: '瑞士法郎 (CHF)', value: 'CHF', rate: 0.77, symbol: 'Fr' },
  { label: '瑞典克朗 (SEK)', value: 'SEK', rate: 9.06, symbol: 'kr' }
]

export const getCurrencyByValue = (value) => {
  const alias = (value || '').toUpperCase()
  const normalized = alias === 'RMB' || alias === 'CNH' ? 'CNY' : alias
  return currencies.find(c => c.value === normalized) || currencies[0]
}

export const convertPrice = (price, sourceCurrencyValue, targetCurrencyValue) => {
  const source = getCurrencyByValue(sourceCurrencyValue || 'CNY')
  const target = getCurrencyByValue(targetCurrencyValue || 'CNY')
  if (!price) return 0
  const priceInUSD = Number(price) / source.rate
  const result = priceInUSD * target.rate
  if (target.value === 'JPY' || target.value === 'KRW') return Math.round(result)
  return Number(result.toFixed(2))
}

export const formatPriceDisplay = (price, sourceCurrencyValue, targetCurrencyValue) => {
  let source = sourceCurrencyValue
  let target = targetCurrencyValue
  if (targetCurrencyValue === undefined) {
    target = sourceCurrencyValue
    source = 'CNY'
  }
  const targetObj = getCurrencyByValue(target)
  const converted = convertPrice(price, source, target)
  return `${targetObj.symbol} ${converted}`
}

