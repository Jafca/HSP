package com.jafca.hsp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val reminderTime = MutableLiveData<Pair<Int, Int>>()

    fun timeSelected(picked: Pair<Int, Int>) {
        this.reminderTime.value = picked
    }
}