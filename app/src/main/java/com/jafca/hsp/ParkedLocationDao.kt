package com.jafca.hsp

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface ParkedLocationDao {

    @Query("SELECT * from parkedLocation")
    fun getAll(): List<ParkedLocation>

    @Insert(onConflict = REPLACE)
    fun insert(parkedLocation: ParkedLocation)

    @Update
    fun update(parkedLocation: ParkedLocation)

    @Delete
    fun delete(parkedLocation: ParkedLocation)

    @Query("DELETE from parkedLocation")
    fun deleteAll()
}