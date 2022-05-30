package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.*

class SessionOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mGoldHeaderTv = mView.findViewById<TextView>(R.id.tvGold)
    var mSessionHeaderTv = mView.findViewById<TextView>(R.id.tvSessionHeader)
    var mSessionTv = mView.findViewById<TextView>(R.id.tvSession)
    var mSessionGoldTv = mView.findViewById<TextView>(R.id.tvGoldSession)
    var mSessionOrnTv = mView.findViewById<TextView>(R.id.tvOrnsSession)
    var mDungeonGoldTv = mView.findViewById<TextView>(R.id.tvGoldDungeon)
    var mDungeonOrnTv = mView.findViewById<TextView>(R.id.tvOrnsDungeon)


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun show() {
        super.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(session: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        mUIRequestHandler.post {

            if (session != null) {
                mSessionHeaderTv.text = "@${session.name}"
                if (session.mDungeonsVisited > 1) {
                    mSessionTv.text = "${session.mDungeonsVisited} dungeons"
                    if (dungeonVisit != null && dungeonVisit.mode.mMode == DungeonMode.Modes.ENDLESS) {
                        mSessionGoldTv.text = createSessionGoldOrnNumberString(session.experience)
                    } else {
                        mSessionGoldTv.text = createSessionGoldOrnNumberString(session.gold)
                    }
                    mSessionOrnTv.text = createSessionGoldOrnNumberString(session.orns)
                    mSessionTv.visibility = View.VISIBLE
                    mSessionGoldTv.visibility = View.VISIBLE
                    mSessionOrnTv.visibility = View.VISIBLE
                } else {
                    mSessionTv.visibility = View.GONE
                    mSessionGoldTv.visibility = View.GONE
                    mSessionOrnTv.visibility = View.GONE
                }
            } else {
                mSessionHeaderTv.text = ""
                mSessionGoldTv.text = "0"
                mSessionOrnTv.text = "0"
                mSessionTv.visibility = View.GONE
                mSessionGoldTv.visibility = View.GONE
                mSessionOrnTv.visibility = View.GONE
            }

            if (dungeonVisit != null) {
                if (dungeonVisit.mode.mMode == DungeonMode.Modes.ENDLESS) {
                    mGoldHeaderTv.text = "Exp"
                    mDungeonGoldTv.text = createSessionGoldOrnNumberString(dungeonVisit.experience)
                } else {
                    mGoldHeaderTv.text = "Gold"
                    mDungeonGoldTv.text = createSessionGoldOrnNumberString(dungeonVisit.gold)
                }
                mDungeonOrnTv.text = createSessionGoldOrnNumberString(dungeonVisit.orns)
            } else {
                if (session == null) {
                    mDungeonGoldTv.text = "0"
                    mDungeonOrnTv.text = "0"
                } else {
                    // Keep the last dungeon data when we are in session
                }
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

    init {
        mPos.x = 0
        mPos.y = 470
    }
}