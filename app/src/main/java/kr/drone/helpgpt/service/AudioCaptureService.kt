package kr.drone.helpgpt.service

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kr.drone.helpgpt.R
import kr.drone.helpgpt.domain.LocalRepository
import kr.drone.helpgpt.domain.OpenAIRepository
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.experimental.and

@AndroidEntryPoint
//https://github.com/julioz/AudioCaptureSample
class AudioCaptureService : Service() {

    @Inject
    lateinit var openAIRepository: OpenAIRepository

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var audioRecord: AudioRecord

    lateinit var audioOutputFile:File
    lateinit var audioCompressedFile:File

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate() {
        super.onCreate()
        val stopAction = Intent(
            this, AudioCaptureService::class.java).apply { action = ACTION_STOP }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        val closePending:PendingIntent = PendingIntent.getService(this,0,
            stopAction, flags)

        val notiBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_fiber_manual_record_24)
            .setContentTitle(getString(R.string.recordForTranslate))
            .addAction(R.drawable.round_stop_24,getString(R.string.stopRecord),closePending)
        createNotificationChannel()

        startForeground(
            SERVICE_ID,
            notiBuilder.build()
        )

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }


    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Audio Capture Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    val extra =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                        } else {
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
                    START_STICKY
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                    START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            START_NOT_STICKY
        }
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
        private const val LOG_TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"
        private const val SAMPLE_RATE = 16000 // use 44100 instead.
        private const val CODEC_BIT_RATE = 64000

        private const val ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val ENCODING_CHANNEL = AudioFormat.CHANNEL_IN_MONO


        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
        const val AudioOriginDIR = "/AudioCaptures"
        const val AudioCompressDIR = "/AudioCompress"
    }
}