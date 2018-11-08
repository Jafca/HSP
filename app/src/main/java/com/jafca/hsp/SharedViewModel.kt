package com.jafca.hsp

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val reminderTime = MutableLiveData<Pair<Int, Int>>()

    fun timeSelected(picked: Pair<Int, Int>) {
        this.reminderTime.value = picked
    }
}