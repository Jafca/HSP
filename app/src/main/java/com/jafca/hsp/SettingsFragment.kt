package com.jafca.hsp

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.preference.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import java.util.concurrent.Executors

class SettingsFragment : PreferenceFragmentCompat() {
    private inner class NumericKeyboardMethod : PasswordTransformationMethod() {
        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            return source
        }
    }

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                getString(R.string.pref_smart) -> {
                    val switchPreference =
                        findPreference<Preference>(getString(R.string.pref_smart)) as SwitchPreference
                    switchPreference.isChecked = prefs.getBoolean(getString(R.string.pref_smart), true)
                }
                getString(R.string.pref_speed) -> {
                    var speed = prefs.getString(getString(R.string.pref_speed), "")
                    if (speed.toFloatOrNull() == null) {
                        speed = "5"
                    } else if (speed.toFloat() < 0.1f) {
                        speed = "0.1"
                    }
                    prefs.edit().putString(getString(R.string.pref_speed), speed).apply()

                    val speedEditTextPreference =
                        findPreference<Preference>(getString(R.string.pref_speed)) as EditTextPreference
                    speedEditTextPreference.text = speed
                    speedEditTextPreference.parent?.summary = "Walking speed = $speed kph"
                }
                getString(R.string.pref_directDistance) -> {
                    val switchPreference =
                        findPreference<Preference>(getString(R.string.pref_directDistance)) as SwitchPreference
                    switchPreference.isChecked = prefs.getBoolean(getString(R.string.pref_directDistance), true)
                }
                getString(R.string.pref_sampleData) -> {
                    val switchPreference =
                        findPreference<Preference>(getString(R.string.pref_sampleData)) as SwitchPreference
                    switchPreference.isChecked = prefs.getBoolean(getString(R.string.pref_sampleData), true)
                }
                getString(R.string.pref_detectParking) -> {
                    val switchPreference =
                        findPreference<Preference>(getString(R.string.pref_detectParking)) as SwitchPreference
                    switchPreference.isChecked = prefs.getBoolean(getString(R.string.pref_detectParking), true)
                }
            }
        }

    @SuppressLint("MissingPermission")
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

        speedEditTextPreference.parent?.summary =
            "Walking speed = ${defPrefs.getString(getString(R.string.pref_speed), "5")} kph"

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