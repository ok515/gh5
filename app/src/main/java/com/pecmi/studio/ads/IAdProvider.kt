package com.pecmi.studio.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Unified Ad Provider Interface following SOLID principles.
 * Decouples the UI layer from specific Ad SDK implementations.
 */
interface IAdProvider {
    fun initialize(context: Context)

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissedOrFailed: () -> Unit
    )

    fun showInterstitialAd(
        activity: Activity,
        onAdClosed: () -> Unit
    )

    @Composable
    fun BannerAdView(modifier: Modifier)
}
