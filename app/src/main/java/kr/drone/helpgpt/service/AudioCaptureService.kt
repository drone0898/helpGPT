package kr.drone.helpgpt.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.LayoutTranslationScriptBinding
import kr.drone.helpgpt.domain.AudioCaptureRepository
import kr.drone.helpgpt.domain.OpenAIRepository
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AudioCaptureService : LifecycleService() {

    @Inject
    lateinit var openAIRepository: OpenAIRepository
    lateinit var audioCaptureRepository: AudioCaptureRepository
    private lateinit var mediaProjectionManager:MediaProjectionManager


    private val compressedAudioFile: MutableSharedFlow<File?> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    private val _translateResultText: MutableStateFlow<String> = MutableStateFlow("")
    val translateResultText: StateFlow<String> = _translateResultText

    private lateinit var mParams: WindowManager.LayoutParams
    private lateinit var mWindowManager: WindowManager
    private lateinit var inflater: LayoutInflater
    private lateinit var binding:LayoutTranslationScriptBinding
    private lateinit var startXY:Pair<Float,Float>
    private lateinit var prevXY:Pair<Int,Int>

    private var isRecording = false

    private val serviceScopeIO = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @SuppressLint("ClickableViewAccessibility")
    private val dragListener: View.OnTouchListener = View.OnTouchListener { _, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startXY = Pair(event.rawX,event.rawY)
                prevXY = Pair(mParams.x,mParams.y)
                mParams.alpha = 0.5f
                mWindowManager.updateViewLayout(binding.root, mParams) //뷰 업데이트
            }

            MotionEvent.ACTION_MOVE -> {
                //터치해서 이동한 만큼 이동 시킨다
                mParams.x = prevXY.first + (event.rawX - startXY.first).toInt()
                mParams.y = prevXY.second + (event.rawY - startXY.second).toInt()
                mWindowManager.updateViewLayout(binding.root, mParams) //뷰 업데이트
            }

            MotionEvent.ACTION_UP -> {
                mParams.alpha = 1f
                mWindowManager.updateViewLayout(
                    binding.root,
                    mParams
                )
            }
        }
        true
    }


    override fun onCreate() {
        super.onCreate()
        val notiBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_fiber_manual_record_24)
            .setContentTitle(getString(R.string.recordForTranslate))
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
        startForeground(
            NOTIFICATION_ID,
            notiBuilder.build()
        )

        updateNotification(true)

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        createView()
        collect()
    }

    private fun createView() {
        inflater = LayoutInflater.from(this)
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_translation_script, null,
            false)
        binding.lifecycleOwner = this
        binding.service = this

        mParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  //항상 최 상위에 있게. status bar 밑에 있음. 터치 이벤트 받을 수 있음.
            (WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS),  //이 속성을 안주면 터치 & 키 이벤트도 먹게 된다. //포커스를 안줘서 자기 영역 밖터치는 인식 안하고 키이벤트를 사용하지 않게 설정
            PixelFormat.TRANSLUCENT
        )
        mParams.gravity =
            Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL //왼쪽 상단에 위치하게 함.

        mParams.alpha = 1f

        binding.root.setOnTouchListener(dragListener)

        //TODO : remember previous ui position
