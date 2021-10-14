package com.softradix.mapboxdemo.utils


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Window
import android.view.WindowManager
import com.softradix.mapboxdemo.R

object Utils {
    var mProgressDialog: Dialog? = null
    fun showInternetDialog(context: Context) {

        mProgressDialog = Dialog(context, android.R.style.Theme_Translucent)
        mProgressDialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        mProgressDialog?.setContentView(R.layout.dialog_internet)
        mProgressDialog?.setCancelable(false)
        mProgressDialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        mProgressDialog?.window?.statusBarColor = Color.parseColor("#80000000")
        mProgressDialog?.show()
    }

    fun hideInternetDialog() {
        if (mProgressDialog != null && mProgressDialog?.isShowing == true) {
            mProgressDialog?.dismiss()
        }
    }
}
