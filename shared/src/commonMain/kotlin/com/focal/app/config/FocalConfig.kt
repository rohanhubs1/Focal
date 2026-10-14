package com.focal.app.config

object FocalConfig {
    var isDemoMode: Boolean = true
    var isDebug: Boolean = true

    // Controls whether DEV role switcher floating pill is displayed
    val showDevRoleSwitcher: Boolean
        get() = isDemoMode || isDebug

    // Controls whether same-device pairing button is visible in Parent pairing screen
    val allowSameDevicePairing: Boolean
        get() = isDemoMode

    // Controls whether parent can program/write blank stickers live in parent "Add Card" flow
    val allowParentBlankCardWriting: Boolean
        get() = isDemoMode

    // Config URLs & Links
    const val SUPPORT_EMAIL = "support@focal.io"
    const val WHATSAPP_SUPPORT_URL = "https://wa.me/919876543210?text=Hi%20FOCAL%20Support"
    const val SUPPORT_WHATSAPP = WHATSAPP_SUPPORT_URL
    const val PRIVACY_POLICY_URL = "https://focal.io/privacy"
    const val TERMS_OF_SERVICE_URL = "https://focal.io/terms"
    const val TERMS_URL = TERMS_OF_SERVICE_URL
    const val FAQ_URL = "https://focal.io/faq"
    const val PHYSICAL_CARD_CHECKOUT_URL = "https://focal.io/buy-card"
    const val CARD_ORDER_URL = PHYSICAL_CARD_CHECKOUT_URL
    const val ACCOUNT_DELETION_URL = "https://focal.io/delete-account"
    const val PLAY_STORE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
    const val APP_VERSION_NAME = "v1.2.0"
    const val APP_BUILD_NUMBER = "108"

    // Pricing & Plan Constants
    const val MONTHLY_PRICE_INR = 299
    const val NFC_CARD_PRICE_INR = 1000
    const val FREE_TRIAL_MONTHS = 3
}
