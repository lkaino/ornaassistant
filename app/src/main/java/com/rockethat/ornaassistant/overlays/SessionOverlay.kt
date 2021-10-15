package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.DungeonVisit
import com.rockethat.ornaassistant.KingdomMember
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.WayvesselSession
import com.rockethat.ornaassistant.db.DungeonVisitDatabaseHelper
import com.rockethat.ornaassistant.db.WayvesselSessionDatabaseHelper
import com.rockethat.ornaassistant.viewadapters.KGAdapter
import com.rockethat.ornaassistant.viewadapters.KGItem
import com.rockethat.ornaassistant.viewadapters.NotificationsAdapter
import com.rockethat.ornaassistant.viewadapters.NotificationsItem
import java.time.Duration
import java.time.LocalDateTime

class SessionOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mSessionGoldTv = mView.findViewById<TextView>(R.id.tvGoldSession)
    var mSessionOrnTv = mView.findViewById<TextView>(R.id.tvOrnsSession)
    var mDungeonGoldTv = mView.findViewById<TextView>(R.id.tvGoldDungeon)
    var mDungeonOrnTv = mView.findViewById<TextView>(R.id.tvOrnsDungeon)


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun show() {
        if (mVisible.compareAndSet(false, true)) {
            if (mView.parent == null) {
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
                paramFloat.x = 0
                paramFloat.y = 470
                Handler(Looper.getMainLooper()).post {
                    mWM.addView(mView, paramFloat)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(session: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        if (session != null) {
            mSessionGoldTv.text = createSessionGoldOrnNumberString(session.gold)
            mSessionOrnTv.text = createSessionGoldOrnNumberString(session.orns)
        } else {
            mSessionGoldTv.text = "0"
            mSessionOrnTv.text = "0"
        }

        if (dungeonVisit != null) {
            mDungeonGoldTv.text = createSessionGoldOrnNumberString(dungeonVisit.gold)
            mDungeonOrnTv.text = createSessionGoldOrnNumberString(dungeonVisit.orns)
        } else {
            if (session == null) {
                mDungeonGoldTv.text = "0"
                mDungeonOrnTv.text = "0"
            } else {
                // Keep the last dungeon data when we are in session
            }
        }
        show()
    }

    private fun createSessionGoldOrnNumberString(value: Long): String {
        if (value > 1000000) {
            return "%.1f m".format(value.toFloat() / 1000000)
        } else if (value > 1000) {
            return "%.1f k".format(value.toFloat() / 1000)
        } else {
            return "$value"
        }
    }
}