package com.jafca.hsp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.*

@Entity(tableName = "parkedLocation")
data class ParkedLocation(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "lat") var lat: Double,
    @ColumnInfo(name = "lon") var lon: Double,
    @ColumnInfo(name = "datetime") var datetime: Date?,
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "starred") var starred: Boolean = false
) {
    fun getLatLng(): LatLng {
        return LatLng(lat, lon)
    }
}