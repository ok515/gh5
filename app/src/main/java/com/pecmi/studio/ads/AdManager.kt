package com.pecmi.studio.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Unified AdManager Engine for Google AdMob monetization.
 */
object AdManager {

    private var currentProvider: IAdProvider? = null

    /**
     * Dynamically instantiates the Google AdMob Provider.
     */
    @Synchronized
    fun getProvider(context: Context): IAdProvider {
        if (currentProvider == null) {
            currentProvider = GmsAdProvider()
            currentProvider?.initialize(context.applicationContext ?: context)
        }
        return currentProvider!!
    }

    fun initialize(context: Context) {
        getProvider(context).initialize(context)
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissedOrFailed: () -> Unit
    ) {
        getProvider(activity).showRewardedAd(activity, onRewarded, onDismissedOrFailed)
    }

    fun showInterstitialAd(
        activity: Activity,
        onAdClosed: () -> Unit
    ) {
        getProvider(activity).showInterstitialAd(activity, onAdClosed)
    }

    val BANNER_TEST_AD_UNIT_ID: String
        get() = AdConfig.Gms.BANNER_AD_UNIT_ID

    val REWARDED_TEST_AD_UNIT_ID: String
        get() = AdConfig.Gms.REWARDED_AD_UNIT_ID
}

/**
 * Banner View Composable using Google AdMob.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.Gms.BANNER_AD_UNIT_ID
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val provider = AdManager.getProvider(context)
    provider.BannerAdView(modifier = modifier)
}

