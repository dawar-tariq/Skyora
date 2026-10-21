package com.skyora.weather.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * A wall clock that refreshes a few times a minute. Weather timestamps and
 * "updated 4 min ago" labels stay truthful without every screen running its own
 * timer.
 */
@Composable
fun rememberNowMillis(intervalMs: Long = 30_000L): Long {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return nowMillis
}
