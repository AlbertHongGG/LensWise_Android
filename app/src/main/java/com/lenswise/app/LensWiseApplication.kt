package com.lenswise.app

import android.app.Application

class LensWiseApplication : Application() {
    companion object {
        lateinit var instance: LensWiseApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
