package kr.drone.helpgpt.ui.activity

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
import kr.drone.helpgpt.ui.view.MyWebViewClient
import kr.drone.helpgpt.vm.MainViewModel
import timber.log.Timber

@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding, MainViewModel>() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_main
    }

    override fun getViewModelClass(): Class<MainViewModel> {
        return MainViewModel::class.java
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
    }
    override fun initEvent() {
        repeatOnStarted {
            viewModel.event.collect {
                when(it) {
                    MainViewModel.EVENT_START_CRAWLING ->
                        binding.crawlingWebview.loadUrl(viewModel.address.value)
                }
            }
        }
        repeatOnStarted {
            viewModel.address.collect {
                viewModel.extractVideoIdFromUrl(it)
            }
        }
        repeatOnStarted {
            viewModel.videoId.collect{
                if(it != ""){
                    binding.youTubePlayerView.invalidate()
                    delay(100L)
                    binding.youTubePlayerView.play(it)
                    Timber.d("play video $it")
                }
            }
        }
    }
}