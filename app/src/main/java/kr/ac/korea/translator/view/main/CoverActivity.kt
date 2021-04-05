package kr.ac.korea.translator.view.main

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.ac.korea.translator.R
import kr.ac.korea.translator.databinding.ActivityCoverBinding
import kr.ac.korea.translator.model.TranslateResponse
import kr.ac.korea.translator.model.TextContainer
import kr.ac.korea.translator.model.TranslateRequest
import kr.ac.korea.translator.network.TranslationService
import kr.ac.korea.translator.service.CaptureService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class CoverActivity : AppCompatActivity() {
    private var mProjectionManager: MediaProjectionManager? = null
    private val REQUEST_CODE = 0
    private var mProgressDialog: ProgressDialog? = null
    lateinit var serviceIntent:Intent
    private lateinit var binding: ActivityCoverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_cover)
        // Set cover UI
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        binding.coverContainer.setOnClickListener {
            finishAndRemoveTask()
        }
        // Set media projection
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)
            serviceIntent = Intent(this, CaptureService::class.java)
            serviceIntent.putExtra(Intent.EXTRA_INTENT, data)
            serviceIntent.putExtra("width", size.x)
            serviceIntent.putExtra("height", size.y)
            serviceIntent.putExtra("rotation", windowManager.defaultDisplay.rotation)
            serviceIntent.putExtra("RESULT_CODE", resultCode)
            val observer = androidx.lifecycle.Observer<MutableList<TextContainer>> { recognizedTexts ->
                if(mProgressDialog!=null && recognizedTexts.size == 0) {
                    hideProgressDialog()
                    Toast.makeText(applicationContext, "번역할 글자를 찾지 못했습니다", Toast.LENGTH_LONG).show()
                }
                else {
                    val retrofit = Retrofit.Builder()
                            .baseUrl(resources.getString(R.string.translate_api_path))
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                    val translateService = retrofit.create(TranslationService::class.java)
                    val translateCall: Call<List<TranslateResponse>> = translateService.translate(
                            resources.getString(R.string.translate_key),
                            UUID.randomUUID().toString(),
                            recognizedTexts.map{TranslateRequest(it.rst!!)}
                    )
                    translateCall.enqueue(object : Callback<List<TranslateResponse>> {
                        override fun onResponse(call: Call<List<TranslateResponse>>, response: Response<List<TranslateResponse>>) {
                            hideProgressDialog()
                            val statusBarHeight = getStatusBarHeight()
                            val translatedTexts = response.body()
                            // 표시해도 되는 결과인지 check
                            translatedTexts?.forEachIndexed { i, translatedText ->
                                if (checkIfCorrectlyTranslated(translatedText, recognizedTexts[i].rst)) {
                                    val textView = TextView(this@CoverActivity)
                                    textView.text = translatedText.translations!![0]!!.text
                                    val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    params.setMargins(recognizedTexts[i].x, recognizedTexts[i].y - statusBarHeight, 0, 0)
                                    textView.layoutParams = params
                                    textView.setTextColor(Color.WHITE)
                                    //Set Textsize - Pixel based on height
                                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, recognizedTexts[i].h.toFloat())
                                    textView.setBackgroundColor(resources.getColor(R.color.transparentBlack, theme))
                                    textView.gravity = View.TEXT_ALIGNMENT_CENTER
                                    binding.coverContainer.addView(textView)
                                }
                            }
                            recognizedTexts.clear()
                        }
                        override fun onFailure(call: Call<List<TranslateResponse>>, t: Throwable) {
                            Log.e("network Error", "translateApi onFailure")
                        }
                    })
                }
            }
            CaptureService.ocrResult.observe(this, observer)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showProgressDialog()
                startForegroundService(serviceIntent)
            }
            else{
                startService(serviceIntent)
            }
        }
    }


    private fun getStatusBarHeight():Int{
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    @Suppress("DEPRECATION")
    private fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this@CoverActivity)
            mProgressDialog!!.setMessage("번역중...")
            mProgressDialog!!.isIndeterminate = true
        }
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }

    fun checkIfCorrectlyTranslated(translateResponse: TranslateResponse?, originalTxt: String?): Boolean {
        if (translateResponse == null) {
            return false
        }
        val translations: List<TranslateResponse.Translation?>? = translateResponse.translations
                ?: return false
        val translated = translations!![0]?.text
        // 타겟 언어나 번역되지 않은 언어 거르기
        return !translateResponse.detectedLanguage?.language.equals("ko") && originalTxt != translated
    }
}