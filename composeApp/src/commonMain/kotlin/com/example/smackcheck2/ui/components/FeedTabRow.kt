package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.ui.theme.PlusJakartaSans

private val tabs = listOf(
    FeedFilter.FOLLOWING to "Following",
    FeedFilter.TRENDING to "Trending",
    FeedFilter.NEARBY to "Nearby",
    FeedFilter.MY_RATINGS to "My Ratings"
)

@Composable
fun FeedTabRow(
    selectedTab: FeedFilter,
    onTabSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val jakartaSans = PlusJakartaSans()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Top
        ) {
            tabs.forEach { (filter, label) ->
                val isSelected = filter == selectedTab

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(filter) }
                        .padding(bottom = 0.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = jakartaSans,
                        color = if (isSelected) Color(0xFF2D2F2F) else Color(0xFF9A9C9C),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Active underline indicator
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(if (isSelected) 40.dp else 0.dp)
                            .background(Color(0xFF9B2335))
                    )
                }
            }
        }

        // Full-width bottom divider
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE7E8E8)
        )
    }
}
