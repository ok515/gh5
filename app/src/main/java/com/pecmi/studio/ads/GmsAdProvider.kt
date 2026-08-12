package com.pecmi.studio.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.pecmi.studio.security.SecurityGuard

class GmsAdProvider : IAdProvider {

    private var isInitialized = false

    override fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissedOrFailed: () -> Unit
    ) {
        initialize(activity)
        val adRequest = AdRequest.Builder().build()
        var isRewardEarned = false

        RewardedAd.load(
            activity,
            AdConfig.Gms.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    try {
                        val ssvOptions = ServerSideVerificationOptions.Builder()
                            .setCustomData("ssv_session_${System.currentTimeMillis()}")
                            .build()
                        rewardedAd.setServerSideVerificationOptions(ssvOptions)
                    } catch (e: Exception) {
                        // SSV optional configuration
                    }

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (isRewardEarned && SecurityGuard.validateRewardGrant(activity, true)) {
                                onRewarded()
                            } else {
                                onDismissedOrFailed()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                            onDismissedOrFailed()
                        }
                    }

                    rewardedAd.show(activity) { rewardItem ->
                        if (rewardItem != null) {
                            isRewardEarned = true
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onDismissedOrFailed()
                }
            }
        )
    }

    @Composable
    override fun BannerAdView(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    initialize(ctx)
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(AdConfig.Gms.BANNER_AD_UNIT_ID)
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
