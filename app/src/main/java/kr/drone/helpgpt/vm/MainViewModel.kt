package kr.drone.helpgpt.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.Translation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kr.drone.helpgpt.domain.OpenAIRepository
import kr.drone.helpgpt.util.VIEW_GONE
import kr.drone.helpgpt.util.VIEW_VISIBLE
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
@HiltViewModel
@Suppress("PropertyName")
class MainViewModel @Inject constructor(
    openAIRepository: OpenAIRepository
) : ViewModel() {
    companion object{
        const val EVENT_START_CRAWLING = "EVENT_START_CRAWLING"
        const val EVENT_STOP_CRAWLING = "EVENT_STOP_CRAWLING"
    }

    private val _videoId = MutableStateFlow("")
    val _address = MutableStateFlow("https://www.youtube.com/watch?v=8LdjmJtwdVA")
    private val _event: MutableStateFlow<String> = MutableStateFlow("")
    val summaryBtnVisibility: MutableStateFlow<Int> = MutableStateFlow(VIEW_VISIBLE)
    val summaryVisibility: MutableStateFlow<Int> = MutableStateFlow(VIEW_GONE)
    val translateBtnVisibility: MutableStateFlow<Int> = MutableStateFlow(VIEW_VISIBLE)
    val translateVisibility: MutableStateFlow<Int> = MutableStateFlow(VIEW_GONE)
    val translateResultText: MutableStateFlow<String> = MutableStateFlow("")

    val event: StateFlow<String> = _event
    val address:StateFlow<String> get() = _address
    val videoId:StateFlow<String> get() = _videoId
    val script:MutableStateFlow<String> = MutableStateFlow("")

    lateinit var translation: Translation
    init {
        viewModelScope.launch {
            openAIRepository.compressedAudioFile.filterNotNull().collect {
                translation = openAIRepository.transcriptionRequest(it)
                translateResultText.value = translation.text
            }
        }
    }

    fun extractVideoIdFromUrl(url:String) {
//        stopCrawling()
        val videoIdPattern = "^(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)"
        val pattern = Pattern.compile(videoIdPattern)
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            val videoId = matcher.group(1) as String
            _videoId.value = videoId
//            startCrawling()
        } else {
            _videoId.value = ""
        }
    }

    // Service위에 Webview를 얹어 자막을 크롤링하는것이 제한되므로,
    // youtube 주소를 입력했을때 사용자에게 보이지않는 webview를 통해 자막을 먼저 크롤링하고,
    // 버튼을 눌렀을 때 크롤링한 자막을 전송해 요약처리하도록 구현.
    // 만약 버튼을 눌렀을 때 크롤링이 끝나지않았다면 로딩바가 돌아가도록 처리할 것
    private fun startCrawling(){
        _event.value = EVENT_START_CRAWLING
    }

    private fun stopCrawling(){
        _event.value = EVENT_STOP_CRAWLING
    }

    fun onSummaryBtnClick(){
        summaryVisibility.value = VIEW_VISIBLE
        hideBtn()
    }

    fun onTranslateBtnClick(){
        translateVisibility.value = VIEW_VISIBLE
        hideBtn()
    }

    private fun hideBtn(){
        translateBtnVisibility.value = VIEW_GONE
        summaryBtnVisibility.value = VIEW_GONE
    }
}



/*
class CrawlingViewModel @Inject constructor(
    private val repository: CrawledDataRepository,
    private val webView: WebView // 이 예시에서는 간단하게 WebView를 주입받았지만 실제 구현에서는 Activity나 Fragment에서 WebView를 관리하고 WebViewClient의 콜백을 통해 크롤링 결과를 처리하는 것이 더 나을 수 있습니다.
) : ViewModel() {
    val crawledData = MutableLiveData<CrawledData>()

    fun crawlAndSaveData() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // 이 부분에서 웹페이지의 데이터를 크롤링하고 그 결과를 crawledData에 저장합니다.
                // 예를 들어 JavaScript를 이용해서 페이지의 특정 부분의 텍스트를 가져올 수 있습니다.
                webView.evaluateJavascript("(function() { return document.getElementById('my-element').innerText; })();") { result ->
                    val data = CrawledData(result)
                    crawledData.value = data
                    saveData(data)
                }
            }
        }
    }

    private fun saveData(data: CrawledData) {
        viewModelScope.launch {
            repository.saveData(data)
        }
    }
}
 */