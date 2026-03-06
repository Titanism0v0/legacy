import { formatPriceDisplay } from '@/utils/currency'

export default {
  computed: {
    currentCurrency() {
      return this.$store.state.currency || 'CNH'
    }
  },
  methods: {
    formatPrice(price, sourceCurrency = 'CNH') {
      return formatPriceDisplay(price, sourceCurrency, this.currentCurrency)
    }
  }
}
