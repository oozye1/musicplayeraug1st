package com.mardous.booming.extensions

import android.app.Application
import com.mardous.booming.App

val appContext: App
    get() = App.appContext

val appInstance: Application
    get() = App.appContext

