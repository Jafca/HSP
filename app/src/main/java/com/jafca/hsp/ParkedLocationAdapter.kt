package com.jafca.hsp

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat

class ParkedLocationAdapter(
    private var context: Context,
    private var parkedLocations: List<ParkedLocation>,
    private var currentLatLng: LatLng,
    private var historyListener: HistoryListener
) :
    RecyclerView.Adapter<ParkedLocationViewHolder>() {

    interface HistoryListener {
        fun update(parkedLocation: ParkedLocation)
        fun delete(parkedLocation: ParkedLocation)
    }

    override fun getItemCount(): Int {
        return parkedLocations.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkedLocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.history_item,
            parent, false
        )
        return ParkedLocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkedLocationViewHolder, position: Int) {
        holder.location.text = parkedLocations[position].getLatLng().toString()
        // TODO: Geo-decode LatLng to address

        var dateString = "N/A"
        val date = parkedLocations[position].datetime
        if (date != null) {
            val format = SimpleDateFormat("hh:mm a  dd MMM yyyy")
            dateString = format.format(date)
        }
        val distance = FloatArray(1)
        Location.distanceBetween(
            currentLatLng.latitude,
            currentLatLng.longitude,
            parkedLocations[position].lat,
            parkedLocations[position].lon,
            distance
        )
        dateString += "\t\t${"%.2f".format(distance[0] / 1000)}km"
        holder.distanceDateTime.text = dateString

        holder.note.text = parkedLocations[position].note

        if (parkedLocations[position].starred) {
            holder.star.setImageResource(R.drawable.star)
            holder.star.tag = "star"
        } else {
            holder.star.setImageResource(R.drawable.star_border)
            holder.star.tag = "unstar"
        }

        holder.star.setOnClickListener {
            if (holder.star.tag == "star") {
                holder.star.setImageResource(R.drawable.star_border)
                holder.star.tag = "unstar"
                parkedLocations[position].starred = false
            } else {
                holder.star.setImageResource(R.drawable.star)
                holder.star.tag = "star"
                parkedLocations[position].starred = true
            }
            updateParkedLocation(parkedLocations[position])
        }

        holder.editNote.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val inflater = LayoutInflater.from(context)
            builder.setTitle("Parking Note")
            val dialogLayout = inflater.inflate(R.layout.alert_dialog_note, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editNoteText)
            editText.setText(parkedLocations[position].note)
            builder.setView(dialogLayout)

            builder.setPositiveButton("SAVE") { _, _ ->
                parkedLocations[position].note = editText.text.toString()
                updateParkedLocation(parkedLocations[position])
            }

            builder.setNegativeButton("CANCEL") { _, _ -> }
            builder.show()
        }

        // TODO: Send back location on click as activity result and show on map as yellow with a route to it
    }

    fun setData(newData: List<ParkedLocation>) {
        parkedLocations = newData
        notifyDataSetChanged()
    }

    private fun updateParkedLocation(parkedLocation: ParkedLocation) {
        historyListener.update(parkedLocation)
    }

    fun deleteParkedLocation(position: Int) {
        historyListener.delete(parkedLocations[position])
        notifyItemRemoved(position)
    }
}