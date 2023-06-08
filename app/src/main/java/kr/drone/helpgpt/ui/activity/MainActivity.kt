package kr.drone.helpgpt.ui.activity

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kr.co.prnd.YouTubePlayerView
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
import kr.drone.helpgpt.ui.view.MyWebViewClient
import kr.drone.helpgpt.vm.MainViewModel

@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding>() {

    val viewModel by viewModels<MainViewModel>()
    var playerOnInitialized:Boolean = false
    var pendingVideoId:String? = null
    private val onInitializedListener = object : YouTubePlayerView.OnInitializedListener {
        override fun onInitializationSuccess(
            provider: YouTubePlayer.Provider,
            player: YouTubePlayer,
            wasRestored: Boolean
        ) {
            playerOnInitialized = true
            pendingVideoId?.let { videoId ->
                player.loadVideo(videoId)
                pendingVideoId = null
            }
        }

        override fun onInitializationFailure(
            provider: YouTubePlayer.Provider,
            result: YouTubeInitializationResult
        ) {
            // Handle initialization failure here
        }
    }


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
        binding.settingsBtn.setOnClickListener {
            if(binding.settingsSlidePane.isOpen){
                binding.settingsSlidePane.closePane()
            } else{
                binding.settingsSlidePane.openPane()
            }
        }
        binding.summaryBtn.setOnClickListener{
            startTargetActivity(SummaryProcActivity::class.java,null,true)
        }
        binding.youTubePlayerView.onInitializedListener = onInitializedListener
    }
    override fun initEvent() {
        repeatsOnStarted (
            listOf(
                {
                    viewModel.event.collect {
                        when (it) {
                            MainViewModel.EVENT_START_CRAWLING ->
                                binding.crawlingWebview.loadUrl(viewModel.address.value)
                        }
                    }
                },
                {
                    viewModel.address.collect {
                        viewModel.extractVideoIdFromUrl(it)
                    }
                },
                {
                    viewModel.videoId.collectLatest {
                        if (it != "") {
                            if(playerOnInitialized){
                                binding.youTubePlayerView.play(it)
                            }else{
                                pendingVideoId = it
                            }
                        }
                    }
                }
            )
        )
    }
}