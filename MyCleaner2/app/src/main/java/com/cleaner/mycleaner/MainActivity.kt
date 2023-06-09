package com.cleaner.mycleaner

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.cleaner.mycleaner.R
import com.cleaner.mycleaner.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import com.google.android.material.button.MaterialButton
import org.w3c.dom.Text
import java.io.File
import java.text.DecimalFormat
import kotlin.random.Random

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences
    var Clean_Button: ImageView?=null
    var textViewStatus:TextView?=null
    var textViewPercentage:TextView?=null
    var textView_bit:TextView?=null
    var lottie : LottieAnimationView?=null
    //var progressBar: ProgressBar?=null
    private var currentPreferenceButtonPositions: Boolean = false

    //admob
    private var mInterstitialAd: InterstitialAd? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Clean_Button=findViewById(R.id.clean_bt)
        textViewStatus=findViewById(R.id.textView)
        textViewPercentage=findViewById(R.id.textView2)
        textView_bit=findViewById(R.id.byte_tx)
        lottie=findViewById(R.id.lottieAnimationView)
        loadAds()
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                mInterstitialAd = null
                loadAds()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
            }
        }
        //progressBar=findViewById(R.id.progressBar)
        Clean_Button?.setOnClickListener {
            //램덤
            if (shouldShowAds()) {
                showAds()
            }else{
                lottie?.playAnimation()
                clean()
            }
        }

    }
    private fun shouldShowAds(): Boolean {
        // 0부터 1 사이의 난수를 생성
        val randomValue = Random.nextDouble()
        // 0.5 이하의 값일 경우 true 반환 (50% 확률)ㄴ
        return randomValue <= 0.5
    }
    //AdMob
    fun loadAds(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-8424167402195797/5965508747", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                //Log.d(TAG, adError?.toString())
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                //Log.d(TAG, 'Ad was loaded.')
                mInterstitialAd = interstitialAd
            }
        })

    }
    fun showAds(){
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            //Log.d(TAG, "The interstitial ad wasn't ready yet.")
        }
    }
    private val isAccessGranted: Boolean
        get() = try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode: Int = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    private fun requestStoragePermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requiredPermissions += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO + Manifest.permission.READ_MEDIA_IMAGES + Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            }
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            if (!isAccessGranted) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
        ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), 1)
    }

    private fun clean() {
        requestStoragePermissions()
        if (!FileScanner.isRunning) {
            val oneClickCleanEnabled = preferences.getBoolean(getString(R.string.key_one_click_clean), false)
            if (oneClickCleanEnabled) {
                Thread {
                    //Toast.makeText(this,"111111",Toast.LENGTH_SHORT).show()
                    scan(true)
                }.start()
            } else {
                //다이얼로그 띄우는곳인것같음
                //Toast.makeText(this,"22222",Toast.LENGTH_SHORT).show()
                Thread {
                    scan(true)
                }.start()
                //val mDialog = MaterialDialog.Builder(this)
                //    .setTitle(getString(R.string.clean_confirm_title))
                //    .setAnimation(R.raw.delete)
                //    .setMessage(getString(R.string.summary_dialog_button_clean))
                //    .setCancelable(false)
                //    .setPositiveButton(getString(R.string.clean)) { dialogInterface, _ ->
                //        Thread {
                //            scan(true)
                //        }.start()
                //        dialogInterface.dismiss()
                //    }
                //    .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface, _ ->
                //        dialogInterface.dismiss()
                //    }
                //    .build()
                //mDialog.animationView.scaleType = ImageView.ScaleType.FIT_CENTER
                //mDialog.show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun scan(delete: Boolean) {
        Looper.prepare()
        runOnUiThread {
            Clean_Button?.isEnabled = !FileScanner.isRunning
            Clean_Button?.setImageResource(R.drawable.clean_bt_2)
            textViewPercentage?.text = "0%"
            textView_bit?.text="00"

        }
        reset()
        if (preferences.getBoolean(getString(R.string.key_clipboard), false)) clearClipboard()
        runOnUiThread {
            arrangeViews(delete)
            textViewStatus?.text = getString(R.string.status_running)//@@
        }
        val path = Environment.getExternalStorageDirectory()

        val fileScanner = FileScanner(path, this)
            .setEmptyDir(preferences.getBoolean(getString(R.string.key_filter_empty), false))
            .setAutoWhite(preferences.getBoolean(getString(R.string.key_auto_whitelist), true))
            .setInvalid(preferences.getBoolean(getString(R.string.key_invalid_media_cleaner), false))
            .setDelete(delete)
            .setCorpse(preferences.getBoolean(getString(R.string.key_filter_corpse), false))
            .setGUI(binding)
            .setContext(this)
            .setUpFilters(
                preferences.getBoolean(getString(R.string.key_filter_generic), true),
                preferences.getBoolean(getString(R.string.key_filter_aggressive), false),
                preferences.getBoolean(getString(R.string.key_filter_apk), false),
                preferences.getBoolean(getString(R.string.key_filter_archive), false))
        if (path.listFiles() == null) {
            val textView = printTextView(getString(R.string.clipboard_clean_failed), Color.RED)
            runOnUiThread {
                //binding.linearLayoutFiles.addView(textView)
            }
        }
        val kilobytesTotal = fileScanner.startScan()
        runOnUiThread {//lottie?.pauseAnimation()

            lottie?.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    // 애니메이션이 시작될 때 실행되는 콜백
                }

                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션이 종료될 때 실행되는 콜백

                }

                override fun onAnimationCancel(animation: Animator) {
                    // 애니메이션이 취소될 때 실행되는 콜백
                }

                override fun onAnimationRepeat(animation: Animator) {
                    // 애니메이션이 반복될 때 실행되는 콜백
                    if (delete){
                        lottie?.pauseAnimation()
                    }
                }
            })
            if(delete){
                var tt= convertSize_1(kilobytesTotal)
                if(tt.length<2){
                    textView_bit?.text="0"+tt
                }else{
                    textView_bit?.text= convertSize_1(kilobytesTotal)
                }
                textViewStatus?.text = convertSize_2(kilobytesTotal)

            }else{
                var tt= convertSize_1(kilobytesTotal)
                if(tt.length<10){
                    textView_bit?.text= "0"+tt
                }else{
                    textView_bit?.text= convertSize_1(kilobytesTotal)
                }
                textViewStatus?.text = convertSize_2(kilobytesTotal)
            }
            //progressBar?.progress = progressBar!!.max
            textViewPercentage?.text = "100%"
        }
        runOnUiThread {
            Clean_Button?.isEnabled = !FileScanner.isRunning
            Clean_Button?.setImageResource(R.drawable.clean_bt_1)

        }
        Looper.loop()
    }
    private fun reset() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        runOnUiThread {
            //바인드
            //binding.linearLayoutFiles.removeAllViews()
            //progressBar?.progress = 0
            //progressBar?.max = 1
        }
    }
    private fun arrangeViews(isDelete: Boolean) {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //if (isDelete) {
            //    //바인드
            //    binding.frameLayoutMain.visibility = View.VISIBLE
            //    binding.scrollViewFiles.visibility = View.GONE
            //} else {
            //    binding.frameLayoutMain.visibility = View.GONE
            //    binding.scrollViewFiles.visibility = View.VISIBLE
            //}
        }
    }
    private fun printTextView(text: String, color: Int): TextView {
        val textView = TextView(this)
        textView.setTextColor(color)
        textView.text = text
        textView.setPadding(3, 3, 3, 3)
        return textView
    }
    private fun clearClipboard() {
        try {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboardManager.clearPrimaryClip()
            } else {
                val clipData = ClipData.newPlainText("", "")
                clipboardManager.setPrimaryClip(clipData)
            }
        } catch (e: NullPointerException) {
            runOnUiThread {
                Toast.makeText(this, R.string.clipboard_clean_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun displayText(text: String) {
        val textColor = resources.getColor(R.color.colorSecondary, theme)
        val textView = printTextView(text, textColor)
        runOnUiThread {
            //binding.linearLayoutFiles.addView(textView)
        }
        //binding.scrollViewFiles.post {
        //    binding.scrollViewFiles.fullScroll(ScrollView.FOCUS_DOWN)
        //}
    }
    fun displayDeletion(file: File): TextView {
        val textView = printTextView(file.absolutePath, resources.getColor(R.color.colorPrimary, resources.newTheme()))
        runOnUiThread {
            //binding.linearLayoutFiles.addView(textView)
        }
        //binding.scrollViewFiles.post {
        //    binding.scrollViewFiles.fullScroll(ScrollView.FOCUS_DOWN)
        //}
        return textView
    }




    companion object {
        @JvmField
        var preferences: SharedPreferences? = null
        @JvmStatic
        fun convertSize(length: Long): String {
            val format = DecimalFormat("#.##")
            val mib = (1024 * 1024).toLong()
            val kib: Long = 1024
            return when {
                length > mib -> "${format.format(length / mib)} MB"
                length > kib -> "${format.format(length / kib)} KB"
                else -> "${format.format(length)} B"
            }
        }
        fun convertSize_1(length: Long): String {
            val format = DecimalFormat("#.##")
            val mib = (1024 * 1024).toLong()
            val kib: Long = 1024
            return when {
                length > mib -> "${format.format(length / mib)}"
                length > kib -> "${format.format(length / kib)}"
                else -> "${format.format(length)}"
            }
        }
        fun convertSize_2(length: Long): String {
            val format = DecimalFormat("#.##")
            val mib = (1024 * 1024).toLong()
            val kib: Long = 1024
            return when {
                length > mib -> "MB"
                length > kib -> "KB"
                else -> "B"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAds()
    }

}