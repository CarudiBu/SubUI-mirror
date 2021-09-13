package com.carudibu.android.subuimirror

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.DialogInterface
import android.app.Presentation
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.Log
import java.lang.Exception
import java.lang.reflect.Field
import android.view.*
import android.content.Intent
import android.app.ActivityOptions
import android.content.Context
import android.util.DisplayMetrics

import android.media.projection.MediaProjection

import android.print.PrintAttributes.Resolution

import android.media.projection.MediaProjectionManager

import android.view.SurfaceView
import android.hardware.display.VirtualDisplay
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial


class MainActivity : AppCompatActivity() {
    private val mAttachedLcdSize: Point = Point()
    private var mPresentationDialog: Presentation? = null

    private val PERMISSION_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start).setOnClickListener {
            var projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            //startPresentation()
            startActivityForResult(
                projectionManager!!.createScreenCaptureIntent(),
                PERMISSION_CODE);
        }

        findViewById<Button>(R.id.stop).setOnClickListener {
            val serviceIntent = Intent(this, MirrorService::class.java)
            stopService(serviceIntent)
        }

        findViewById<SwitchMaterial>(R.id.crop).setOnCheckedChangeListener { buttonView, isChecked ->
            val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putBoolean("crop", isChecked)
                apply()
            }
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

    private fun startPresentation() {

        Log.v("subuimirror", "connectSubLcd")
        val subDisplay: Display? = getSubDisplay()
        if (subDisplay == null) {
            Log.e("subuimirror", "No subscreen found")
            return
        }
        if (mPresentationDialog == null) {
            mPresentationDialog = CoverPresentation(this, subDisplay)
        }
        mPresentationDialog!!.setOnKeyListener { dialogInterface, i, keyEvent ->
            connectSubLcd(dialogInterface, i, keyEvent)
        }
        setWindowAttributes()
        try {
            mPresentationDialog!!.show()
        } catch (unused: WindowManager.InvalidDisplayException) {
            Log.w("subuimirror", "Display was removed")
            mPresentationDialog = null
        }
    }

    private fun stopPresentation(){
        mPresentationDialog?.cancel()
    }

    private fun getSubDisplay(): Display? {
        val displays: Array<Display> =
            (getSystemService("display") as DisplayManager).getDisplays("com.samsung.android.hardware.display.category.BUILTIN")
        Log.d("subuimirror", "builtInDisplays size : " + displays.size)
        val display: Display? = if (displays.size > 1) displays[1] else null
        if (display != null) {
            display.getRealSize(this.mAttachedLcdSize)
        }
        return display
    }

    private fun setWindowAttributes() {
        mPresentationDialog!!.window!!.clearFlags(8)
        mPresentationDialog!!.window!!.addFlags(2097152)
        mPresentationDialog!!.window!!.addFlags(128)
        mPresentationDialog!!.window!!.addFlags(67108864)
        mPresentationDialog!!.window!!.addFlags(1024)
        val attributes = mPresentationDialog!!.window!!.attributes
        try {
            val field: Field = attributes.javaClass.getField("layoutInDisplayCutoutMode")
            field.setAccessible(true)
            field.setInt(attributes, 1)
            mPresentationDialog!!.window!!.attributes = attributes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun connectSubLcd(
        dialogInterface: DialogInterface?,
        i: Int,
        keyEvent: KeyEvent
    ): Boolean {
        if (keyEvent.getAction() === 0) {
            return onKeyDown(i, keyEvent)
        }
        return if (keyEvent.getAction() === 1) {
            onKeyUp(i, keyEvent)
        } else false
    }


}