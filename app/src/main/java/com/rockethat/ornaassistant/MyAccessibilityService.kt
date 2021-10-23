package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class MyAccessibilityService() : AccessibilityService() {

    private val TAG = "OrnaAssist"
    private var mDebugDepth = 0

    var lastEvent: Long = 0
    var getChildCalls = 0
    var state: MainState? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        state = MainState(
            getSystemService(WINDOW_SERVICE) as WindowManager, applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.kg_layout, null),
            inflater.inflate(R.layout.assess_layout, null),
            this
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (p0 == null/* || p0.packageName == null || !p0.packageName.contains("orna")*/) {
            return
        } else if (p0.source == null) {
            return
        }
        /*val sinceLast = System.currentTimeMillis() - lastEvent
        if (sinceLast > 0) {
            //Log.v(TAG, "Event: ${p0?.eventType}, ${sinceLast}")
        } else {
            Log.v(
                TAG,
                "DISCARD Event: ${p0?.eventType}, ${sinceLast} ms / ${p0.source.childCount} / ${p0.source.parent.childCount}"
            )
            return
        }*/

        if (p0.packageName.toString()
                .contains("discord") && p0.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED
        ) {
            return
        }

        mDebugDepth = 0

        var values = ArrayList<ScreenData>()
        var mNodeInfo: AccessibilityNodeInfo? = p0.source

        if (p0?.source != null) {
            getChildCalls = 0
            parseScreen(mNodeInfo, values, 0, 0)
        }
        /*if (dur > 0 && p0.source.childCount > 0) {
            val parchilds = if (p0.source.parent != null) p0.source.parent.childCount else 0
            Log.v(
                TAG,
                "${p0.eventType} Parsing took $dur ms for ${values.size} / ${p0.source.childCount} / ${parchilds} items, ${if (p0.source.childCount > 0) dur / p0.source.childCount else 0} ms per item"
            )
        }*/

        state?.processData(p0.packageName.toString(), values)

        lastEvent = System.currentTimeMillis()
        /*
        var valueNames = ArrayList<String>()
        values.forEach {
            valueNames.add("${it.name}")
        }

        Log.v(
            TAG,
            "${p0.eventType} ${System.currentTimeMillis() - start} ms: getChildCalls $getChildCalls, $valueNames"
        )*/
    }

    private fun parseScreen(
        mNodeInfo: AccessibilityNodeInfo?,
        data: ArrayList<ScreenData>?,
        depth: Int,
        time: Long
    ): Boolean {
        var done = false
        if (mNodeInfo == null) return done
        if (depth > 250) return true

        //Log.d(TAG, "$text #${mNodeInfo.text}#")
        if (mNodeInfo.text != null) {
            when (mNodeInfo.text.toString()) {
                "DROP" -> done = true
                "New" -> done = true // Inventory
                "SEND TO KEEP" -> done = true // Inventory
                "Map" -> done = true
                //"Character" -> done = true
            }
            val rect = Rect()
            mNodeInfo.getBoundsInScreen(rect)
            data?.add(ScreenData(mNodeInfo.text.toString(), rect, time, mDebugDepth, mNodeInfo))
        }

        if (!done) {
            val count = mNodeInfo.childCount
            for (i in 0 until count) {
                getChildCalls++
                var child: AccessibilityNodeInfo?
                val thistime = measureTimeMillis { child = mNodeInfo.getChild(i) }
                if (child != null) {
                    mDebugDepth++;
                    done = parseScreen(child, data, depth + i, thistime)
                    if (done) {
                        break
                    }
                    mDebugDepth--;
                }
            }
        }

        mNodeInfo.recycle()

        return done
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onServiceConnected() {
        super.onServiceConnected()

        var info = this.serviceInfo

        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.notificationTimeout = 500
        //info.interactiveUiTimeoutMillis = 1
        info.packageNames = listOf(
            "playorna.com.orna",
            "com.discord",
            "com.rockethat.ornaassistant"
        ).toTypedArray()
        this.serviceInfo = info

        Log.i(TAG, "onServiceConnected called")

    }
}