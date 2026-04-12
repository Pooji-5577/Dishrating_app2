package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import kotlin.math.round
import org.jetbrains.compose.resources.painterResource
import smackcheck.composeapp.generated.resources.Res
import smackcheck.composeapp.generated.resources.home_container_2
import smackcheck.composeapp.generated.resources.home_container_3
import smackcheck.composeapp.generated.resources.home_map_banner

@Composable
fun DarkHomeScreen(
    currentLocation: String,
    isLoading: Boolean = false,
    allRestaurants: List<Restaurant> = emptyList(),
    allDishes: List<Dish> = emptyList(),
    noRestaurantsFound: Boolean = false,
    photoViewModel: RestaurantPhotoViewModel? = null,
    onLocationClick: () -> Unit,
    onDishClick: (String) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMapClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    onGameClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    onTopDishesClick: () -> Unit = {},
    onTopRestaurantsClick: () -> Unit = {},
    onNearbyRestaurantsClick: () -> Unit = {},
    onSocialFeedClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddRestaurantClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg = Color(0xFFF7F5F2)
    val textPrimary = Color(0xFF2A201B)
    val textSecondary = Color(0xFF8A7C74)
    val chipBg = Color(0xFFD8CFC9)
    val chipActive = Color(0xFF7B2B2D)
    val cardBg = Color(0xFFFFFEFD)

    val dishCards = if (allDishes.isEmpty()) {
        listOf(
            DishCardData("d1", "Truffl Tagliatelle", "Olive's Plate", 4.8f),
            DishCardData("d2", "Wagyu Burger", "Stack & Co.", 4.7f)
        )
    } else {
        allDishes.take(6).map {
            DishCardData(it.id, it.name, it.restaurantName, it.rating)
        }
    }

    val restaurants = if (allRestaurants.isEmpty()) {
        listOf(
            RestaurantListData("r1", "Nami Sushi", "Japanese Sushi", 4.8f, "1.5 km"),
            RestaurantListData("r2", "Stack & Co.", "American", 4.6f, "2.4 km"),
            RestaurantListData("r3", "Osteria Marco", "Italian", 4.3f, "4.6 km")
        )
    } else {
        allRestaurants.take(6).map {
            RestaurantListData(
                id = it.id,
                name = it.name,
                subtitle = it.cuisine,
                rating = it.averageRating,
                distance = "${if (it.city.isBlank()) "2.0" else "1.8"} km"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeTopBar(onNotificationsClick = onNotificationsClick, onProfileClick = onProfileClick)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Good evening, Ava", color = textSecondary, fontSize = 12.sp)
                    Text(
                        "Find your next meal.",
                        color = textPrimary,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                SearchCard(onSearchClick = onSearchClick)
            }

            item {
                FilterRow(
                    filters = listOf("All", "Hot Now", "Italian", "Asian"),
                    chipBg = chipBg,
                    chipActive = chipActive
                )
            }

            item {
                SectionTitle("Following")
            }

            item {
                FollowingRow()
            }

            item {
                HeaderWithAction("Top Dishes Today", "See all", onTopDishesClick)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dishCards) { dish ->
                        TopDishCard(
                            dish = dish,
                            onClick = { onDishClick(dish.id) },
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            item {
                HeaderWithAction("Nearby Restaurants", "", onNearbyRestaurantsClick, showArrow = true)
            }

            item {
                NearbyCard(
                    name = restaurants.firstOrNull()?.name ?: "Nami Sushi",
                    subtitle = "Famous for their Salmon Aburi Nigiri and everyday fresh service",
                    rating = restaurants.firstOrNull()?.rating ?: 4.8f,
                    onClick = {
                        restaurants.firstOrNull()?.let { onRestaurantClick(it.id) }
                    }
                )
            }

            item {
                MapBanner(onNearbyRestaurantsClick = onNearbyRestaurantsClick)
            }

            item {
                NearbyCard(
                    name = restaurants.getOrNull(1)?.name ?: "Stack & Co.",
                    subtitle = "Home of the grilled brisket smash burger with bold flavor",
                    rating = restaurants.getOrNull(1)?.rating ?: 4.6f,
                    onClick = {
                        restaurants.getOrNull(1)?.let { onRestaurantClick(it.id) }
                    }
                )
            }

            item {
                HeaderWithAction("Top Restaurants This Week", "View Ranking", onTopRestaurantsClick)
            }

            items(restaurants.take(3)) { row ->
                TopRestaurantRow(
                    data = row,
                    onClick = { onRestaurantClick(row.id) },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }

        HomeBottomBar(
            onHomeClick = {},
            onSearchClick = onSearchClick,
            onCenterClick = onGameClick,
            onMapClick = onMapClick,
            onProfileClick = onProfileClick
        )
    }
}

@Composable
private fun HomeTopBar(onNotificationsClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("SmackCheck", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A201B))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.NotificationsNone,
                contentDescription = "Notifications",
                tint = Color(0xFF44352F),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onNotificationsClick() }
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1C8C3))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = Color(0xFF5B4A41),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchCard(onSearchClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSearchClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEDE7E3)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color(0xFF9C8D86),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Search dishes...", color = Color(0xFF9C8D86), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCCFC8)),
                contentAlignment = Alignment.Center
            ) {
                Text("•", color = Color(0xFF8E7E76), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterRow(filters: List<String>, chipBg: Color, chipActive: Color) {
    var selected by remember { mutableStateOf(filters.first()) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { label ->
            val active = selected == label
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (active) chipActive else chipBg,
                modifier = Modifier.clickable { selected = label }
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else Color(0xFF5E4D45),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color(0xFF2A201B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun HeaderWithAction(
    title: String,
    action: String,
    onActionClick: () -> Unit,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color(0xFF2A201B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (showArrow) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = Color(0xFF6C5D55),
                modifier = Modifier.size(16.dp)
            )
        } else if (action.isNotBlank()) {
            Text(
                action,
                color = Color(0xFF8A7C74),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun FollowingRow() {
    val users = listOf("A", "M", "J", "K")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        users.forEach { label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB16F58)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TopDishCard(
    dish: DishCardData,
    onClick: () -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = Modifier
            .width(165.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = cardBg
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Image(
                painter = painterResource(Res.drawable.home_container_2),
                contentDescription = dish.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = dish.name,
                color = textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dish.restaurant,
                color = textSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text("★ ${format1Decimal(dish.rating)}", color = Color(0xFF8B6A57), fontSize = 9.sp)
        }
    }
}

@Composable
private fun NearbyCard(name: String, subtitle: String, rating: Float, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFEFD)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Image(
                painter = painterResource(Res.drawable.home_container_3),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = Color(0xFF2A201B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("★ ${format1Decimal(rating)}", color = Color(0xFF8B6A57), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color(0xFF8A7C74), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF7B2B2D)) {
                Text(
                    "Book Table",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MapBanner(onNearbyRestaurantsClick: () -> Unit) {
    Image(
        painter = painterResource(Res.drawable.home_map_banner),
        contentDescription = "8 restaurants near you",
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onNearbyRestaurantsClick() },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TopRestaurantRow(
    data: RestaurantListData,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFB18E69)),
            contentAlignment = Alignment.Center
        ) {
            Text(data.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(data.name, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(data.subtitle, color = textSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("★ ${format1Decimal(data.rating)}", color = Color(0xFF8B6A57), fontSize = 9.sp)
            Text(data.distance, color = textSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeBottomBar(
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCenterClick: () -> Unit,
    onMapClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(color = Color(0xFFFAF8F6), shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = Color(0xFFC24E56),
                modifier = Modifier.size(20.dp).clickable { onHomeClick() }
            )
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color(0xFFC3BAB5),
                modifier = Modifier.size(20.dp).clickable { onSearchClick() }
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB10F2E))
                    .clickable { onCenterClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = "Map",
                tint = Color(0xFFC3BAB5),
                modifier = Modifier.size(20.dp).clickable { onMapClick() }
            )
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = "Profile",
                tint = Color(0xFFC3BAB5),
                modifier = Modifier.size(20.dp).clickable { onProfileClick() }
            )
        }
    }
}

private data class DishCardData(
    val id: String,
    val name: String,
    val restaurant: String,
    val rating: Float
)

private data class RestaurantListData(
    val id: String,
    val name: String,
    val subtitle: String,
    val rating: Float,
    val distance: String
)

private fun format1Decimal(value: Float): String {
    val rounded = round(value * 10f) / 10f
    return rounded.toString()
}
