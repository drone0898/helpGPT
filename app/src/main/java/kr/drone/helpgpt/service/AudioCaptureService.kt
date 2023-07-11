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
import com.aallam.openai.api.BetaOpenAI
import com.konovalov.vad.webrtc.Vad
import com.konovalov.vad.webrtc.VadListener
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.SampleRate
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kr.drone.helpgpt.R
import kr.drone.helpgpt.databinding.LayoutTranslationScriptBinding
import kr.drone.helpgpt.domain.OpenAIRepository
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

@AndroidEntryPoint
//https://github.com/julioz/AudioCaptureSample
class AudioCaptureService : LifecycleService() {

    @Inject
    lateinit var openAIRepository: OpenAIRepository

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var audioRecord: AudioRecord

    private lateinit var audioOutputFile:File
    private lateinit var audioCompressedFile:File
    private lateinit var vad: VadWebRTC

    private val compressedAudioFile: MutableSharedFlow<File?> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    private val _translateResultText: MutableStateFlow<String> = MutableStateFlow("")
    val translateResultText: StateFlow<String> = _translateResultText

    private lateinit var mParams: WindowManager.LayoutParams
    private lateinit var mWindowManager: WindowManager
    private lateinit var inflater: LayoutInflater
    private lateinit var binding:LayoutTranslationScriptBinding
    private lateinit var startXY:Pair<Float,Float>
    private lateinit var prevXY:Pair<Int,Int>
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
    private var isRecording = true

    private val serviceScopeIO = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var audioRecordJob:Job

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

        updateNotification(isRecording)

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

//        vad = Vad.builder()
//            .setSampleRate(SampleRate.SAMPLE_RATE_16K)
//            .setFrameSize(FrameSize.FRAME_SIZE_320)
//            .setSilenceDurationMs(300)
//            .setSpeechDurationMs(50)
//            .build()

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
        audioRecordJob.cancel()
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
                    if(extra!=null){
                        mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                Activity.RESULT_OK,
                                extra
                            ) as MediaProjection
                        startAudioCapture()
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
                        mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                Activity.RESULT_OK,
                                extra
                            ) as MediaProjection
                        startAudioCapture()
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
    private fun pauseRecording() {
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()
        serviceScope.launch {
            compressedAudioFile.emit(convertPcmToM4a())
            Timber.d("AudioCaptureService stop self()")
        }
    }

    private fun resumeRecording() {

    }

    private fun startAudioCapture() {
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        } else {
            TODO("VERSION.SDK_INT < Q")
        }

        val audioFormat = AudioFormat.Builder()
            .setEncoding(ENCODING_FORMAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(ENCODING_CHANNEL)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: need permission
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                ENCODING_CHANNEL,
                ENCODING_FORMAT
            )
            val audioData = ByteArray(bufferSize)

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            audioRecord.startRecording()
            audioOutputFile = createAudioFile(AudioOriginDIR, "pcm")

//            vad.setContinuousSpeechListener(audioData.asList().chunked(2).map
//            { (l, h) -> (l.toInt() + h.toInt().shl(8)).toShort() }.toShortArray(),
//                object : VadListener{
//                override fun onNoiseDetected() {
//                    Timber.d("VAD Noise Detected")
//                }
//
//                override fun onSpeechDetected() {
//                    Timber.d("VAD Speech Detected")
//                }
//            })

            audioRecordJob = CoroutineScope(Dispatchers.IO).launch {
                audioOutputFile.writeChannel().apply {
                    while (isActive && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val readSize = audioRecord.read(audioData,0,audioData.size)
                        writeFully(audioData,0,readSize)
                    }
                    Timber.d("Audio capture finished for ${audioOutputFile.absolutePath}. File size is ${audioOutputFile.length()} bytes.")
                    close()
                }
            }
        }
    }
    private fun createAudioFile(dir:String, filenameExt:String): File {
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
    private suspend fun convertPcmToM4a():File = withContext(Dispatchers.IO) {
        audioCompressedFile = createAudioFile(AudioCompressDIR,"m4a")
        val buffer = ByteArray(1024)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, CODEC_BIT_RATE)

        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaMuxer = MediaMuxer(audioCompressedFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1

        val audioInputStream = FileInputStream(audioOutputFile)
        val countDownLatch = CountDownLatch(1)
        var eof = false

        mediaCodec.setCallback(object: MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                if(eof) return
                val inputBuffer = codec.getInputBuffer(index)
                inputBuffer?.let {
                    it.clear()
                    val bytesRead = audioInputStream.read(buffer)
                    if (bytesRead <= 0) {
                        codec.queueInputBuffer(index, 0, 0, System.currentTimeMillis(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        eof = true
                        countDownLatch.countDown()
                    } else {
                        it.put(buffer, 0, bytesRead)
                        codec.queueInputBuffer(index, 0, bytesRead, System.currentTimeMillis(), 0)
                    }
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if(eof) return
                val outputBuffer = codec.getOutputBuffer(index)
                outputBuffer?.let {
                    mediaMuxer.writeSampleData(trackIndex, it, info)
                }
                codec.releaseOutputBuffer(index, false)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                countDownLatch.countDown()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                trackIndex = mediaMuxer.addTrack(format)
                mediaMuxer.start()
            }
        })

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()

        try {
            countDownLatch.await()
        } catch (e: Exception) {
            Timber.e(e, "Error processing audio data")
        } finally {
            audioInputStream.close()
            mediaCodec.stop()
            mediaCodec.release()
            mediaMuxer.stop()
            mediaMuxer.release()
        }

        return@withContext audioCompressedFile
    }
    private fun stopAudioCapture() {
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()
        audioRecordJob.cancel()
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "Translation channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Audio Capture Service Channel"
        private const val SAMPLE_RATE = 16000 // or 44100 (maybe error occur)
        private const val CODEC_BIT_RATE = 64000

        private const val ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val ENCODING_CHANNEL = AudioFormat.CHANNEL_IN_MONO


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