package kr.drone.helpgpt.ui.activity

import android.view.View
import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import dagger.hilt.android.AndroidEntryPoint
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityTranslateBinding
import kr.drone.helpgpt.vm.TranslateViewModel

@AndroidEntryPoint
class TranslateActivity: BaseActivity<ActivityTranslateBinding>() {

    val viewModel:TranslateViewModel by viewModels()
    override fun getLayoutResourceId(): Int {
        return R.layout.activity_translate
    }

    override fun bindingViewModel() {
        binding.setVariable(BR.viewModel, viewModel)
    }

    override fun initialize() {

    }

    override fun initBinding() {
    }

    override fun initEvent() {
    }
}