package com.jafca.hsp

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "parkedLocation")
data class ParkedLocation(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "lat") var lat: Double,
    @ColumnInfo(name = "lon") var lon: Double,
    @ColumnInfo(name = "note") var note: String
) {
    constructor() : this(null, 0.0, 0.0, "")
    constructor(latLng: LatLng)
            : this(null, latLng.latitude, latLng.longitude, "")
}