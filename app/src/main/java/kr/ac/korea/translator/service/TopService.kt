package kr.ac.korea.translator.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageButton
import kr.ac.korea.translator.R
import kr.ac.korea.translator.view.main.CoverActivity

class TopService : Service(), OnTouchListener {
    var vOverlay: View? = null
    var xpos:Float = 0f
    var ypos:Float = 0f
    private var wm: WindowManager? = null
    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val li = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.START or Gravity.TOP
        lp.type = checkVersion()
        vOverlay = li.inflate(R.layout.layout_top_service, null)
        vOverlay?.setOnTouchListener(this)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm?.addView(vOverlay, lp)
        vOverlay?.findViewById<ImageButton>(R.id.btn_translate)?.setOnClickListener{ _->
            startActivity(Intent(applicationContext, CoverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        vOverlay?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener{_->stopSelf() }
    }

    override fun onDestroy() {
        super.onDestroy()
        vOverlay?.apply{
            wm?.removeView(vOverlay)
            vOverlay = null
        }
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action
        val pointer = motionEvent.pointerCount
        when (action) {
            MotionEvent.ACTION_UP -> if (pointer == 1) {
                vOverlay?.performClick()
            }
            MotionEvent.ACTION_DOWN -> if (pointer == 1) {
                xpos = motionEvent.rawX
                ypos = motionEvent.rawY
            }
            MotionEvent.ACTION_MOVE -> if (pointer == 1) {
                val lp = view?.layoutParams as WindowManager.LayoutParams
                val dx :Float = xpos - motionEvent.rawX
                val dy :Float = ypos - motionEvent.rawY
                xpos = motionEvent.rawX
                ypos = motionEvent.rawY
                lp.x = (lp.x - dx).toInt()
                lp.y = (lp.y - dy).toInt()
                wm?.updateViewLayout(view, lp)
                return true
            }
        }
        return false
    }

    private fun checkVersion(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

}