package com.hashcode.whatsstatussaver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import com.hashcode.whatsstatussaver.data.StatusSavingService
import com.hashcode.whatsstatussaver.floatingbutton.FloatingButtonService

class SettingsActivity : AppCompatActivity() {
    lateinit var floatingSwitch : Switch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        floatingSwitch = findViewById(R.id.allow_float_switch)
        val intent = Intent(this@SettingsActivity,FloatingButtonService::class.java)
        val sharedPref = getSharedPreferences("settings-pref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val preAllowed = sharedPref.getBoolean("floating-allowed", false)
        floatingSwitch.isChecked = preAllowed
        floatingSwitch.setOnCheckedChangeListener({ button, value ->
            editor.putBoolean("floating-allowed",value)
            editor.apply()
            if(!value) stopService(intent)

            else startService(intent)


        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
