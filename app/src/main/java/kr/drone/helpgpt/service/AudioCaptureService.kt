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
            AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // TODO provide UI options for inclusion/exclusion
                .build()
        } else {
            TODO("VERSION.SDK_INT < Q")
        }

        /**
         * Using hardcoded values for the audio format, Mono PCM samples with a sample rate of 8000Hz
         * These can be changed according to your application's needs
         */
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
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
//                        audioRecord.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
                        writeFully(audioData,0,readSize)
//                        fileOutputStream.write(
//                            capturedAudioSamples.toByteArray(),
//                            0,
//                            BUFFER_SIZE_IN_BYTES
//                        )
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

    private fun stopAudioCapture() {
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()

        serviceScope.launch {
//            openAIRepository.compressedAudioFile.emit(compressAudioFile())
//            openAIRepository.compressedAudioFile.emit(audioOutputFile)
            Timber.d("AudioCaptureService stop self()")
            stopSelf()
        }
    }

    private suspend fun compressAudioFile(): File = withContext(Dispatchers.IO) {

        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)

        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        val fileChannel = FileInputStream(audioOutputFile).channel
        audioCompressedFile = createAudioFile(AudioCompressDIR, "m4a")
        val outputStream = FileOutputStream(audioCompressedFile)

        mediaCodec.setCallback(object : MediaCodec.Callback() {

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val buffer = codec.getInputBuffer(index)
                val bytesRead = fileChannel.read(buffer)
                if (bytesRead < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(index, 0, bytesRead, System.nanoTime(), 0)
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Timber.e("MediaCodec onError: ", e)
                cleanup()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Timber.d("MediaCodec onOutputFormatChanged")
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                val outBuffer = ByteArray(info.size)
                val outputBuffer = codec.getOutputBuffer(index)
                outputBuffer?.get(outBuffer)
                outputStream.write(outBuffer)
                codec.releaseOutputBuffer(index, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    cleanup()
                }
            }

            fun cleanup() {
                mediaCodec.stop()
                mediaCodec.release()
                fileChannel.close()
                outputStream.close()
            }
        })

        mediaCodec.start()

        return@withContext audioCompressedFile
    }

    override fun onBind(p0: Intent?): IBinder? = null

    companion object {
        private const val LOG_TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"
        private const val SAMPLE_RATE = 16000 // use 44100 instead.

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
        const val AudioOriginDIR = "/AudioCaptures"
        const val AudioCompressDIR = "/AudioCompress"
    }
}