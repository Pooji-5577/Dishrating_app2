package com.example.smackcheck2.model

import com.example.smackcheck2.service.GroupedDishReviewItemRequest

data class GroupedReviewFormDraft(
    val dishDrafts: List<CapturedDishDraft>,
    val rating: Float,
    val comment: String,
    val tags: List<String>,
    val restaurant: Restaurant?,
    val items: List<GroupedDishReviewItemRequest>,
    val receiptBytes: ByteArray?,
    val receiptSummary: String?,
    val receiptItems: List<String>,
    val currencySymbol: String,
    val currencyCode: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as GroupedReviewFormDraft
        return dishDrafts == other.dishDrafts &&
            rating == other.rating &&
            comment == other.comment &&
            tags == other.tags &&
            restaurant == other.restaurant &&
            items == other.items &&
            receiptBytes.contentEqualsOrNull(other.receiptBytes) &&
            receiptSummary == other.receiptSummary &&
            receiptItems == other.receiptItems &&
            currencySymbol == other.currencySymbol &&
            currencyCode == other.currencyCode
    }

    override fun hashCode(): Int {
        var result = dishDrafts.hashCode()
        result = 31 * result + rating.hashCode()
        result = 31 * result + comment.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (restaurant?.hashCode() ?: 0)
        result = 31 * result + items.hashCode()
        result = 31 * result + (receiptBytes?.contentHashCode() ?: 0)
        result = 31 * result + (receiptSummary?.hashCode() ?: 0)
        result = 31 * result + receiptItems.hashCode()
        result = 31 * result + currencySymbol.hashCode()
        result = 31 * result + (currencyCode?.hashCode() ?: 0)
        return result
    }
}

enum class PendingGroupedReviewPhase {
    POSTING,
    SUCCEEDED,
    FAILED
}

data class PendingGroupedReviewStatus(
    val draft: GroupedReviewFormDraft,
    val phase: PendingGroupedReviewPhase,
    val xpEarned: Int? = null,
    val errorMessage: String? = null
)

private fun ByteArray?.contentEqualsOrNull(other: ByteArray?): Boolean =
    when {
        this === other -> true
        this == null || other == null -> false
        else -> contentEquals(other)
    }
