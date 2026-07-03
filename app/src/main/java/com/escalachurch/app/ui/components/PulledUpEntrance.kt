package com.escalachurch.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

/**
 * Whether a [PulledUpEntrance] on this screen should currently be visible: starts hidden and
 * flips to visible shortly after each time the screen's lifecycle resumes (not just once on
 * first composition), so the entrance animation replays every time you navigate back to a tab -
 * saveState/restoreState on the nav graph keeps composables' `remember` state alive across tab
 * switches, so a plain one-shot LaunchedEffect(Unit) would only ever fire the first time.
 */
@Composable
fun rememberEntranceVisible(animationsEnabled: Boolean): Boolean {
    var entryKey by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        entryKey++
        onPauseOrDispose { }
    }
    var visible by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(entryKey, animationsEnabled) {
        if (!animationsEnabled) {
            visible = true
            return@LaunchedEffect
        }
        visible = false
        delay(16) // let AnimatedVisibility register the hidden state before flipping, so the enter transition replays
        visible = true
    }
    return visible
}

/**
 * Wraps [content] so it slides up from below with a soft spring bounce ("pulled into place")
 * combined with a fade, each time [visible] flips to true - see [rememberEntranceVisible].
 */
@Composable
fun PulledUpEntrance(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ) { fullHeight -> fullHeight / 2 } + fadeIn(tween(400))
    ) { content() }
}
