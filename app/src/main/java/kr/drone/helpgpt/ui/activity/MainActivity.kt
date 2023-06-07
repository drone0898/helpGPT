package kr.drone.helpgpt.ui.activity

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import dagger.hilt.android.AndroidEntryPoint
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
import kr.drone.helpgpt.ui.view.MyWebViewClient
import kr.drone.helpgpt.vm.MainViewModel

@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding>() {

    val viewModel by viewModels<MainViewModel>()

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_main
    }

    override fun bindingViewModel() {
        binding.setVariable(BR.viewModel, viewModel)
//        lifecycle.addObserver(viewModel)
    }

    override fun initialize() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initBinding() {
        binding.settingsBtn.setOnClickListener {
            if(binding.settingsSlidePane.isOpen){
                binding.settingsSlidePane.closePane()
            } else{
                binding.settingsSlidePane.openPane()
            }
        }
        if(BuildConfig.DEBUG){
            WebView.setWebContentsDebuggingEnabled(true)
        }
        binding.crawlingWebview.webViewClient = MyWebViewClient()
        binding.crawlingWebview.let {
            it.settings.mediaPlaybackRequiresUserGesture = true
            it.settings.domStorageEnabled = true // Sets whether the DOM storage API is enabled.
            it.settings.allowContentAccess= true
            it.settings.displayZoomControls = false
            it.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            it.settings.javaScriptEnabled= true
        }
        binding.summaryBtn.setOnClickListener{
            startTargetActivity(SummaryProcActivity::class.java,null,true)
        }
    }
    override fun initEvent() {
        repeatOnStarted {
//            viewModel.event.collect {
//                when(it) {
//                    MainViewModel.EVENT_START_CRAWLING ->
//                        binding.crawlingWebview.loadUrl(viewModel.address.value)
//                }
//            }
        }
        repeatOnStarted {
            viewModel.address.collect {
                viewModel.extractVideoIdFromUrl(it)
            }
        }
        repeatOnStarted {
            viewModel.videoId.collect{
                if(it != ""){
//                    binding.youTubePlayerView.invalidate()
//                    delay(100L)
//                    binding.youTubePlayerView.play(it)
//                    Timber.d("play video $it")
                }
            }
        }
    }
}