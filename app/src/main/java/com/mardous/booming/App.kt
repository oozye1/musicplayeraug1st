package com.mardous.booming

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds

class App : Application() {

    companion object {
        lateinit var appContext: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        MobileAds.initialize(this)
    }
}
