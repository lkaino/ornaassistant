package com.rockethat

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

import android.view.MotionEvent
import android.view.View


class MultiClickRecyclerView : RecyclerView {
    var mTouchListener: View? = null
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
    }

    fun setTouchListener(view: View)
    {
        mTouchListener = view
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        var consumed = false

        return consumed
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN && this.scrollState == SCROLL_STATE_SETTLING) {
            stopScroll()
        }

        return true
    }
}