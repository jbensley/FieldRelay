package us.bensley.fieldrelay

import android.app.Application
import us.bensley.fieldrelay.di.ServiceLocator

class FieldRelayApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        us.bensley.fieldrelay.di.ServiceLocator.init(this)
    }
}
