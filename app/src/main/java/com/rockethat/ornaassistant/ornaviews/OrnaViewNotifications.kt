package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.graphics.Rect
import android.view.WindowManager
import com.rockethat.ornaassistant.ScreenData
import java.util.ArrayList

class OrnaViewNotifications(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.NOTIFICATIONS, wm, ctx) {

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {

        var inviters = mutableMapOf<String, Rect>()
        var inviter: ScreenData? = null
        for (item in data) {
            if (item.name.contains("invited you")) {
                inviter = item
            } else if (inviter != null && item.name.lowercase().contains("accept")) {
                val inviterName = inviter.name.replace(" has invited you to their party.", "")
                inviters[inviterName] = inviter.position
            }
        }

        if (inviters.isNotEmpty()) {
            updateResults(mutableMapOf(OrnaViewUpdateType.NOTIFICATIONS_INVITERS to inviters))
        }

        return false
    }
}