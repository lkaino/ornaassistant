package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.view.WindowManager
import com.rockethat.ornaassistant.OrnaView
import com.rockethat.ornaassistant.OrnaViewType
import com.rockethat.ornaassistant.ScreenData
import java.util.ArrayList

class OrnaViewWayvessel(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.WAYVESSEL, wm, ctx) {
}