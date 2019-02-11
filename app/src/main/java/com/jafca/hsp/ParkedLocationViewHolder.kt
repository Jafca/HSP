package com.jafca.hsp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParkedLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var location: TextView = itemView.findViewById(R.id.locationTextView)
    var distanceDateTime: TextView = itemView.findViewById(R.id.distanceDateTimeTextView)
    var note: TextView = itemView.findViewById(R.id.noteTextView)
    var editNote: ImageView = itemView.findViewById(R.id.editNoteButton)
    var star: ImageView = itemView.findViewById(R.id.starButton)
}