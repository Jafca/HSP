package com.jafca.hsp

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query

@Dao
interface ParkedLocationDao {

    @Query("SELECT * from parkedLocation")
    fun getAll(): List<ParkedLocation>

    @Insert(onConflict = REPLACE)
    fun insert(parkedLocation: ParkedLocation)

    @Query("DELETE from parkedLocation")
    fun deleteAll()
}