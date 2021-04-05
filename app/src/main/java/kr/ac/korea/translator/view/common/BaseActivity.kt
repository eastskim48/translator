package kr.ac.korea.translator.view.common

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : Activity(), View.OnClickListener {
    protected var token: String? = null
    override fun onClick(v: View?) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun init() {}
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}