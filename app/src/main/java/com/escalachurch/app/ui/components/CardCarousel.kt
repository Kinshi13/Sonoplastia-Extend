package com.escalachurch.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Generic horizontal-swipe card carousel used by both the Início (scales) and
 * Doxologia screens: swiping right moves to future items, swiping left goes
 * back to past ones. [initialPage] should already point at the resolved
 * "next item" index so the very first frame shows the right card.
 *
 * [sidePadding] insets the pager from the screen edges so the card's rounded
 * corners and shadow always have breathing room and are never clipped by the
 * viewport - without it, a full-bleed page reads as "cut off" at the sides.
 *
 * Fase 7 (Android parallax): neighboring pages scale down and fade slightly as they recede from
 * center, purely as a function of the pager's own live scroll offset - not a timer, not a sensor,
 * so there is zero cost while idle and nothing to leak in the background (see the battery lesson
 * from the old background-music player, removed in Fase 6). [reducedMotion] disables the effect
 * entirely (pages render flat/full-size), same convention as the rest of the app.
 * [onScrollFractionChanged] exposes that same live offset (in "pages", e.g. 1.35) so a screen can
 * drive an ambient background layer (see ParallaxStarfield) in sync with the same gesture, without
 * this component needing to know anything about that background.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> CardCarousel(
    items: List<T>,
    initialPage: Int,
    modifier: Modifier = Modifier,
    sidePadding: androidx.compose.ui.unit.Dp = 10.dp,
    reducedMotion: Boolean = false,
    onPageChanged: (Int) -> Unit = {},
    onScrollFractionChanged: (Float) -> Unit = {},
    pageContent: @Composable (T) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (items.size - 1).coerceAtLeast(0))) {
        items.size
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(pagerState, reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { fraction -> onScrollFractionChanged(fraction) }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 16.dp
        ) { page ->
            val pageModifier = if (reducedMotion) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    val distance = abs((pagerState.currentPage + pagerState.currentPageOffsetFraction) - page)
                        .coerceIn(0f, 1f)
                    val scale = 1f - distance * 0.08f
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - distance * 0.35f
                }
            }
            Box(modifier = pageModifier) {
                pageContent(items[page])
            }
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
