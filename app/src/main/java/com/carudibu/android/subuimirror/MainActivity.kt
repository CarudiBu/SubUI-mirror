package com.carudibu.android.subuimirror

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial


class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start).setOnClickListener {
            var projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            startActivityForResult(
                projectionManager!!.createScreenCaptureIntent(),
                PERMISSION_CODE);
        }

        findViewById<Button>(R.id.stop).setOnClickListener {
            val serviceIntent = Intent(this, MirrorService::class.java)
            stopService(serviceIntent)
        }

        // TODO: FIX ROTATION ISSUES (COVER DOESN'T BEHAVE AS EXPECTED WHEN PHONE ROTATES, GIVE OPTION?)

        findViewById<SwitchMaterial>(R.id.crop).setOnCheckedChangeListener { buttonView, isChecked ->
            val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putBoolean("crop", isChecked)
                apply()
            }
        }

        findViewById<Button>(R.id.orientation).setOnClickListener {
            OrientationSettingsFragment().show(supportFragmentManager, "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PERMISSION_CODE) {
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, MirrorService::class.java)
        serviceIntent.putExtra("data", data)
        serviceIntent.putExtra("int", resultCode)
        startService(serviceIntent)
    }

}