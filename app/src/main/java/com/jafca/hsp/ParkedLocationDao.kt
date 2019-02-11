package com.jafca.hsp

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface ParkedLocationDao {
    @Query("SELECT * FROM parkedLocation ORDER BY datetime DESC")
    fun getAllLive(): LiveData<List<ParkedLocation>>

    @Query("SELECT * FROM parkedLocation")
    fun getAll(): List<ParkedLocation>

    @Insert(onConflict = REPLACE)
    fun insert(parkedLocation: ParkedLocation)

    @Update
    fun update(parkedLocation: ParkedLocation)

    @Delete
    fun delete(parkedLocation: ParkedLocation)

    @Query("DELETE FROM parkedLocation")
    fun deleteAll()

    @Query("DELETE FROM parkedLocation WHERE starred = 0")
    fun deleteNonStarred()
}