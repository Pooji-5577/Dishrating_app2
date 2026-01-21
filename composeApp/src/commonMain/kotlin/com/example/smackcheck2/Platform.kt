package com.example.smackcheck2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform