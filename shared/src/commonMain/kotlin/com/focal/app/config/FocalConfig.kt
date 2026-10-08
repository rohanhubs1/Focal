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
}
