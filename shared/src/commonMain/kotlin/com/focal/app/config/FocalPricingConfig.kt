package com.focal.app.config

/**
 * Centralized pricing configuration for FOCAL plans and hardware.
 * Eliminates hard-coded prices across screens.
 */
object FocalPricingConfig {
    const val PLAN_MONTHLY_PRICE_INR: Int = 299
    const val PLAN_FREE_TRIAL_MONTHS: Int = 3
    const val NFC_CARD_PRICE_INR: Int = 1000

    const val PLAN_PRICE_PER_MONTH: String = "₹299/month"
    const val PLAN_PRICE_FORMATTED: String = "₹299"
    const val NFC_CARD_PRICE_FORMATTED: String = "₹1,000"
    const val NFC_CARD_ONE_TIME: String = "₹1,000 one-time"
    const val PLAN_TRIAL_SUMMARY: String = "Free for 3 months, then ₹299/month. Cancel anytime."
    const val PLAN_TRIAL_BADGE: String = "Free trial · 3 months free"
}
