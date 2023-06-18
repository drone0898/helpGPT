package kr.drone.helpgpt.ui.activity

import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityIntroBinding
import kr.drone.helpgpt.vm.IntroViewModel

class IntroActivity: BaseActivity<ActivityIntroBinding>() {

    val viewModel by viewModels<IntroViewModel>()

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_intro
    }

    override fun bindingViewModel() {
        binding.setVariable(BR.viewModel, viewModel)
    }

    override fun initialize() {

    }

    override fun initBinding() {

    }

    override fun initEvent() {
        if(BuildConfig.DEBUG){

//            viewModel.insertTestData()
        }
    }
}