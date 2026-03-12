package com.example.smackcheck2.util

/**
 * Utility object for handling errors and providing user-friendly messages
 */
object ErrorHandler {

    /**
     * Convert an exception to a user-friendly error message
     */
    fun getUserFriendlyMessage(throwable: Throwable?): String {
        if (throwable == null) return "An unknown error occurred. Please try again."

        val message = throwable.message ?: ""

        // Check for common error patterns
        return when {
            // Network errors
            message.contains("network", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ->
                "Network error. Please check your connection and try again."

            // Authentication errors
            message.contains("unauthorized", ignoreCase = true) ||
            message.contains("unauthenticated", ignoreCase = true) ||
            message.contains("token", ignoreCase = true) ->
                "Session expired. Please sign in again."

            // Permission errors
            message.contains("forbidden", ignoreCase = true) ||
            message.contains("permission", ignoreCase = true) ->
                "You don't have permission to perform this action."

            // Database/Schema errors
            message.contains("schema", ignoreCase = true) ||
            message.contains("column", ignoreCase = true) ||
            message.contains("table", ignoreCase = true) ->
                "Database configuration issue. Please contact support if this persists."

            // Not found errors
            message.contains("not found", ignoreCase = true) ||
            message.contains("404", ignoreCase = true) ->
                "The requested resource was not found."

            // Validation errors
            message.contains("invalid", ignoreCase = true) ||
            message.contains("validation", ignoreCase = true) ->
                "Invalid input. Please check your information and try again."

            // Already exists errors
            message.contains("already exists", ignoreCase = true) ||
            message.contains("duplicate", ignoreCase = true) ||
            message.contains("unique", ignoreCase = true) ->
                "This information already exists. Please use different values."

            // Size/limit errors
            message.contains("too large", ignoreCase = true) ||
            message.contains("size", ignoreCase = true) ||
            message.contains("quota", ignoreCase = true) ->
                "The item is too large or you've reached a limit."

            // Rate limit errors
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) ->
                "Too many requests. Please wait a moment and try again."

            // Server errors
            message.contains("500") ||
            message.contains("502") ||
            message.contains("503") ||
            message.contains("server error", ignoreCase = true) ->
                "Server error. Please try again later."

            // If message is already user-friendly (doesn't contain technical terms), use it
            !containsTechnicalTerms(message) && message.isNotBlank() ->
                message

            // Default fallback
            else -> "Something went wrong. Please try again later."
        }
    }

    /**
     * Check if a message contains technical terms that shouldn't be shown to users
     */
    private fun containsTechnicalTerms(message: String): Boolean {
        val technicalTerms = listOf(
            "null", "undefined", "exception", "stack trace",
            "http", "https", "api", "url", "uri",
            "json", "xml", "sql", "query",
            "class", "method", "function", "error:",
            "at com.", "at java.", "at kotlin.",
            "supabase.co", "postgrest", "gotrue",
            "bearer", "authorization", "jwt",
            "x-client-info", "content-type"
        )

        val lowerMessage = message.lowercase()
        return technicalTerms.any { term ->
            lowerMessage.contains(term)
        }
    }

    /**
     * Log error for debugging while returning user-friendly message
     */
    fun handleError(throwable: Throwable?, context: String = ""): String {
        // Print to console for debugging
        if (context.isNotEmpty()) {
            println("Error in $context: ${throwable?.message}")
        } else {
            println("Error: ${throwable?.message}")
        }

        // Print stack trace in debug mode
        throwable?.printStackTrace()

        return getUserFriendlyMessage(throwable)
    }
}
