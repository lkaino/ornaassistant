package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.util.ArrayList
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
class Battle(private val mAS: AccessibilityService) {
    private val TAG = "OrnaBattle"
    private var mLastClick = LocalDateTime.now()

    fun update(data: ArrayList<ScreenData>) {
    }
}
