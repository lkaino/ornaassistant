package com.example.ornaassistant

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class ScreenData(
    val name: String, val position: Rect, val time: Long, val depth: Int, val mNodeInfo: AccessibilityNodeInfo?
)
