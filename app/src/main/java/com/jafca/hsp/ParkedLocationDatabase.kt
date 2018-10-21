package com.jafca.hsp

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [ParkedLocation::class], version = 1)
abstract class ParkedLocationDatabase : RoomDatabase() {

    abstract fun parkedLocationDao(): ParkedLocationDao

    companion object {
        private var INSTANCE: ParkedLocationDatabase? = null

        fun getInstance(context: Context): ParkedLocationDatabase? {
            if (INSTANCE == null) {
                synchronized(ParkedLocationDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        ParkedLocationDatabase::class.java, "parkedLocation.db")
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}