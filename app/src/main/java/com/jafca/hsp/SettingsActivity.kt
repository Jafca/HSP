package com.jafca.hsp

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.settingsToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().add(R.id.settings_container, SettingsFragment()).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settingsmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_reset -> {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setTitle("Reset Settings")
            builder.setMessage(getString(R.string.reset_settings_text))

            builder.setPositiveButton("YES") { _, _ ->
                with(PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()) {
                    putBoolean(getString(R.string.pref_smart), true)
                    putString(getString(R.string.pref_speed), "5.0")
                    putBoolean(getString(R.string.pref_directDistance), true)
                    putBoolean(getString(R.string.pref_sampleData), true)
                    putBoolean(getString(R.string.pref_detectParking), true)
                    apply()
                }
            }
            builder.setNegativeButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}