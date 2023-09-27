package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.view.WindowManager
import com.rockethat.ornaassistant.OrnaView
import com.rockethat.ornaassistant.OrnaViewType
import com.rockethat.ornaassistant.ScreenData

class OrnaViewInventory(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.INVENTORY, wm, ctx) {
}