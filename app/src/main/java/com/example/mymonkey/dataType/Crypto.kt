package com.example.mymonkey.dataType

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Crypto(
    val name: String,
    val buyPrice: Double,
    val amount: Double,
    val id: Int
) : Parcelable {
    val profit: Double = 0.0
}

val AvailableCryptos = listOf(
    "Bitcoin",
    "Ethereum",
    "XRP",
    "Dogecoin"
)

data class CryptoWithPrice(
    val crypto: Crypto,
    val currentPrice: Double
) {
    val currentValue: Double get() = currentPrice * crypto.amount
    val purchaseValue: Double get() = crypto.buyPrice * crypto.amount
    val profit: Double get() = currentValue - purchaseValue
}