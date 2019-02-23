package com.jafca.hsp

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "smart") {
                val switchPreference = findPreference<Preference>("smart") as SwitchPreference
                switchPreference.isChecked = prefs.getBoolean("smart", true)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }
}