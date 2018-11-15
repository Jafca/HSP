package com.jafca.hsp

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

@Dao
interface ParkedLocationDao {

    @Query("SELECT * from parkedLocation")
    fun getAll(): List<ParkedLocation>

    @Insert(onConflict = REPLACE)
    fun insert(parkedLocation: ParkedLocation)

    @Update
    fun update(parkedLocation: ParkedLocation)

    @Query("DELETE from parkedLocation")
    fun deleteAll()
}