package solution.com.googlemap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager


class ProviderReceiver : BroadcastReceiver() {
    companion object {
        val Location_Update = "Location_Update"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.apply {
            if (action == "android.location.PROVIDERS_CHANGED")
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Location_Update))
        }
    }
}