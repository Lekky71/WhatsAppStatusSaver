package com.hashcode.whatsstatussaver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Switch
import android.widget.Toast
import com.hashcode.whatsstatussaver.floatingbutton.FloatingButtonService

class SettingsActivity : AppCompatActivity() {
    private val CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084

    lateinit var floatingSwitch: Switch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        floatingSwitch = findViewById(R.id.allow_float_switch)
        startFloating()
        val intent = Intent(this@SettingsActivity, FloatingButtonService::class.java)
        val sharedPref = getSharedPreferences("settings-pref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val preAllowed = sharedPref.getBoolean("floating-allowed", false)
        floatingSwitch.isChecked = preAllowed
        floatingSwitch.setOnCheckedChangeListener({ button, value ->
            editor.putBoolean("floating-allowed", value)
            editor.apply()
            if (!value) stopService(intent)
            else startService(intent)
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    fun startFloating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Toast.makeText(this, "You to allow this app to draw over other apps in order" +
                    " for the floating circle to work", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName))
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
//            initializeView()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == Activity.RESULT_OK) {
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this@SettingsActivity, MainActivity::class.java)
                startActivity(mainIntent)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
