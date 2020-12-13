package com.mapapp

import android.app.Application
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider

class MainApplication : Application(), KodeinAware {


    override val kodein = Kodein.lazy {
        import(androidXModule(this@MainApplication))

        bind() from provider { ViewModelFactory() }
    }

}