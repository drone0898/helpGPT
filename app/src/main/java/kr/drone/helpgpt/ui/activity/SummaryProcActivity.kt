package kr.drone.helpgpt.ui.activity

import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import dagger.hilt.android.AndroidEntryPoint
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivitySummaryProcBinding
import kr.drone.helpgpt.vm.SummaryProcViewModel

@AndroidEntryPoint
class SummaryProcActivity: BaseActivity<ActivitySummaryProcBinding>() {

    val viewModel:SummaryProcViewModel by viewModels()
    override fun getLayoutResourceId(): Int {
        return R.layout.activity_summary_proc
    }

    override fun bindingViewModel() {
        binding.setVariable(BR.viewModel, viewModel)
    }

    override fun initialize() {

    }

    override fun initBinding() {

    }

    override fun initEvent() {
        repeatOnStarted {
            viewModel.testOpenAI()
        }
    }
}