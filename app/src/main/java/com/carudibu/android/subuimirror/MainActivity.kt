package com.carudibu.android.subuimirror

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.content.DialogInterface

import android.app.Presentation
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.WindowManager.InvalidDisplayException
import java.lang.Exception
import java.lang.reflect.Field
import android.view.*


class MainActivity : AppCompatActivity() {
    private val mAttachedLcdSize: Point = Point()
    private var mPresentationDialog: Presentation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        } catch (unused: InvalidDisplayException) {
            Log.w("subuimirror", "Display was removed")
            mPresentationDialog = null
        }

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