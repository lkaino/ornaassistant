package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean


open class Overlay(
    val mWM: WindowManager,
    val mCtx: Context,
    val mView: View,
    val mWidth: Double
) {
    var mVisible = AtomicBoolean(false)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    open fun show() {
        if (mVisible.compareAndSet(false, true)) {
            if (mView.parent == null) {
                Log.i("OrnaOverlay", "SHOW!")
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
                paramFloat.x = 5
                paramFloat.y = 5

                Handler(Looper.getMainLooper()).post {
                    mWM.addView(mView, paramFloat)
                }
                Log.i("OrnaOverlay", "SHOW DONE!")
            }
        }
    }


    open fun hide() {
        if (mVisible.compareAndSet(true, false)) {
            Log.i("OrnaOverlay", "HIDE!")
            Handler(Looper.getMainLooper()).post {
                if (mView.parent != null) {
                    mWM.removeViewImmediate(mView)
                }
            }
        }

    }
}