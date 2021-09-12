package com.carudibu.android.subuimirror

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import android.os.Build
import android.view.*
import androidx.core.app.NotificationCompat
import java.lang.Exception
import java.lang.reflect.Field
import android.view.animation.Animation

import android.view.animation.RotateAnimation
import android.widget.FrameLayout








class MirrorService: Service() {

    private var mScreenDensity = 0
    private var mProjectionManager: MediaProjectionManager? = null
    private var mScreenSharing = false
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: Display? = null
    private var mSurface: Surface? = null
    private val mSurfaceView: SurfaceView? = null

    private val mAttachedLcdSize: Point = Point()
    private var mPresentationDialog: Presentation? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        generateForegroundNotification()

        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()

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

        val metrics = DisplayMetrics()

        mScreenDensity = metrics.densityDpi
        mProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mScreenSharing = true


        Handler().postDelayed({
            mMediaProjection = mProjectionManager!!.getMediaProjection(
                intent.getIntExtra("int", 0),
                intent.extras?.getParcelable("data")!!
            )

            val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            val surfaceView = mPresentationDialog!!.findViewById<TextureView>(R.id.surfaceView)

            val sf = if(sharedPref.getBoolean("crop", false)) 3 else 1

            surfaceView.scaleY = 1F * sf
            surfaceView.scaleX = 1F * sf


            mMediaProjection!!.createVirtualDisplay(
                "cover",
                512,
                260,
                100,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                Surface(surfaceView.surfaceTexture),
                null,
                null
            )
        }, 500)

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        mMediaProjection?.stop()
        mPresentationDialog?.cancel()
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun getSubDisplay(): Display? {
        val displays: Array<Display> =
            (getSystemService("display") as DisplayManager).getDisplays("com.samsung.android.hardware.display.category.BUILTIN")
        Log.d("subuimirror", "builtInDisplays size : " + displays.size)
        val display: Display? = if (displays.size > 1) displays[1] else null
        if (display != null) {
            //display.getRealSize(this.mAttachedLcdSize)
        }
        return display
    }

    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, 0)
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel("service_channel", "Service Notifications",
                        NotificationManager.IMPORTANCE_MIN)
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
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


}