package com.rockethat.ornaassistant.viewadapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log

import android.view.LayoutInflater

import android.view.ViewGroup
import com.rockethat.ornaassistant.R

class AssessAdapter(
    private val mItems: List<AssessItem>,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<AssessAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access


    inner class ViewHolder(itemView: View, private val clickListener: () -> Unit) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val cols = mutableListOf<TextView>()
        init {
            itemView.setOnClickListener(this)
            cols.clear()
            cols.addAll(
                listOf(
                    itemView.findViewById(R.id.lblAssess1),
                    itemView.findViewById(R.id.lblAssess2),
                    itemView.findViewById(R.id.lblAssess3),
                    itemView.findViewById(R.id.lblAssess4),
                    itemView.findViewById(R.id.lblAssess5),
                    itemView.findViewById(R.id.lblAssess6),
                    itemView.findViewById(R.id.lblAssess7),
                    itemView.findViewById(R.id.lblAssess8)
                )
            )
        }

        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
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
            inflater.inflate(R.layout.assess_rv_layout, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView, clickListener)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val item: AssessItem = mItems[position]

        val typeface = Typeface.NORMAL
        val bgColor = Color.BLACK
        val bgAlpha = 0
        val typefaceHdr = Typeface.BOLD
        val bgColorHdr = Color.DKGRAY
        val bgAlphaHdr = 200

        for (i in 0 until holder.cols.size) {
            var header = false
            if (position == 0) {
                if (i > 0) {
                    header = true
                }
            } else {
                if (i == 0) {
                    header = true
                }
            }

            if (header) {
                holder.cols[i].setTypeface(null, typefaceHdr)
                holder.cols[i].setBackgroundColor(bgColorHdr)
                holder.cols[i].background.alpha = bgAlphaHdr
            } else {
                holder.cols[i].setTypeface(null, typeface)
                holder.cols[i].setBackgroundColor(bgColor)
                holder.cols[i].background.alpha = bgAlpha
            }
        }

        // Set item views based on your views and data model
        var i = 0
        for (col in item.cols) {
            if (i <  holder.cols.size) {
                holder.cols[i].text = col
                holder.cols[i].visibility = View.VISIBLE
            }
            i++
        }

        while (i <  holder.cols.size) {
            holder.cols[i].visibility = View.GONE
            i++
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mItems.size
    }
}