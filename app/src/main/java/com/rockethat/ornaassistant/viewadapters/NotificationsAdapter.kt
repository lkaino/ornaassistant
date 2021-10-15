package com.rockethat.ornaassistant.viewadapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface

import android.view.LayoutInflater

import android.view.ViewGroup
import com.rockethat.ornaassistant.R

class NotificationsAdapter(
    private val mItems: List<NotificationsItem>,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View, private val clickListener: () -> Unit) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val inviterText = itemView.findViewById<TextView>(R.id.inviter)
        val nText = itemView.findViewById<TextView>(R.id.notificationN)
        val vogText = itemView.findViewById<TextView>(R.id.notificationVoG)
        val dText = itemView.findViewById<TextView>(R.id.notificationD)
        val bgText = itemView.findViewById<TextView>(R.id.notificationBG)
        val uwText = itemView.findViewById<TextView>(R.id.notificationUW)
        val cgText = itemView.findViewById<TextView>(R.id.notificationCG)
        val cooldownText = itemView.findViewById<TextView>(R.id.cooldown)

        override fun onClick(p0: View?) {
            clickListener()
        }
    }


    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val contactView: View =
            inflater.inflate(R.layout.notification_rv_layout, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView, clickListener)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val item: NotificationsItem = mItems[position]

        val textViews = listOf(
            holder.inviterText,
            holder.nText,
            holder.dText,
            holder.vogText,
            holder.bgText,
            holder.uwText,
            holder.cgText,
            holder.cooldownText,
        )

        var typeface = Typeface.NORMAL
        var color = Color.BLACK
        var alpha = 0
        if (position == 0) {
            typeface = Typeface.BOLD
            color = Color.DKGRAY
            alpha = 200
        }

        textViews.forEach {
            it.setTypeface(null, typeface)
            it.setBackgroundColor(color)
            it.background.alpha = alpha
            /*it.setOnClickListener {
                clickListener
            }*/
        }
        // Set item views based on your views and data model
        holder.inviterText.text = item.inviter
        holder.nText.text = item.N
        holder.dText.text = item.D
        holder.vogText.text = item.VoG
        holder.bgText.text = item.BG
        holder.uwText.text = item.UW
        holder.cgText.text = item.CG
        holder.cooldownText.text = item.cooldown

        if (item.statusBad) {
            holder.cooldownText.setTextColor(Color.RED)
        } else {
            holder.cooldownText.setTextColor(Color.WHITE)
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mItems.size
    }
}