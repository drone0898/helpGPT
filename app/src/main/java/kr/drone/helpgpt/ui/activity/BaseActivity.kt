package kr.drone.helpgpt.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kr.drone.helpgpt.core.BaseApplication
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseActivity <V: ViewDataBinding> : AppCompatActivity() {

    protected lateinit var binding: V
    protected lateinit var baseApplication: BaseApplication

    @LayoutRes
    protected abstract fun getLayoutResourceId():Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseApplication = application as BaseApplication

        initialize()

        binding = DataBindingUtil.setContentView(this, getLayoutResourceId())
        binding.lifecycleOwner = this
        bindingViewModel()
        initBinding()
        initEvent()
    }


    protected abstract fun bindingViewModel()
    protected abstract fun initialize() // 초기화
    protected abstract fun initBinding() // 데이터 바인딩
    protected abstract fun initEvent() // 이벤트 바인딩

//    private fun lazyInitViewModel() {
//        binding.setVariable(BR.viewModel, vm)
//        lifecycle.addObserver(vm)
//        return vm
//    }

    fun LifecycleOwner.repeatOnStarted(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED,block)
        }
    }
    fun LifecycleOwner.repeatsOnStarted(blocks: List<suspend CoroutineScope.() -> Unit>) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                blocks.forEach{block ->
                    launch{block()}
                }
            }
        }
    }

    /**
     * @param maintain true면 백스택 유지, false면 초기화
     */
    open fun startTargetActivity(
        target: Class<*>,
        extraData: Bundle?,
        maintain: Boolean
    ) {
        val uri = intent.data
        val cIntent = Intent(this, target)
        if (extraData != null) {
            cIntent.putExtras(extraData)
        }
        if (uri != null) {
            cIntent.data = uri
        }
        if (!maintain) {
            cIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(cIntent)
        if (!maintain) {
            finish()
        }
    }
}