//        val pos: CoordinateValue = LocalDataManager.getInstance().getData(CoordinateValue::class.java)
//        if (pos != null) {
//            mParams.x = pos.x
//            mParams.y = pos.y
//        }
        if (Settings.canDrawOverlays(this@AudioCaptureService)) {
            mWindowManager.addView(binding.root, mParams)
        }
    }

    @OptIn(BetaOpenAI::class)
    private fun collect() {
        serviceScopeIO.launch {
            compressedAudioFile.filterNotNull().collect {
                _translateResultText.value = openAIRepository.transcriptionRequest(it).text
            }
        }
    }

    private fun updateNotification(isRecording: Boolean) {
        this.isRecording = isRecording
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        val pauseIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = ACTION_PAUSE_RECORD
        }
        val pausePendingIntent: PendingIntent = PendingIntent.getService(this, 0, pauseIntent, flags)

        val resumeIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = ACTION_RESUME_RECORD
        }
        val resumePendingIntent: PendingIntent = PendingIntent.getService(this, 0, resumeIntent, flags)

        // 중지 액션 PendingIntent
        val stopIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notiBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(if(isRecording){R.drawable.round_fiber_manual_record_24}else{R.drawable.round_pause_circle_outline_24})
            .setContentTitle(getString(R.string.recordForTranslate))

        // 녹음 상태에 따라 알림의 액션을 결정
        if (isRecording) {
            notiBuilder.addAction(R.drawable.round_pause_24, getString(R.string.pauseRecord), pausePendingIntent)
            notiBuilder.addAction(R.drawable.round_stop_24, getString(R.string.stopRecord), stopPendingIntent)
        } else {
            notiBuilder.addAction(R.drawable.round_fiber_manual_record_24, getString(R.string.resumeRecord), resumePendingIntent)
            notiBuilder.addAction(R.drawable.round_stop_24, getString(R.string.stopRecord), stopPendingIntent)
        }

        // 알림 업데이트
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notiBuilder.build())
    }

    override fun onDestroy() {
        mWindowManager.removeView(binding.root)
        serviceScopeIO.cancel()
        serviceScope.cancel()
        audioCaptureRepository.closeRepository()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
             when (intent.action) {
                ACTION_START -> {
                    val extra =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(EXTRA_RESULT_DATA)
                        }
                    if(extra!=null) {
                        val mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                Activity.RESULT_OK,
                                extra
                            ) as MediaProjection
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: need permission
                        }else{
                            startRecording(mediaProjection)
                        }
                    }
                }
                ACTION_START_WITH_OVERLAY -> {
                    val extra =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(EXTRA_RESULT_DATA)
                        }
                    if(extra!=null){
                        val mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                Activity.RESULT_OK,
                                extra
                            ) as MediaProjection
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: need permission
                        }else{
                            startRecording(mediaProjection)
                        }
                    }
                }
                ACTION_RESUME_RECORD-> {
                    resumeRecording()
                }
                ACTION_PAUSE_RECORD-> {
                    pauseRecording()
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        }
        return START_STICKY
    }

    private fun startRecording(mediaProjection: MediaProjection){
        audioCaptureRepository = AudioCaptureRepository(mediaProjection)
        audioCaptureRepository.startAudioCapture(createAudioFile(AudioOriginDIR, "pcm"))
        lifecycleScope.launch {
            audioCaptureRepository.speech.collect {
                Timber.d("Speech : $it")
            }
        }
    }

    private fun pauseRecording() {
        updateNotification(false)
        serviceScope.launch {
            compressedAudioFile.emit(audioCaptureRepository.convertPcmToM4a(createAudioFile(AudioCompressDIR,"m4a")))
            Timber.d("AudioCaptureService stop self()")
        }
    }

    private fun resumeRecording() {
        updateNotification(true)
    }
    private fun stopAudioCapture() {
        updateNotification(false)
        audioCaptureRepository.closeRepository()
        stopSelf()
    }

    private fun createAudioFile(dir: String = AudioOriginDIR, filenameExt:String): File {
        val audioCapturesDirectory = File(cacheDir, dir)
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs()
        }
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(Date())
        val fileName = "Capture-$timestamp.$filenameExt"
        val file = File(audioCapturesDirectory.absolutePath + "/" + fileName)
        Timber.d("Created File : ${file.absolutePath}/$fileName")
        return file
    }

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "Translation channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Audio Capture Service Channel"


        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val ACTION_RESUME_RECORD = "AudioCaptureService:ResumeRecord"
        const val ACTION_PAUSE_RECORD = "AudioCaptureService:PauseRecord"
        const val ACTION_START_WITH_OVERLAY = "AudioCaptureService:StartWithOverlay"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"

        const val AudioOriginDIR = "/AudioCaptures"
        const val AudioCompressDIR = "/AudioCompress"
    }
}