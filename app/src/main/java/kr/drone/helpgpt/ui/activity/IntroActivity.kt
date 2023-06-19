package kr.drone.helpgpt.ui.activity

import androidx.activity.viewModels
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.drone.helpgpt.BuildConfig
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityIntroBinding
import kr.drone.helpgpt.util.PROPERTIES_TEST_API_KEY
import kr.drone.helpgpt.vm.IntroViewModel
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityIntroBinding>() {

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
        lifecycleScope.launch (Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                kotlin.runCatching {
                    onCheckUserProfile {
                        result -> if(result.isSuccess) {
                            startTargetActivity(MainActivity::class.java,null,false)
                        }
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private suspend fun onCheckUserProfile(callback: (Result<String>) -> Unit){
        withContext(Dispatchers.IO) {
            val result: Result<String> = try {
                if (BuildConfig.DEBUG) {
                    val properties = Properties()
                    this@IntroActivity.assets.open("test_config.properties").use {
                        properties.load(it)
                    }
                    viewModel.insertTestData(properties.getProperty(PROPERTIES_TEST_API_KEY))
                    Result.success("USE TEST DATA")
                } else {
                    Result.success("CHECKED")
                }
            } catch(e: Exception){
                Result.failure(e)
            }
            withContext(Dispatchers.Main){
                callback(result)
            }
        }
    }
}