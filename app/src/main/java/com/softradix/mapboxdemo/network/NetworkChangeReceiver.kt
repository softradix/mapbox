package com.softradix.mapboxdemo.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.softradix.mapboxdemo.utils.NetworkUtil.getConnectivityStatusString
import com.softradix.mapboxdemo.utils.Utils.hideInternetDialog
import com.softradix.mapboxdemo.utils.Utils.showInternetDialog

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = getConnectivityStatusString(context)
        if (status == "3") {
            showInternetDialog(context)
        } else {
            hideInternetDialog()
        }
    }
}