package com.example.smackcheck2.util

import kotlin.math.abs
import kotlin.math.round

fun formatOneDecimal(value: Float): String = formatOneDecimal(value.toDouble())

fun formatOneDecimal(value: Double): String {
    val roundedTenth = round(value * 10.0).toInt()
    val wholePart = roundedTenth / 10
    val decimalPart = abs(roundedTenth % 10)
    return "$wholePart.$decimalPart"
}

fun formatLikeCountCompact(count: Int): String {
    return if (count >= 1000) {
        "${formatOneDecimal(count / 1000.0)}k likes"
    } else {
        "$count likes"
    }
}
