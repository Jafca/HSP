package com.jafca.hsp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HelpAdapter(private var helpItems: List<Pair<Int, String>>) : RecyclerView.Adapter<HelpViewHolder>() {
    override fun getItemCount(): Int {
        return helpItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.help_item,
            parent, false
        )
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        holder.icon.setImageResource(helpItems[position].first)
        holder.description.text = helpItems[position].second
    }
}