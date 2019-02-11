package com.jafca.hsp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ParkedLocation::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ParkedLocationDatabase : RoomDatabase() {
    abstract fun parkedLocationDao(): ParkedLocationDao

    companion object {
        private var INSTANCE: ParkedLocationDatabase? = null

        fun getInstance(context: Context): ParkedLocationDatabase? {
            if (INSTANCE == null) {
                synchronized(ParkedLocationDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        ParkedLocationDatabase::class.java, "parkedLocation.db"
                    )
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