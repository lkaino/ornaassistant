package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.view.WindowManager
import com.rockethat.ornaassistant.ScreenData
import java.util.ArrayList

class OrnaViewInventory(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaView(OrnaViewType.INVENTORY, wm, ctx) {
}