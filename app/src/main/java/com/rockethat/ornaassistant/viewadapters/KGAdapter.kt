package com.rockethat.ornaassistant.viewadapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.R

class KGAdapter(
    private val mItems: List<KGItem>,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<KGAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View, private val clickListener: () -> Unit) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val nameText = itemView.findViewById<TextView>(R.id.kgName)
        val floorsText = itemView.findViewById<TextView>(R.id.kgFloors)
        val immunityText = itemView.findViewById<TextView>(R.id.kgImmunity)
        val sleeptimeText = itemView.findViewById<TextView>(R.id.kgSleepTime)
        val seenCountText = itemView.findViewById<TextView>(R.id.kgSeenCount)
        val localTimeText = itemView.findViewById<TextView>(R.id.kgLocalTime)

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
            inflater.inflate(R.layout.kg_rv_layout, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView, clickListener)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val item: KGItem = mItems[position]

        val textViews = listOf(
            holder.nameText,
            holder.floorsText,
            holder.sleeptimeText,
            holder.immunityText,
            holder.seenCountText,
            holder.localTimeText,
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
        // Set item views based on your views and data model
        holder.nameText.text = item.name
        holder.floorsText.text = item.floors
        holder.sleeptimeText.text = item.sleeptime
        holder.immunityText.text = item.immunity
        holder.sleeptimeText.text = item.sleeptime
        holder.seenCountText.text = item.seenCount
        holder.localTimeText.text = item.localTime

        if (position > 0 && item.zerk) {
            holder.nameText.setTextColor(Color.RED)
        } else {
            holder.nameText.setTextColor(Color.WHITE)
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mItems.size
    }
}