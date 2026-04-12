package com.example.smackcheck2.util

import kotlinx.datetime.Clock

fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis == 0L) return ""
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - timestampMillis

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        weeks < 52 -> "${weeks}w"
        else -> "${days / 365}y"
    }
}
