package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.os.*
import android.os.HandlerThread


open class Overlay(
    val mWM: WindowManager,
    val mCtx: Context,
    val mView: View,
    val mWidth: Double
) {
    var mVisible = AtomicBoolean(false)
    private val mUIHandlerThread = HandlerThread("UIHandlerThread")
    var mUIRequestHandler: Handler

    object mPos {
        var x = 5
        var y = 5
        var startX = 0
        var startY = 0
        var eventStartX = 0
        var eventStartY = 0
        var moveEvents = 0
    }

    private var mParamFloat: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()
        gravity = Gravity.TOP or Gravity.LEFT
        x = mPos.x
        y = mPos.y
    }

    fun isVisible(): Boolean {
        return mVisible.get()
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    open fun show() {
        if (mVisible.compareAndSet(false, true)) {
            if (mView.parent == null) {
                mUIRequestHandler.post {
                    mWM.addView(mView, mParamFloat)
                }

                mView.setOnTouchListener(OnTouchListener { v, event ->
                    Log.i("OrnaOverlay", "touch $event!")
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        mPos.startX = mPos.x
                        mPos.startY = mPos.y
                        mPos.eventStartX = x
                        mPos.eventStartY = y
                        mPos.moveEvents = 0
                    }
                    if (event.action == MotionEvent.ACTION_MOVE) {
                        mPos.moveEvents++
                    }
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (mPos.moveEvents > 1) {
                            val eventX = mPos.startX + x - mPos.eventStartX
                            val eventY = mPos.startY + y - mPos.eventStartY
                            mPos.x = eventX
                            mPos.y = eventY
                            val params = mParamFloat
                            params.x = mPos.x
                            params.y = mPos.y
                            mWM.updateViewLayout(mView, params)
                        } else {
                            hide()
                        }
                    }
                    false
                })
            }
            mVisible.set(true)

            Log.i("OrnaOverlay", "SHOW DONE!")
        }
    }


    open fun hide() {
        if (mVisible.compareAndSet(true, false)) {
            Log.i("OrnaOverlay", "HIDE!")
            if (mView.parent != null) {
                mUIRequestHandler.post {
                    mWM.removeViewImmediate(mView)
                }
                mVisible.set(false)
            }
        }

    }

    init {
        mUIHandlerThread.start()
        mUIRequestHandler = Handler(mUIHandlerThread.looper)
    }
}