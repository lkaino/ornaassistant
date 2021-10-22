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
import java.util.concurrent.locks.ReentrantLock


open class Overlay(
    val mWM: WindowManager,
    val mCtx: Context,
    val mView: View,
    val mWidth: Double
) {
    var mVisible = AtomicBoolean(false)
    val lock = ReentrantLock()
    private var mParamFloat: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()
        gravity = Gravity.TOP or Gravity.LEFT
        x = 5
        y = 5
    }

    fun isVisible(): Boolean {
        return mVisible.get()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    open fun show() {
        if (mVisible.compareAndSet(false, true)) {
            Log.i("OrnaOverlay", "SHOW!")

            Handler(Looper.getMainLooper()).post {
                if (mView.parent == null) {
                    mWM.addView(mView, mParamFloat)
                }
                mVisible.set(true)
            }
            Log.i("OrnaOverlay", "SHOW DONE!")
        }
    }


    open fun hide() {
        if (mVisible.compareAndSet(true, false)) {
            Log.i("OrnaOverlay", "HIDE!")
            Handler(Looper.getMainLooper()).post {
                if (mView.parent != null) {
                    mWM.removeViewImmediate(mView)
                    mVisible.set(false)
                }
            }
        }

    }
}