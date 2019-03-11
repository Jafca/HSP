package com.jafca.hsp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var icon: ImageView = itemView.findViewById(R.id.helpItemImageView)
    var description: TextView = itemView.findViewById(R.id.helpItemTextView)
}