package kr.drone.helpgpt.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kr.drone.helpgpt.core.BaseApplication
import kr.drone.helpgpt.vm.BaseViewModel

import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

abstract class BaseActivity <V: ViewDataBinding, M: BaseViewModel> : AppCompatActivity() {

    private val viewModelProvider: ViewModelProvider by lazy {
        ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(baseApplication))
    }
    protected val viewModel: M by lazy {
        lazyInitViewModel()
    }
    protected lateinit var binding: V
    protected lateinit var baseApplication: BaseApplication

    @LayoutRes
    protected abstract fun getLayoutResourceId():Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseApplication = application as BaseApplication

        initialize()

        binding = DataBindingUtil.setContentView(this, getLayoutResourceId())
        binding.setVariable(BR.activity, this)
        binding.lifecycleOwner = this
        initBinding()
        initEvent()
    }

    private fun lazyInitViewModel():M {
        val vm = viewModelProvider[getViewModelClass()]
        binding.setVariable(BR.viewModel, vm)
        lifecycle.addObserver(vm)
        return vm
    }

    protected abstract fun initialize() // 초기화
    protected abstract fun initBinding() // 데이터 바인딩
    protected abstract fun initEvent() // 이벤트 바인딩
    protected abstract fun getViewModelClass(): Class<M>


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