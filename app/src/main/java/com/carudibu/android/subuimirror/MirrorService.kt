package com.carudibu.android.subuimirror

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.widget.Toast
import android.view.*
import androidx.core.app.NotificationCompat
import java.lang.Exception
import java.lang.reflect.Field

import android.view.WindowManager
import android.hardware.SensorManager

import android.view.OrientationEventListener

class MirrorService: Service() {

    private var mProjectionManager: MediaProjectionManager? = null
    private var mScreenSharing = false
    private var mMediaProjection: MediaProjection? = null
    private var mPresentationDialog: Presentation? = null
    private var orientationEventListener: OrientationEventListener? = null

    private var sf: Int = 1

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        generateForegroundNotification()

        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()

        val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        sf = if(sharedPref.getBoolean("crop", false)) 3 else 1

        orientationEventListener =
            object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    updateOrientation()
                }
            }
        orientationEventListener?.enable()

        val subDisplay: Display? = getSubDisplay()
        if (subDisplay == null) {
            Toast.makeText(this, "Cover screen not detected!", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
        if (mPresentationDialog == null) {
            mPresentationDialog = CoverPresentation(this, subDisplay)
        }
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
        try {
            mPresentationDialog!!.show()
        } catch (unused: WindowManager.InvalidDisplayException) {
            Log.w("subuimirror", "Display was removed")
            mPresentationDialog = null
        }

        mProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mScreenSharing = true

        Handler().postDelayed({
            mMediaProjection = mProjectionManager!!.getMediaProjection(
                intent.getIntExtra("int", 0),
                intent.extras?.getParcelable("data")!!
            )

            val surfaceView = mPresentationDialog!!.findViewById<TextureView>(R.id.surfaceView)

            val windowService = getSystemService(WINDOW_SERVICE) as WindowManager
            val currentRotation = windowService.defaultDisplay.rotation

            updateOrientation()

            val orientationType = if(currentRotation == Surface.ROTATION_0 || currentRotation == Surface.ROTATION_180) Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE

            val baseScale = if(orientationType == Configuration.ORIENTATION_PORTRAIT) 1F else 0.5F

            setScale(surfaceView, baseScale * sf, baseScale * sf)

            mMediaProjection!!.createVirtualDisplay(
                "cover",
                512,
                260,
                100,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                Surface(surfaceView.surfaceTexture),
                null,
                null
            ) }, 500)

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    fun setScale(surfaceView: TextureView, x: Float, y: Float){

        val pivotPointX: Float = 512 / 2F
        val pivotPointY: Float = 260 / 2F

        val matrix = Matrix()
        matrix.setScale(x, y, pivotPointX, pivotPointY)

        surfaceView.setTransform(matrix)
    }

    fun updateOrientation(){
        val windowService = getSystemService(WINDOW_SERVICE) as WindowManager

        when(windowService.defaultDisplay.rotation){
            Surface.ROTATION_0 -> {
                val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                val rotation = if(sharedPref.getInt("portrait", 0) == 0) 180F else 90F
                val baseScale = 1F

                mPresentationDialog?.findViewById<TextureView>(R.id.surfaceView)?.rotation = rotation

                setScale(mPresentationDialog!!.findViewById(R.id.surfaceView), baseScale * sf, baseScale * sf)
            }
            Surface.ROTATION_90 -> {
                val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                val rotation = if(sharedPref.getInt("landscape", 0) == 0) 90F else 180F
                val baseScale = if(sharedPref.getInt("landscape", 0) == 0) 0.5F else 1F

                mPresentationDialog?.findViewById<TextureView>(R.id.surfaceView)?.rotation = rotation

                setScale(mPresentationDialog!!.findViewById(R.id.surfaceView), baseScale * sf, baseScale * sf)
            }
            Surface.ROTATION_180 -> {
                val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                val rotation = if(sharedPref.getInt("portrait", 0) == 0) 0F else 90F
                val baseScale = 1F

                mPresentationDialog?.findViewById<TextureView>(R.id.surfaceView)?.rotation = rotation

                setScale(mPresentationDialog!!.findViewById(R.id.surfaceView), baseScale * sf, baseScale * sf)
            }
            Surface.ROTATION_270 -> {
                val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                val rotation = if(sharedPref.getInt("landscape", 0) == 0) 270F else 0F
                val baseScale = if(sharedPref.getInt("landscape", 0) == 0) 0.5F else 1F

                mPresentationDialog?.findViewById<TextureView>(R.id.surfaceView)?.rotation = rotation

                setScale(mPresentationDialog!!.findViewById(R.id.surfaceView), baseScale * sf, baseScale * sf)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        mMediaProjection?.stop()
        mPresentationDialog?.cancel()
        orientationEventListener?.disable()
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun getSubDisplay(): Display? {
        val displays: Array<Display> =
            (getSystemService("display") as DisplayManager).getDisplays("com.samsung.android.hardware.display.category.BUILTIN")
        Log.d("subuimirror", "builtInDisplays size : " + displays.size)
        val display: Display? = if (displays.size > 1) displays[1] else null
        return display
    }

    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 373

    private fun generateForegroundNotification() {
        val intentMainLanding = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intentMainLanding, 0)
        iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        if (mNotificationManager == null) {
            mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        assert(mNotificationManager != null)
        mNotificationManager?.createNotificationChannelGroup(
            NotificationChannelGroup("mirror_service", "Mirror service")
        )
        val notificationChannel =
            NotificationChannel("service_channel", "Service Notifications",
                NotificationManager.IMPORTANCE_MIN)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager?.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "service_channel")

        builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" service is running").toString())
            .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
            .setContentText("Touch to open")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (iconNotification != null) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
        }
        builder.color = resources.getColor(R.color.purple_200)
        notification = builder.build()
        startForeground(mNotificationId, notification)

    }

}