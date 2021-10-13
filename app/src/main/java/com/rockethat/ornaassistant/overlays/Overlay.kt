package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.OrnaViewUpdateType
import com.rockethat.ornaassistant.ScreenData
import java.util.ArrayList

open class Overlay(
    val mWM: WindowManager,
    val mCtx: Context,
    val mView: View,
    val mWidth: Double
) {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    open fun show() {
        if (mView.parent == null) {
            val flags =
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val paramFloat = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            paramFloat.width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()

            paramFloat.gravity = Gravity.TOP or Gravity.LEFT
            paramFloat.x = 0
            paramFloat.y = 0
            mWM.addView(mView, paramFloat)
        }
    }

    open fun hide() {
        if (mView.parent != null) {
            mWM.removeViewImmediate(mView)
        }
    }
}