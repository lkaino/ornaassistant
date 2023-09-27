package com.rockethat.ornaassistant.ui.viewadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.R

class KingdomSeenAdapter(
    private val mItems: List<KingdomSeenItem>
) : RecyclerView.Adapter<KingdomSeenAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val nameText = itemView.findViewById<TextView>(R.id.kingdomLblName)
        val seenText = itemView.findViewById<TextView>(R.id.kingdomLblSeen)
        override fun onClick(p0: View?) {

        }
    }


    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fragment_kingdom_rv_layout, viewGroup, false)

        return ViewHolder(view)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val item: KingdomSeenItem = mItems[position]

        val textViews = listOf(
            holder.nameText,
            holder.seenText,
        )

        /*var typeface = Typeface.NORMAL
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
        }*/
        // Set item views based on your views and data model
        holder.nameText.text = item.name
        holder.seenText.text = item.seenCount
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mItems.size
    }
}