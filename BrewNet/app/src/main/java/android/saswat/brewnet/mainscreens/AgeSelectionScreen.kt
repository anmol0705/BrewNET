package android.saswat.brewnet.mainscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@Composable
fun AgeSelectionScreen(
    navController: NavController,
    onAgeSelected: (Int) -> Unit = {}
) {
    var selectedAge by remember { mutableStateOf(32) }
    val ages = (18..100).toList()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val itemHeightDp = 56.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val visibleItems = 7 // Number of items visible at once

    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && isDragging) {
            isDragging = false
            val firstVisibleItem = listState.firstVisibleItemIndex
            val firstVisibleItemOffset = listState.firstVisibleItemScrollOffset
            
            val centerPosition = firstVisibleItem + (firstVisibleItemOffset / itemHeightPx)
            val targetPosition = if (firstVisibleItemOffset > itemHeightPx / 2) {
                firstVisibleItem + 1
            } else {
                firstVisibleItem
            }
            
            listState.animateScrollToItem(targetPosition)
            selectedAge = ages.getOrNull(targetPosition) ?: selectedAge
        }
    }

    LaunchedEffect(Unit) {
        val initialIndex = ages.indexOf(selectedAge)
        if (initialIndex != -1) {
            listState.scrollToItem(initialIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF2F6FF))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "How Old Are You?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please provide your age in years",
            fontSize = 16.sp,
            color = Color(0xFF71777D),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * visibleItems)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val itemIndex = (offset.y / itemHeightPx).toInt()
                            val targetIndex = listState.firstVisibleItemIndex + itemIndex - 2
                            if (targetIndex in ages.indices) {
                                scope.launch {
                                    listState.animateScrollToItem(targetIndex)
                                    selectedAge = ages[targetIndex]
                                }
                            }
                        }
                    },
                contentPadding = PaddingValues(
                    top = itemHeightDp * ((visibleItems - 1) / 2),
                    bottom = itemHeightDp * ((visibleItems - 1) / 2)
                )
            ) {
                items(ages) { age ->
                    val visibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    val centerIndex = visibleItemIndex.value +3
                    val distanceFromCenter = abs(ages.indexOf(age) - centerIndex)
                    val alpha = max(0f, 1f - (distanceFromCenter * 0.25f))
                    val scale = if (age == selectedAge) 1f else max(0.7f, 1f - (distanceFromCenter * 0.1f))

                    AgeItem(
                        age = age,
                        isSelected = age == selectedAge,
                        itemHeight = itemHeightDp,
                        alpha = alpha,
                        scale = scale,
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(ages.indexOf(age))
                                selectedAge = age
                            }
                        }
                    )
                }
            }

            // Selection indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {

                Divider(
                    color = Color(0xFF246BFD),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(itemHeightDp))
                Divider(
                    color = Color(0xFF246BFD),
                    thickness = 1.dp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onAgeSelected(selectedAge) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF246BFD)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AgeItem(
    age: Int,
    isSelected: Boolean,
    itemHeight: Dp,
    alpha: Float,
    scale: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = age.toString(),
            fontSize = 38.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color(0xFF246BFD)
                else -> Color(0xFF71777D)
            },
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        )
    }
}