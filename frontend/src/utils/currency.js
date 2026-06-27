export const currencies = [
  { label: '人民币 (CNY)', value: 'CNY', symbol: '¥' },
  { label: '离岸人民币 (CNH)', value: 'CNH', symbol: '¥' },
  { label: '美元 (USD)', value: 'USD', symbol: '$' },
  { label: '欧元 (EUR)', value: 'EUR', symbol: '€' },
  { label: '日元 (JPY)', value: 'JPY', symbol: '¥' },
  { label: '英镑 (GBP)', value: 'GBP', symbol: '£' },
  { label: '韩元 (KRW)', value: 'KRW', symbol: '₩' },
  { label: '加元 (CAD)', value: 'CAD', symbol: 'C$' },
  { label: '澳元 (AUD)', value: 'AUD', symbol: 'A$' },
  { label: '港币 (HKD)', value: 'HKD', symbol: 'HK$' },
  { label: '瑞士法郎 (CHF)', value: 'CHF', symbol: 'Fr' },
  { label: '瑞典克朗 (SEK)', value: 'SEK', symbol: 'kr' }
]

const EXCHANGE_RATE_STORAGE_KEY = 'exchange_rates'
let runtimeRates = readRatesFromStorage()

function normalizeCurrency(value) {
  const alias = (value || '').toUpperCase()
  if (alias === 'RMB') return 'CNY'
  return alias || 'CNY'
}

function readRatesFromStorage() {
  if (typeof localStorage === 'undefined') {
    return { CNY: 1, CNH: 1 }
  }
  try {
    const raw = localStorage.getItem(EXCHANGE_RATE_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return sanitizeRates(parsed)
  } catch (e) {
    return { CNY: 1, CNH: 1 }
  }
}

function sanitizeRates(rates) {
  const normalized = {}
  Object.keys(rates || {}).forEach((key) => {
    const currency = normalizeCurrency(key)
    const rate = Number(rates[key])
    if (!Number.isNaN(rate) && rate > 0) {
      normalized[currency] = rate
    }
  })
  normalized.CNY = 1
  normalized.CNH = 1
  return normalized
}

function getRateToCny(currency) {
  const normalized = normalizeCurrency(currency)
  if (normalized === 'CNY' || normalized === 'CNH') return 1
  const rate = Number(runtimeRates[normalized])
  if (!Number.isNaN(rate) && rate > 0) return rate
  return 1
}

export const setExchangeRates = (rates) => {
  runtimeRates = sanitizeRates(rates)
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(EXCHANGE_RATE_STORAGE_KEY, JSON.stringify(runtimeRates))
  }
}

export const getExchangeRates = () => {
  return { ...runtimeRates }
}

export const getCurrencyByValue = (value) => {
  const normalized = normalizeCurrency(value)
  return currencies.find(c => c.value === normalized) || currencies[0]
}

export const convertPrice = (price, sourceCurrencyValue, targetCurrencyValue) => {
  if (price === null || price === undefined || price === '') return 0

  const sourceCurrency = normalizeCurrency(sourceCurrencyValue || 'CNY')
  const targetCurrency = normalizeCurrency(targetCurrencyValue || 'CNY')
  const sourceRate = getRateToCny(sourceCurrency)
  const targetRate = getRateToCny(targetCurrency)
  if (targetRate <= 0) return 0

  const priceInCny = Number(price) * sourceRate
  const result = priceInCny / targetRate

  if (targetCurrency === 'JPY' || targetCurrency === 'KRW') return Math.round(result)
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
