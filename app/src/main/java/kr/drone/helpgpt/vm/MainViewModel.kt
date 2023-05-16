package kr.drone.helpgpt.vm

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.regex.Pattern

class MainViewModel(application: Application) : BaseViewModel(application) {

    val address:StateFlow<String> get() = _address
    var _address = MutableStateFlow("https://www.youtube.com/watch?v=8LdjmJtwdVA")
    val videoId:StateFlow<String> get() = _videoId
    private var _videoId = MutableStateFlow("")

    fun extractVideoIdFromUrl(url:String){
        val videoIdPattern = "^(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)"
        val pattern = Pattern.compile(videoIdPattern)
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            _videoId.value = matcher.group(1) as String
        } else {
            _videoId.value = ""
        }
    }
}