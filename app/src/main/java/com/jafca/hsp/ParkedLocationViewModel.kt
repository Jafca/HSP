package com.jafca.hsp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ParkedLocationViewModel(application: Application) : AndroidViewModel(application) {
    private val parkedLocationDao: ParkedLocationDao =
        ParkedLocationDatabase.getInstance(application)!!.parkedLocationDao()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    fun getAllParkedLocations(): LiveData<List<ParkedLocation>> {
        return parkedLocationDao.getAllLive()
    }

    fun deleteParkedLocation(parkedLocation: ParkedLocation) {
        executorService.execute { parkedLocationDao.delete(parkedLocation) }
    }

    fun updateParkedLocation(parkedLocation: ParkedLocation) {
        executorService.execute { parkedLocationDao.update(parkedLocation) }
    }
}