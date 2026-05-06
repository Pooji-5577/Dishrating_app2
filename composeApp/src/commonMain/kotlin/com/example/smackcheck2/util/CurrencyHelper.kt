package com.example.smackcheck2.util

data class CurrencyInfo(val code: String, val symbol: String) {
    fun format(amount: Double): String {
        val formatted = if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            ((amount * 100).toLong() / 100.0).toString()
        }
        return "$symbol$formatted"
    }
}

object CurrencyHelper {
    val DEFAULT = CurrencyInfo(code = "INR", symbol = "\u20B9 ")

    private val byCountry = mapOf(
        "PL" to CurrencyInfo(code = "PLN", symbol = "zł "),
        "IN" to CurrencyInfo(code = "INR", symbol = "\u20B9 "),
        "US" to CurrencyInfo(code = "USD", symbol = "$"),
        "GB" to CurrencyInfo(code = "GBP", symbol = "£"),
        "JP" to CurrencyInfo(code = "JPY", symbol = "¥"),
        "CN" to CurrencyInfo(code = "CNY", symbol = "¥"),
        "CA" to CurrencyInfo(code = "CAD", symbol = "CA$"),
        "AU" to CurrencyInfo(code = "AUD", symbol = "A$"),
        "CH" to CurrencyInfo(code = "CHF", symbol = "CHF "),
        "SE" to CurrencyInfo(code = "SEK", symbol = "kr "),
        "NO" to CurrencyInfo(code = "NOK", symbol = "kr "),
        "DK" to CurrencyInfo(code = "DKK", symbol = "kr "),
        "CZ" to CurrencyInfo(code = "CZK", symbol = "Kč "),
        "HU" to CurrencyInfo(code = "HUF", symbol = "Ft "),
        "RO" to CurrencyInfo(code = "RON", symbol = "lei "),
        "BG" to CurrencyInfo(code = "BGN", symbol = "лв "),
        "TR" to CurrencyInfo(code = "TRY", symbol = "₺"),
        "RU" to CurrencyInfo(code = "RUB", symbol = "₽"),
        "BR" to CurrencyInfo(code = "BRL", symbol = "R$"),
        "MX" to CurrencyInfo(code = "MXN", symbol = "MX$"),
        "AE" to CurrencyInfo(code = "AED", symbol = "د.إ "),
        "SG" to CurrencyInfo(code = "SGD", symbol = "S$"),
        "HK" to CurrencyInfo(code = "HKD", symbol = "HK$"),
        "KR" to CurrencyInfo(code = "KRW", symbol = "₩"),
        "TH" to CurrencyInfo(code = "THB", symbol = "฿")
    )

    // Eurozone countries that share EUR
    private val euroZone = setOf(
        "AT", "BE", "CY", "EE", "FI", "FR", "DE", "GR", "IE", "IT", "LV",
        "LT", "LU", "MT", "NL", "PT", "SK", "SI", "ES", "HR"
    )

    private val euro = CurrencyInfo(code = "EUR", symbol = "€")

    fun forCountry(countryCode: String?): CurrencyInfo {
        if (countryCode.isNullOrBlank()) return DEFAULT
        val cc = countryCode.uppercase()
        byCountry[cc]?.let { return it }
        if (cc in euroZone) return euro
        return DEFAULT
    }
}
