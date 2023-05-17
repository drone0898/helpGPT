package kr.drone.helpgpt.ui.activity

import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
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

    override fun initBinding() {
        binding.settingsBtn.setOnClickListener {
            if(binding.settingsSlidePane.isOpen){
                binding.settingsSlidePane.closePane()
            } else{
                binding.settingsSlidePane.openPane()
            }
        }
    }
    override fun initEvent() {
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