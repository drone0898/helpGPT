package kr.drone.helpgpt.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.ActivityMainBinding
import kr.drone.helpgpt.vm.MainViewModel

@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding, MainViewModel>() {

    private val viemodel: MainViewModel by viewModels()
    override fun getLayoutResourceId(): Int {
        return R.layout.activity_main
    }

    override fun getViewModelClass(): Class<MainViewModel> {
        return MainViewModel::class.java
    }

    override fun initialize() {

    }

    override fun initBinding() {

    }

    override fun initEvent() {

    }
}