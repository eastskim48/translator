package kr.ac.korea.translator.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import kr.ac.korea.translator.model.TextContainer
import kr.ac.korea.translator.network.OCRApi
import kr.ac.korea.translator.view.main.MainActivity
import java.io.FileOutputStream
import java.io.IOException

class CaptureService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_ID = 1
        private const val CHANNEL_ID = "MediaProjectionService"
        private const val SCREENCAP_NAME = "screencap"
        private const val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        val ocrResult by lazy{
            MutableLiveData<MutableList<TextContainer>>()
        }
    }

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mProjectionManager: MediaProjectionManager
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mImageReader: ImageReader
    private var mDensity:Int = 0
    private var mRotation:Int = 0
    private var mWidth:Int = 0
    private var mHeight:Int = 0
    private var finished:Boolean=false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent:Intent? = intent!!.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        mWidth = intent.getIntExtra("width", 0)
        mHeight = intent.getIntExtra("height", 0)
        val resultCode:Int = intent.getIntExtra("RESULT_CODE", 0)
        mRotation = intent.getIntExtra("rotation", 0)
        startMediaProjection(captureIntent!!, resultCode)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }
        mMediaProjection.let{
            it.stop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_NONE
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(serviceChannel)
            }
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
                .setContentTitle("Foreground Service")
                .setContentIntent(pendingIntent)
                .build()
        startForeground(FOREGROUND_SERVICE_ID, notification)
    }

    private fun startMediaProjection(data:Intent, code:Int){
        mMediaProjection = mProjectionManager.getMediaProjection(code, data)
        mMediaProjection.also{
            // display metrics
            val metrics = resources.displayMetrics
            mDensity = metrics.densityDpi
            // create virtual display depending on device width / height
            createVirtualDisplay()
            // register media projection stop callback
            mMediaProjection.registerCallback(object:MediaProjection.Callback(){
                override fun onStop() {
                    mVirtualDisplay.release()
                    mImageReader.setOnImageAvailableListener(null, null)
                    mMediaProjection.unregisterCallback(this)
                }
            },null)
        }
    }

    @SuppressLint("WrongConstant")
    fun createVirtualDisplay() {
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, RGBA_8888, 1)
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.surface, null, null)
        mImageReader.setOnImageAvailableListener(ImageAvailableListener(), null)
    }

    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if(finished) return
            finished = true
            var image: Image? = null
            val fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            val ocrApi = OCRApi(applicationContext)
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val pixelStride:Int = planes[0].pixelStride
                    val rowPadding:Int = planes[0].rowStride - pixelStride * mWidth
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(planes[0].buffer)
                }
                val updateResult:((MutableList<TextContainer>)->(Unit))= {
                    ocrResult.postValue(it)
                }
                ocrApi.post(bitmap, updateResult)
            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
                bitmap?.recycle()
                image?.close()
                stopSelf()
            }
        }
    }
}