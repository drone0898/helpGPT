package kr.drone.helpgpt.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kr.co.prnd.YouTubePlayerView
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
import kr.drone.helpgpt.service.AudioCaptureService
import kr.drone.helpgpt.service.TranslateScriptService
import kr.drone.helpgpt.ui.view.MyWebViewClient
import kr.drone.helpgpt.vm.MainViewModel


@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding>() {

    val viewModel by viewModels<MainViewModel>()

    private val translationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val audioCaptureIntent = Intent(this, AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_START
                    if(it.data!=null){
                        putExtra(AudioCaptureService.EXTRA_RESULT_DATA, it.data)
                    }
                }
                startForegroundService(audioCaptureIntent)
            } else {
                Toast.makeText(
                    this, "Request to obtain MediaProjection denied.",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            val overlayPermission = Settings.canDrawOverlays(this)
            if(!overlayPermission){
                viewModel.showBtn()
                Toast.makeText(this,"화면위에 그리기 권한이 없어 실행할 수 없습니다.",
                    Toast.LENGTH_SHORT).show()
            }else{
                startCaptureIntent(translationWithOverlayLauncher)
            }
        }

    private val translationWithOverlayLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                val scriptServiceIntent = Intent(this,
                    TranslateScriptService::class.java).apply {
                    action = TranslateScriptService.ACTION_START
                    if(it.data!=null){
                        putExtra(TranslateScriptService.EXTRA_RESULT_DATA, it.data)
                    }
                }
                val audioCaptureIntent = Intent(this,
                    AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_START
                    if(it.data!=null){
                        putExtra(AudioCaptureService.EXTRA_RESULT_DATA, it.data)
                    }
                }
                startForegroundService(audioCaptureIntent)
                startForegroundService(scriptServiceIntent)
            }
        }

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_main
    }

    override fun bindingViewModel() {
        binding.setVariable(BR.viewModel, viewModel)
    }

    override fun initialize() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initBinding() {
        if(BuildConfig.DEBUG){
            WebView.setWebContentsDebuggingEnabled(true)
        }
        binding.youtubeWebview.webViewClient = MyWebViewClient()
        binding.youtubeWebview.let {
            it.settings.mediaPlaybackRequiresUserGesture = true
            it.settings.domStorageEnabled = true // Sets whether the DOM storage API is enabled.
            it.settings.allowContentAccess= true
            it.settings.displayZoomControls = false
            it.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            it.settings.javaScriptEnabled= true
        }
        binding.settingsBtn.setOnClickListener {
            if(binding.settingsSlidePane.isOpen){
                binding.settingsSlidePane.closePane()
            } else{
                binding.settingsSlidePane.openPane()
            }
        }
        binding.summaryBtn.setOnClickListener{
            viewModel.onSummaryBtnClick()
        }

        binding.translateBtn.setOnClickListener{
            viewModel.onTranslateBtnClick()
            checkRequestedPermission(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)) {
                startCaptureIntent(translationLauncher)
            }
        }

        binding.translateServiceBtn.setOnClickListener{
            viewModel.onTranslateBtnClick()
            checkRequestedPermission(listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)) {
                val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                if(!Settings.canDrawOverlays(this)){
                    overlayPermissionLauncher.launch(overlayIntent)
                } else{
                    startCaptureIntent(translationWithOverlayLauncher)
                }
            }
        }
    }

    private fun startCaptureIntent(launcher:ActivityResultLauncher<Intent>) {
        val mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun checkRequestedPermission(permissions:List<String>, granted: ()->Unit){
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    granted()
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission Denied\n$deniedPermissions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .setPermissions(*permissions.toTypedArray()).check()
    }
    override fun initEvent() {
        repeatsOnStarted (
            listOf(
                {
                    viewModel.address.collect {
                        viewModel.extractVideoIdFromUrl(it)
                    }
                },
                {
                    viewModel.videoId.collectLatest {
                        if (it != "") {
                            binding.youtubeWebview.loadUrl(viewModel.address.value)
                        }
                    }
                }
            )
        )
    }
}