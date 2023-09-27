package com.rockethat.ornaassistant.viewadapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.R

class NotificationsAdapter(
    private val items: List<NotificationsItem>,
    private val itemClickListener: () -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        val inviterText: TextView = itemView.findViewById(R.id.inviter)
        val nText: TextView = itemView.findViewById(R.id.notificationN)
        val vogText: TextView = itemView.findViewById(R.id.notificationVoG)
        val dText: TextView = itemView.findViewById(R.id.notificationD)
        val bgText: TextView = itemView.findViewById(R.id.notificationBG)
        val uwText: TextView = itemView.findViewById(R.id.notificationUW)
        val cgText: TextView = itemView.findViewById(R.id.notificationCG)
        val cooldownText: TextView = itemView.findViewById(R.id.cooldown)

        override fun onClick(view: View?) {
            itemClickListener()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.notification_rv_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val textViews = listOf(
            holder.inviterText,
            holder.nText,
            holder.dText,
            holder.vogText,
            holder.bgText,
            holder.uwText,
            holder.cgText,
            holder.cooldownText
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
        }

        holder.inviterText.text = item.inviter
        holder.nText.text = item.N
        holder.dText.text = item.D
        holder.vogText.text = item.VoG
        holder.bgText.text = item.BG
        holder.uwText.text = item.UW
        holder.cgText.text = item.CG
        holder.cooldownText.text = item.cooldown

        holder.cooldownText.setTextColor(if (item.statusBad) Color.RED else Color.WHITE)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
