package com.example.myapplication.init

import android.content.Context
import com.focal.app.data.AuthRepository
import com.focal.app.data.FocalRepository
import com.focal.app.data.LocalAuthRepository
import com.focal.app.data.LocalFocalRepository
import com.focal.app.pairing.DefaultPairingRepository
import com.focal.app.pairing.PairingRepository

object AppInitializer {
    fun init(context: Context) {
        AuthRepository.install(LocalAuthRepository())
        FocalRepository.install(LocalFocalRepository.instance)
        PairingRepository.install(DefaultPairingRepository.instance)
    }
}
