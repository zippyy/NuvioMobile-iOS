package com.nuvio.app.features.livetv

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLiveTvStore(): LiveTvStore
expect suspend fun liveTvHttpText(url: String, headers: Map<String,String>): String
expect fun liveTvNow(): Long
expect fun liveTvTimeLabel(epochMs: Long): String
