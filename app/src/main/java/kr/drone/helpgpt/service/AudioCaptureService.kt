package kr.drone.helpgpt.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kr.drone.helpgpt.R
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
class AudioCaptureService : Service() {

    @Inject
    lateinit var openAIRepository: OpenAIRepository

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var audioRecord: AudioRecord

    private lateinit var audioOutputFile:File
    private lateinit var audioCompressedFile:File

    private lateinit var mParams: WindowManager.LayoutParams
    private lateinit var mWindowManager: WindowManager
    private var isRecording = true

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

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
        val stopPendingIntent: PendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)

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
        super.onDestroy()
        job.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
    private fun pauseRecording(){
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()
    }

    private fun resumeRecording(){

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
            CoroutineScope(Dispatchers.IO).launch {
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

        serviceScope.launch {
            openAIRepository.compressedAudioFile.emit(convertPcmToM4a())
            Timber.d("AudioCaptureService stop self()")
            stopSelf()
        }
    }
    override fun onBind(p0: Intent?): IBinder? = null

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