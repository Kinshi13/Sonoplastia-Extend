package com.escalachurch.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Generic horizontal-swipe card carousel used by both the Início (scales) and
 * Doxologia screens: swiping right moves to future items, swiping left goes
 * back to past ones. [initialPage] should already point at the resolved
 * "next item" index so the very first frame shows the right card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> CardCarousel(
    items: List<T>,
    initialPage: Int,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
    pageContent: @Composable (T) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (items.size - 1).coerceAtLeast(0))) {
        items.size
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { page ->
            pageContent(items[page])
        }
        PageIndicator(pagerState = pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicator(pagerState: PagerState) {
    if (pagerState.pageCount <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pagerState.pageCount) { index ->
            val selected = index == pagerState.currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(if (selected) 8.dp else 6.dp)
                    .width(if (selected) 20.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}
