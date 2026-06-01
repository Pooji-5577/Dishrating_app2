package com.example.smackcheck2.util

fun decodePercentEncodedText(value: String): String {
    if ('%' !in value && '+' !in value) return value

    val output = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%' && index + 2 < value.length) {
            val decoded = value.substring(index + 1, index + 3).toIntOrNull(16)
            if (decoded != null) {
                output.append(decoded.toChar())
                index += 3
                continue
            }
        }
        output.append(if (char == '+') ' ' else char)
        index += 1
    }
    return output.toString()
}
