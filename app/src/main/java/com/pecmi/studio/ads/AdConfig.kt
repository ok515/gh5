package com.pecmi.studio.ads

import com.pecmi.studio.BuildConfig

/**
 * Centralized Ad Unit IDs for Google AdMob.
 * Dynamically populated from BuildConfig depending on Debug vs Release environment.
 */
object AdConfig {

    object Gms {
        val BANNER_AD_UNIT_ID: String
            get() = BuildConfig.ADMOB_BANNER_ID
        val INTERSTITIAL_AD_UNIT_ID: String
            get() = BuildConfig.ADMOB_INTERSTITIAL_ID
        val REWARDED_AD_UNIT_ID: String
            get() = BuildConfig.ADMOB_REWARDED_ID
    }
}

