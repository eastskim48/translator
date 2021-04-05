package kr.ac.korea.translator.utils

import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat

object VersionCompatibilityUtils {
    fun setStatusColor(context: Context?, window: Window?, color: Int) {
        if (Build.VERSION.SDK_INT >= 21) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window?.statusBarColor = ContextCompat.getColor(context!!, color)
        }
    }
}