package kr.ac.korea.translator.view.main

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kr.ac.korea.translator.R
import kr.ac.korea.translator.databinding.ActivityMainBinding
import kr.ac.korea.translator.service.TopService
import kr.ac.korea.translator.utils.TargetLanguageSelectorUtils
import kr.ac.korea.translator.view.common.BaseActivity

class MainActivity : BaseActivity(){
    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.apply {
                this.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.parseColor("#ffffff")
            }
        }
        binding.spnLanguage.isFocusable = true
        binding.spnLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TargetLanguageSelectorUtils.getSupportedLanguages(this))
        binding.spnLanguage.setSelection(TargetLanguageSelectorUtils.getSavedTargetLanguageIndex(this))
        binding.spnLanguage.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                TargetLanguageSelectorUtils.setLanguagePreferencesByIndex(this@MainActivity, i)
            }
            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                TargetLanguageSelectorUtils.setLanguagePreferencesByIndex(this@MainActivity, 0)
            }
        }

        // adView
        MobileAds.initialize(this, getString(R.string.admob_app_id))
        binding.adView.loadAd(AdRequest.Builder().build())

        // Buttons
        binding.btnStart.setOnClickListener { startOverlay() }
        binding.btnStop.setOnClickListener { Process.killProcess(Process.myPid()) }
        setGuideButtons()
    }

    private fun setGuideButtons() {
        binding.btnHelp.setOnClickListener {
            binding.adView.visibility = View.INVISIBLE
            binding.guide1.visibility = View.VISIBLE
            binding.nextGuide1.setOnClickListener {
                binding.guide1.visibility = View.INVISIBLE
                binding.guide2.visibility = View.VISIBLE
            }
            binding.nextGuide2.setOnClickListener {
                binding.guide2.visibility = View.INVISIBLE
                binding.guide3.visibility = View.VISIBLE
            }
            binding.nextGuide3.setOnClickListener {
                binding.guide3.visibility = View.INVISIBLE
                binding.guide4.visibility = View.VISIBLE
            }
            binding.nextGuide4.setOnClickListener {
                binding.guide4.visibility = View.INVISIBLE
                binding.adView.visibility = View.VISIBLE
            }
        }
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            startService(Intent(this@MainActivity, TopService::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        startOverlay()
    }
}