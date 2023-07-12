package kr.drone.helpgpt.domain

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import com.konovalov.vad.webrtc.Vad
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch

/**
 * 이 Repository 는 생성시 Manifest.permission.RECORD_AUDIO 권한이 필요함
 *
 * ref https://github.com/julioz/AudioCaptureSample
 *
 */
@SuppressLint("MissingPermission")
class AudioCaptureRepository constructor(private val mediaProjection: MediaProjection) {

    private val vad: VadWebRTC
    private val audioRecord: AudioRecord
    private var audioData:ByteArray

    private lateinit var audioRecordJob: Job
    lateinit var vadJob: Job

    lateinit var speech: Flow<Boolean>

    init {
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

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            ENCODING_CHANNEL,
            ENCODING_FORMAT
        )
        audioData = ByteArray(bufferSize)

        vad = Vad.builder()
            .setSampleRate(SampleRate.SAMPLE_RATE_16K)
            .setFrameSize(FrameSize.FRAME_SIZE_320)
            .setMode(Mode.NORMAL)
            .setSilenceDurationMs(200)
            .setSpeechDurationMs(25)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
    }

    fun startAudioCapture(audioOutputFile:File) {
        audioRecord.startRecording()
        vadJob = CoroutineScope(Dispatchers.IO).launch {
            speech = flow {
                while (currentCoroutineContext().isActive) {
                    delay(25)
                    if (audioRecordJob.isActive) {
                        emit(vad.isSpeech(audioData.asList().chunked(2).map { (l, h) ->
                            (l.toInt() + h.toInt().shl(8)).toShort()
                        }.toShortArray()))
                    } else {
                        emit(false)
                    }
                }
            }
        }
        audioRecordJob = CoroutineScope(Dispatchers.IO).launch {
            audioOutputFile.writeChannel().use {
                while (isActive && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readSize = audioRecord.read(audioData,0,audioData.size)
                    writeFully(audioData,0,readSize)
                }
                Timber.d("Audio capture finished for ${audioOutputFile.absolutePath}. File size is ${audioOutputFile.length()} bytes.")
            }
        }
    }

    private fun pauseAudioCapture() {
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()
        audioRecordJob.cancel()
        vadJob.cancel()
    }

    fun closeRepository(){
        audioRecord.stop()
        audioRecord.release()
        mediaProjection.stop()
        audioRecordJob.cancel()
        vadJob.cancel()
    }

    suspend fun convertPcmToM4a(compressedFile:File):File = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, CODEC_BIT_RATE)

        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaMuxer = MediaMuxer(compressedFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1

        val audioInputStream = FileInputStream(compressedFile)
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

        return@withContext compressedFile
    }



    companion object {
        private const val SAMPLE_RATE = 16000 // or 44100 (maybe error occur)
        private const val CODEC_BIT_RATE = 64000

        private const val ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val ENCODING_CHANNEL = AudioFormat.CHANNEL_IN_MONO
    }
}