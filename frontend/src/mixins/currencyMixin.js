import { formatPriceDisplay } from '@/utils/currency'

export default {
  computed: {
    currentCurrency() {
      return this.$store.state.currency || 'CNY'
    }
  },
  methods: {
    formatPrice(price, sourceCurrency = 'CNY') {
      return formatPriceDisplay(price, sourceCurrency, this.currentCurrency)
    }
  }
}
