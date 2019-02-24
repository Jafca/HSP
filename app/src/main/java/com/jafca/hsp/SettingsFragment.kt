package com.jafca.hsp

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private inner class NumericKeyboardMethod : PasswordTransformationMethod() {
        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            return source
        }
    }

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "smart") {
                val switchPreference = findPreference<Preference>("smart") as SwitchPreference
                switchPreference.isChecked = prefs.getBoolean("smart", true)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        val defPrefs = PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
        defPrefs.registerOnSharedPreferenceChangeListener(listener)

        val speedEditTextPreference = findPreference<Preference>(getString(R.string.pref_speed)) as EditTextPreference
        speedEditTextPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            editText.transformationMethod = NumericKeyboardMethod()
            editText.selectAll()
        }

        speedEditTextPreference.parent?.title =
            "Walking Speed (currently ${defPrefs.getString(getString(R.string.pref_speed), "5")} kph)"

        val calcSpeedButton = findPreference<Preference>("pref_calcSpeed")
        calcSpeedButton.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context!!)
            fusedLocationClient.lastLocation.addOnSuccessListener(
                Executors.newSingleThreadExecutor(),
                OnSuccessListener { location: Location ->
                    with(defPrefs.edit()) {
                        putString(getString(R.string.pref_speed), location.speed.toString())
                        apply()
                    }
                })
            true
        }
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this.activity!!.applicationContext)
            .registerOnSharedPreferenceChangeListener(listener)
    }
}